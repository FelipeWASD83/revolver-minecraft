"""Leva a Desert Eagle do projeto deagle-3d para dentro do mod.

Roda no Blender (abre o deagle.blend, não mexe nele):

    "C:\\Program Files\\Blender Foundation\\Blender 5.2\\blender.exe" -b ..\\deagle-3d\\modelo\\deagle.blend --python ferramentas\\exportar_deagle.py

Escreve em src/main/resources/assets/revolver/:
- models/item/deagle/<peça>.obj + .mtl + .json: o corpo parado num só; ferrolho, cão, gatilho, trava do ferrolho,
  botão do pente, pente e clarão separados; o estojo (o que voa no tiro e o de cima do pente usam a mesma malha) e
  uma ponta de bala por tipo de cartucho;
- textures/item/deagle/: branco e borracha (a borracha do jogo é mais clara que a do Blender, senão vira um borrão
  preto);
- textures/item/cartucho_50*.png: os ícones dos cartuchos .50 AE;
- models/item/deagle.json e cartucho_50*.json: os itens;
- animacoes/deagle.json: as cinco animações do Blender (tiro, tiro_ultimo, seco, recarregar e recarregar_vazia),
  uma matriz 3x4 por peça por quadro, e os quadros dos eventos (disparo, troca do pente, ferrolho...).
"""

import json
import sys
from pathlib import Path

import bmesh
import bpy
import numpy as np
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
import comum  # noqa: E402
from comum import ASSETS, PASTA, TEXTURAS, m34, malha_no_item, matriz_item  # noqa: E402

MODELOS = ASSETS / "models" / "item" / "deagle"
FPS = 30
# início e comprimento de cada faixa no deagle.blend (iguais às constantes do gerar.py de lá)
ANIMACOES = {"tiro": (1, 14), "tiro_ultimo": (20, 14), "seco": (40, 10), "recarregar": (60, 76),
             "recarregar_vazia": (145, 86)}
PALMA = Vector((-68.0, 0.0, -50.0))   # centro da pegada no deagle.blend (mm)

ANIMADAS = ["ferrolho", "cao", "gatilho", "trava_ferrolho", "botao_pente", "pente", "clarao", "estojo_ejetado",
            "estojo_topo", "bala_topo"]
ESCONDIDAS = ["clarao", "estojo_ejetado"]   # escala zero em repouso: exportadas inteiras, aparecem pela animação

# Cor de cada material no jogo (sem metal nem reflexo no Minecraft: a cor vai chapada e mais clara que no Cycles; o
# preto puro vira um borrão, então o "escuro" é um cinza grafite)
MATERIAIS = {
    "aco_inox": ("branco", (0.80, 0.81, 0.84)),
    "aco_escuro": ("branco", (0.27, 0.27, 0.29)),
    "aco_pente": ("branco", (0.52, 0.53, 0.56)),
    "borracha": ("borracha", (1.0, 1.0, 1.0)),
    "latao": ("branco", (0.88, 0.70, 0.40)),
    "espoleta": ("branco", (0.80, 0.78, 0.74)),
    "cobre": ("branco", (0.84, 0.52, 0.34)),
    "clarao": ("branco", (1.0, 0.86, 0.48)),
}
TEXTURAS_JSON = {"borracha": "revolver:item/deagle/borracha", "branco": "revolver:item/deagle/branco",
                 "particle": "revolver:item/deagle/branco"}
CABECALHO = "# {nome}: gerado por ferramentas/exportar_deagle.py a partir do deagle-3d"

# Os tipos de cartucho .50 AE (mesma ordem e nomes do TipoBala.java): cor da ponta no modelo e no ícone
TIPOS = [
    ("comum", "cartucho_50", (0.84, 0.52, 0.34), (0.66, 0.36, 0.20), (0.93, 0.66, 0.46)),
    ("aco", "cartucho_50_aco", (0.82, 0.84, 0.88), (0.66, 0.68, 0.73), (0.93, 0.94, 0.96)),
    ("diamante", "cartucho_50_diamante", (0.38, 0.90, 0.86), (0.20, 0.70, 0.68), (0.60, 0.97, 0.94)),
    ("netherite", "cartucho_50_netherite", (0.32, 0.27, 0.29), (0.22, 0.18, 0.20), (0.45, 0.40, 0.42)),
]

# Como a deagle aparece em cada lugar (mesma conta do revólver; ela é maior, então a escala é um pouco menor)
EXIBICAO = {
    "firstperson_righthand": {"giro": (0, 90, 0), "escala": 0.78, "palma": (-0.05, 0.03, 0.08)},
    "thirdperson_righthand": {"giro": (90, 90, 0), "escala": 0.38, "palma": (0.0, 0.06, 0.02)},
    "gui": {"giro": (0, 0, 0), "escala": 0.80, "centro": True},
    "ground": {"giro": (0, 0, 0), "escala": 0.36, "centro": True, "desloc": (0, 0.12, 0)},
    "fixed": {"giro": (0, 180, 0), "escala": 0.75, "centro": True},
}

# Quadros dos eventos (contados do começo de cada animação, quadro 1 = começo); o TemposDeagle.java repete estes
# números e o teste confere
EVENTOS = {
    "tiro": {"disparo": 4, "estojo_cai": 14, "fim": 14},
    "tiro_ultimo": {"disparo": 4, "trava": 7, "estojo_cai": 14, "fim": 14},
    "seco": {"clique": 3, "fim": 10},
    "recarregar": {"botao": 15, "pente_sai": 16, "troca": 34, "pente_entra": 54, "fim": 76},
    "recarregar_vazia": {"botao": 15, "pente_sai": 16, "troca": 34, "pente_entra": 54, "solta_trava": 62,
                         "ferrolho": 64, "fim": 86},
}


def escrever_obj(nome, bm, materiais, emissivo=False, cores=None):
    return comum.escrever_obj("deagle", nome, bm, materiais, MATERIAIS, TEXTURAS_JSON, CABECALHO, emissivo, cores)


def textura_borracha():
    """O pontilhado do cabo para o jogo: 32x32, grafite com pontos mais claros e mais escuros."""
    n = 32
    rng = np.random.default_rng(1983)
    v = 0.24 + 0.05 * rng.standard_normal((n, n))
    pontos = rng.random((n, n)) < 0.18
    v[pontos] += 0.07
    v = np.clip(v, 0.14, 0.36)
    img = bpy.data.images.new("borracha", n, n)
    px = np.concatenate([np.repeat(v[..., None], 3, axis=2), np.ones((n, n, 1))], axis=2).astype(np.float32)
    img.pixels.foreach_set(px.ravel())
    img.filepath_raw = str(TEXTURAS / "deagle" / "borracha.png")
    img.file_format = 'PNG'
    img.save()


def principal():
    cena = bpy.context.scene
    cena.frame_set(0)
    MODELOS.mkdir(parents=True, exist_ok=True)
    (TEXTURAS / "deagle").mkdir(parents=True, exist_ok=True)
    (ASSETS / "animacoes").mkdir(parents=True, exist_ok=True)
    for velho in MODELOS.glob("*"):
        velho.unlink()

    raiz = bpy.data.objects["deagle"]
    pecas = [o for o in bpy.data.objects if o.type == 'MESH']
    estaticas = [o for o in pecas if o.name not in ANIMADAS]
    cantos = []
    for o in pecas:
        if o.name in ESCONDIDAS:
            continue
        cantos += [raiz.matrix_world.inverted() @ o.matrix_world @ Vector(c) for c in o.bound_box]
    minimo = Vector([min(c[i] for c in cantos) for i in range(3)])
    maximo = Vector([max(c[i] for c in cantos) for i in range(3)])
    centro = (minimo + maximo) / 2
    A = matriz_item(raiz, centro)
    Ainv = A.inverted()
    print("tamanho em mm:", [round(v, 1) for v in (maximo - minimo)])

    # ---- geometria (as escondidas vão inteiras: escala 1 só durante a exportação)
    guardadas = {}
    for nome in ESCONDIDAS:
        ob = bpy.data.objects[nome]
        guardadas[nome] = ob.scale.copy()
        ob.scale = (1, 1, 1)
    bpy.context.view_layer.update()
    relatorio = []
    bm = bmesh.new()
    usados = []
    for o in estaticas:
        malha_no_item(o, A, bm, usados, "aco_inox")
    relatorio.append(("corpo", escrever_obj("corpo", bm, usados), ", ".join(sorted(o.name for o in estaticas))))
    for nome, fonte in [("ferrolho", "ferrolho"), ("cao", "cao"), ("gatilho", "gatilho"), ("trava", "trava_ferrolho"),
                        ("botao", "botao_pente"), ("pente", "pente"), ("clarao", "clarao"), ("estojo", "estojo_ejetado")]:
        bm = bmesh.new()
        usados = []
        malha_no_item(bpy.data.objects[fonte], A, bm, usados, "aco_inox")
        relatorio.append((nome, escrever_obj(nome, bm, usados, emissivo=(nome == "clarao")), fonte))
    for tipo, _, ponta, _, _ in TIPOS:
        bm = bmesh.new()
        usados = []
        malha_no_item(bpy.data.objects["bala_topo"], A, bm, usados, "cobre")
        relatorio.append((f"bala_{tipo}", escrever_obj(f"bala_{tipo}", bm, usados, cores={"cobre": ponta}),
                          "bala_topo"))
    base_escondida = {nome: bpy.data.objects[nome].matrix_world.copy() for nome in ESCONDIDAS}
    for nome, esc in guardadas.items():
        bpy.data.objects[nome].scale = esc
    bpy.context.view_layer.update()

    # ---- animações: matriz de cada peça em cada quadro, no espaço do item, relativa à peça exportada
    ob = bpy.data.objects
    pistas = {"corpo": raiz, "ferrolho": ob["ferrolho"], "cao": ob["cao"], "gatilho": ob["gatilho"],
              "trava": ob["trava_ferrolho"], "botao": ob["botao_pente"], "pente": ob["pente"], "clarao": ob["clarao"],
              "ejetado": ob["estojo_ejetado"], "topo_estojo": ob["estojo_topo"], "topo_bala": ob["bala_topo"]}
    cena.frame_set(0)
    base = {}
    for nome, o in pistas.items():
        if nome == "clarao":
            base[nome] = base_escondida["clarao"]
        elif nome in ("ejetado", "topo_estojo"):
            base[nome] = base_escondida["estojo_ejetado"]      # as duas usam a malha do estojo exportado
        else:
            base[nome] = o.matrix_world.copy()

    def amostra():
        return {nome: A @ o.matrix_world @ base[nome].inverted() @ Ainv for nome, o in pistas.items()}

    cena.frame_set(0)
    repouso = {n: m34(M) for n, M in amostra().items()}
    animacoes = {}
    for nome_anim, (inicio, quadros) in ANIMACOES.items():
        faixas = {n: [] for n in pistas}
        for q in range(quadros):
            cena.frame_set(inicio + q)
            for n, M in amostra().items():
                faixas[n].append(m34(M))
        faixas = {n: f for n, f in faixas.items() if any(m != repouso[n] for m in f)}
        animacoes[nome_anim] = {"quadros": quadros, "faixas": faixas}
    cena.frame_set(0)
    dados = {"_leia": "Gerado por ferramentas/exportar_deagle.py a partir do deagle-3d. Matriz 3x4 por linha, no "
                      "espaço do item, aplicada sobre a peça exportada; quadro 1 = índice 0.",
             "fps": FPS, "repouso": repouso, "animacoes": animacoes, "eventos": EVENTOS}
    (ASSETS / "animacoes" / "deagle.json").write_text(json.dumps(dados, separators=(",", ":")), encoding="utf-8")

    # ---- texturas e itens
    comum.textura_branca("deagle")
    textura_borracha()
    for _, item_nome, _, escura, clara in TIPOS:
        # o .50 AE é mais curto e bem mais grosso que o .45 no ícone
        comum.icone_cartucho(item_nome, escura, clara, a=(4.0, 12.0), b=(12.0, 4.0), aro=2.3, corpo=2.3,
                             inicio_ponta=0.6, curva_ponta=0.8)
    item = {"parent": "builtin/entity", "gui_light": "front",
            "textures": {"particle": "revolver:item/deagle/branco"}, "display": comum.exibicao(A, PALMA, EXIBICAO)}
    (ASSETS / "models" / "item" / "deagle.json").write_text(json.dumps(item, indent=2) + "\n", encoding="utf-8")
    for _, item_nome, _, _, _ in TIPOS:
        cartucho = {"parent": "minecraft:item/generated", "textures": {"layer0": f"revolver:item/{item_nome}"}}
        (ASSETS / "models" / "item" / f"{item_nome}.json").write_text(json.dumps(cartucho, indent=2) + "\n",
                                                                      encoding="utf-8")

    # ---- conferência: peça exportada (repouso) + matriz do quadro tem que cair onde o Blender põe a peça
    conferir = {"ferrolho": "ferrolho", "cao": "cao", "pente": "pente", "trava": "trava_ferrolho",
                "topo_bala": "bala_topo", "ejetado": "estojo_ejetado", "corpo": "armacao"}
    base_armacao = ob["armacao"].matrix_world.copy()
    pior = 0.0
    for nome_anim, (inicio, quadros) in ANIMACOES.items():
        faixas = animacoes[nome_anim]["faixas"]
        for q in (0, 3, 5, 8, 20, 30, 45, 60, 70, 80):
            if q >= quadros:
                continue
            cena.frame_set(inicio + q)
            for nome, fonte in conferir.items():
                lista = faixas.get(nome)
                M34 = lista[q] if lista else repouso[nome]
                M = Matrix((M34[0:4], M34[4:8], M34[8:12], (0, 0, 0, 1)))
                o = ob[fonte]
                if o.matrix_world.to_scale().length < 1e-6:
                    continue
                origem = base_armacao if nome == "corpo" else base[nome]
                for v in list(o.data.vertices)[::97]:
                    exportado = A @ origem @ v.co
                    real = A @ o.matrix_world @ v.co
                    pior = max(pior, (M @ exportado - real).length)
    cena.frame_set(0)
    linhas = ["EXPORTAÇÃO DA DEAGLE PARA O MOD", ""]
    linhas += [f"{n:<15} {t:6d} triângulos  ({f})" for n, t, f in relatorio]
    linhas.append("")
    for nome_anim in ANIMACOES:
        linhas.append("%s: %d quadros, peças que mexem: %s" % (nome_anim, animacoes[nome_anim]["quadros"],
                                                               ", ".join(animacoes[nome_anim]["faixas"])))
    linhas += ["conferência (peça exportada + matriz do quadro contra o Blender): maior erro %.7f bloco" % pior,
               "exibição: " + json.dumps(comum.exibicao(A, PALMA, EXIBICAO))]
    (PASTA / "ferramentas" / "exportacao-deagle.txt").write_text("\n".join(linhas) + "\n", encoding="utf-8")
    print("\n".join(linhas))
    if pior > 1e-4:
        print("ERRO: a conferência falhou")
        sys.exit(1)


principal()
