"""Leva o revólver do projeto revolver-3d para dentro do mod.

Roda no Blender (abre o revolver.blend, não mexe nele):

    "C:\\Program Files\\Blender Foundation\\Blender 5.2\\blender.exe" -b ..\\revolver-3d\\modelo\\revolver.blend --python ferramentas\\exportar_modelo.py

Escreve em src/main/resources/assets/revolver/:
- models/item/revolver/<peça>.obj + .mtl + .json: uma peça por arquivo (o corpo parado num só; cão, gatilho,
  tambor, portinhola, botão da haste e clarão separados; um estojo e uma bala, que o jogo desenha seis vezes);
- textures/item/revolver/: madeira, têmpera e branco (o branco recebe a cor do material pelo Kd do .mtl);
- textures/item/cartucho_45.png: o ícone do cartucho, em pixel art;
- models/item/revolver.json e cartucho_45.json: os itens;
- animacoes/revolver.json: as duas animações do Blender (tiro e recarregar), uma matriz 3x4 por peça por quadro,
  e os quadros dos eventos (disparo, estojo caindo, bala entrando...).

Espaço do item: 1 = um bloco; o revólver tem 1 de comprimento e fica centrado em (0,5; 0,5; 0,5), com o cano
para +X, o topo para +Y e o lado direito (portinhola, haste) para +Z, que é o lado que aparece no inventário.
"""

import json
import shutil
import sys
from pathlib import Path

import bmesh
import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
import comum  # noqa: E402
from comum import ASSETS, PASTA, TEXTURAS, m34, malha_no_item, matriz_item  # noqa: E402

ORIGEM = PASTA.parent / "revolver-3d"
MODELOS = ASSETS / "models" / "item" / "revolver"
FPS = 30
TIRO = (1, 50)          # quadros da cena no revolver.blend (iguais às constantes do gerar.py de lá)
RECARGA = (70, 340)     # início e comprimento
REC_T0, REC_BLOCO = 30, 46
PALMA = Vector((-70.0, 0.0, -40.0))   # centro da pegada no revolver.blend (mm), para pôr o cabo na mão

ANIMADAS = ["cao", "gatilho", "tambor", "portinhola", "botao_ejetor", "clarao"]
MUNICAO = [f"estojo_{k}" for k in range(6)] + [f"bala_{k}" for k in range(6)]

# Cor de cada material no jogo (o Minecraft não tem metal nem verniz: a cor vai chapada, mais clara que a do
# Cycles). Material com textura multiplica a textura por esta cor.
MATERIAIS = {
    "aco_azulado": ("branco", (0.64, 0.66, 0.70)),
    "aco_temperado": ("tempera", (1.0, 1.0, 1.0)),
    "madeira": ("madeira", (1.0, 1.0, 1.0)),
    "latao": ("branco", (0.88, 0.70, 0.40)),
    "chumbo": ("branco", (0.56, 0.56, 0.59)),
    "espoleta": ("branco", (0.80, 0.78, 0.74)),
    "clarao": ("branco", (1.0, 0.86, 0.48)),
}

# Os tipos de bala (mesma ordem e nomes do TipoBala.java): cor da ponta no modelo e no ícone (escura, clara)
TIPOS = [
    ("comum", "cartucho_45", (0.56, 0.56, 0.59), (0.45, 0.46, 0.50), (0.70, 0.71, 0.75)),
    ("aco", "cartucho_45_aco", (0.82, 0.84, 0.88), (0.66, 0.68, 0.73), (0.93, 0.94, 0.96)),
    ("diamante", "cartucho_45_diamante", (0.38, 0.90, 0.86), (0.20, 0.70, 0.68), (0.60, 0.97, 0.94)),
    ("netherite", "cartucho_45_netherite", (0.32, 0.27, 0.29), (0.22, 0.18, 0.20), (0.45, 0.40, 0.42)),
]

# Como o item aparece em cada lugar: giro (graus, XYZ como no JSON do Minecraft), escala e onde o centro da
# pegada deve cair (em blocos, no espaço da mão). A translação do JSON sai daqui. Ajuste fino depois de ver no
# jogo: é só mudar estes números e rodar de novo.
EXIBICAO = {
    "firstperson_righthand": {"giro": (0, 90, 0), "escala": 0.85, "palma": (-0.05, 0.03, 0.08)},
    "thirdperson_righthand": {"giro": (90, 90, 0), "escala": 0.40, "palma": (0.0, 0.06, 0.02)},
    "gui": {"giro": (0, 0, 0), "escala": 0.95, "centro": True},
    "ground": {"giro": (0, 0, 0), "escala": 0.40, "centro": True, "desloc": (0, 0.12, 0)},
    "fixed": {"giro": (0, 180, 0), "escala": 0.85, "centro": True},
}


CABECALHO = "# {nome}: gerado por ferramentas/exportar_modelo.py a partir do revolver-3d"
TEXTURAS_JSON = {"madeira": "revolver:item/revolver/madeira", "tempera": "revolver:item/revolver/tempera",
                 "branco": "revolver:item/revolver/branco", "particle": "revolver:item/revolver/madeira"}


def escrever_obj(nome, bm, materiais, emissivo=False, cores=None):
    return comum.escrever_obj("revolver", nome, bm, materiais, MATERIAIS, TEXTURAS_JSON, CABECALHO, emissivo, cores)


def exibicao(A):
    return comum.exibicao(A, PALMA, EXIBICAO)


def principal():
    cena = bpy.context.scene
    cena.frame_set(0)
    MODELOS.mkdir(parents=True, exist_ok=True)
    (TEXTURAS / "revolver").mkdir(parents=True, exist_ok=True)
    (ASSETS / "animacoes").mkdir(parents=True, exist_ok=True)
    for velho in MODELOS.glob("*"):
        velho.unlink()

    raiz = bpy.data.objects["revolver"]
    pecas = [o for o in bpy.data.objects if o.type == 'MESH']
    estaticas = [o for o in pecas if o.name not in ANIMADAS + MUNICAO]
    # centro do revólver em repouso, no espaço da raiz (sem o clarão, que em repouso tem escala zero)
    cantos = []
    for o in pecas:
        if o.name == "clarao":
            continue
        cantos += [raiz.matrix_world.inverted() @ o.matrix_world @ Vector(c) for c in o.bound_box]
    minimo = Vector([min(c[i] for c in cantos) for i in range(3)])
    maximo = Vector([max(c[i] for c in cantos) for i in range(3)])
    centro = (minimo + maximo) / 2
    A = matriz_item(raiz, centro)
    Ainv = A.inverted()
    print("tamanho em mm:", [round(v, 1) for v in (maximo - minimo)])

    # ---- geometria
    relatorio = []
    bm = bmesh.new()
    usados = []
    for o in estaticas:
        malha_no_item(o, A, bm, usados, "aco_azulado")
    relatorio.append(("corpo", escrever_obj("corpo", bm, usados), ", ".join(sorted(o.name for o in estaticas))))
    clarao = bpy.data.objects["clarao"]
    escala_clarao = clarao.scale.copy()
    clarao.scale = (1, 1, 1)             # em repouso ele é escala zero: exporta inteiro e esconde pelo quadro
    bpy.context.view_layer.update()
    rep_clarao = clarao.matrix_world.copy()
    for nome, fonte in [("cao", "cao"), ("gatilho", "gatilho"), ("tambor", "tambor"), ("portinhola", "portinhola"),
                        ("botao", "botao_ejetor"), ("clarao", "clarao"), ("estojo", "estojo_0")]:
        bm = bmesh.new()
        usados = []
        malha_no_item(bpy.data.objects[fonte], A, bm, usados, "aco_azulado")
        relatorio.append((nome, escrever_obj(nome, bm, usados, emissivo=(nome == "clarao")), fonte))
    # a mesma bala uma vez por tipo, só muda a cor do chumbo
    for tipo, _, ponta, _, _ in TIPOS:
        bm = bmesh.new()
        usados = []
        malha_no_item(bpy.data.objects["bala_0"], A, bm, usados, "aco_azulado")
        relatorio.append((f"bala_{tipo}", escrever_obj(f"bala_{tipo}", bm, usados, cores={"chumbo": ponta}),
                          "bala_0"))
    clarao.scale = escala_clarao
    bpy.context.view_layer.update()

    # ---- animações: matriz de cada peça em cada quadro, no espaço do item, relativa à pose exportada
    pistas = {"corpo": raiz, "cao": bpy.data.objects["cao"], "gatilho": bpy.data.objects["gatilho"],
              "tambor": bpy.data.objects["tambor"], "portinhola": bpy.data.objects["portinhola"],
              "botao": bpy.data.objects["botao_ejetor"], "clarao": clarao}
    for k in range(6):
        pistas[f"estojo{k}"] = bpy.data.objects[f"estojo_{k}"]
        pistas[f"bala{k}"] = bpy.data.objects[f"bala_{k}"]
    cena.frame_set(0)
    base = {}
    for nome, ob in pistas.items():
        if nome.startswith("estojo"):
            base[nome] = bpy.data.objects["estojo_0"].matrix_world.copy()
        elif nome.startswith("bala"):
            base[nome] = bpy.data.objects["bala_0"].matrix_world.copy()
        elif nome == "clarao":
            base[nome] = rep_clarao
        else:
            base[nome] = ob.matrix_world.copy()

    def amostra():
        return {nome: A @ ob.matrix_world @ base[nome].inverted() @ Ainv for nome, ob in pistas.items()}

    cena.frame_set(0)
    repouso = {n: m34(M) for n, M in amostra().items()}
    animacoes = {}
    for nome_anim, inicio, quadros in [("tiro", TIRO[0], TIRO[1]), ("recarregar", RECARGA[0], RECARGA[1])]:
        faixas = {n: [] for n in pistas}
        for q in range(quadros):
            cena.frame_set(inicio + q)
            for n, M in amostra().items():
                faixas[n].append(m34(M))
        # peça que não mexe na animação fica só no repouso
        faixas = {n: f for n, f in faixas.items() if any(m != repouso[n] for m in f)}
        animacoes[nome_anim] = {"quadros": quadros, "faixas": faixas}
    cena.frame_set(0)

    fim_blocos = REC_T0 + 6 * REC_BLOCO
    eventos = {
        "tiro": {"engatilha": 11, "disparo": 22, "fim": TIRO[1]},
        "recarregar": {"meio_armado": 10, "abre_portinhola": 16, "fecha_portinhola": fim_blocos + 6,
                       "engatilha": fim_blocos + 12, "desarma": fim_blocos + 28, "fim": RECARGA[1],
                       "blocos": [{"camara": (1 - i) % 6, "inicio": REC_T0 + i * REC_BLOCO,
                                   "empurra": REC_T0 + i * REC_BLOCO + 4, "estojo_some": REC_T0 + i * REC_BLOCO + 21,
                                   "bala_aparece": REC_T0 + i * REC_BLOCO + 22,
                                   "bala_entra": REC_T0 + i * REC_BLOCO + 36,
                                   "gira": REC_T0 + i * REC_BLOCO + 41} for i in range(6)]},
    }
    dados = {"_leia": "Gerado por ferramentas/exportar_modelo.py a partir do revolver-3d. Matriz 3x4 por linha, no "
                      "espaço do item, aplicada sobre a peça exportada; quadro 1 = índice 0.",
             "fps": FPS, "repouso": repouso, "animacoes": animacoes, "eventos": eventos}
    (ASSETS / "animacoes" / "revolver.json").write_text(json.dumps(dados, separators=(",", ":")),
                                                         encoding="utf-8")

    # ---- texturas e itens
    for nome in ("madeira", "tempera"):
        shutil.copyfile(ORIGEM / "modelo" / f"{nome}.png", TEXTURAS / "revolver" / f"{nome}.png")
    comum.textura_branca("revolver")
    for _, item_nome, _, escura, clara in TIPOS:
        comum.icone_cartucho(item_nome, escura, clara)
    item = {"parent": "builtin/entity", "gui_light": "front",
            "textures": {"particle": "revolver:item/revolver/madeira"}, "display": exibicao(A)}
    (ASSETS / "models" / "item" / "revolver.json").write_text(json.dumps(item, indent=2) + "\n", encoding="utf-8")
    for _, item_nome, _, _, _ in TIPOS:
        cartucho = {"parent": "minecraft:item/generated", "textures": {"layer0": f"revolver:item/{item_nome}"}}
        (ASSETS / "models" / "item" / f"{item_nome}.json").write_text(json.dumps(cartucho, indent=2) + "\n",
                                                                      encoding="utf-8")

    # ---- conferência: peça exportada (repouso) + matriz do quadro tem que cair onde o Blender põe a peça
    cena.frame_set(0)
    armacao = bpy.data.objects["armacao"]
    base_armacao = armacao.matrix_world.copy()
    conferir = {"cao": "cao", "tambor": "tambor", "portinhola": "portinhola", "estojo3": "estojo_3",
                "bala4": "bala_4", "corpo": "armacao"}
    pior = 0.0
    for nome_anim, inicio in [("tiro", TIRO[0]), ("recarregar", RECARGA[0])]:
        faixas = animacoes[nome_anim]["faixas"]
        for q in (0, 13, 21, 40, 120, 200, 300):
            if q >= animacoes[nome_anim]["quadros"]:
                continue
            cena.frame_set(inicio + q)
            for nome, fonte in conferir.items():
                lista = faixas.get(nome)
                M34 = lista[q] if lista else repouso[nome]
                M = Matrix((M34[0:4], M34[4:8], M34[8:12], (0, 0, 0, 1)))
                ob = bpy.data.objects[fonte]
                if ob.matrix_world.to_scale().length < 1e-6:
                    continue                      # escondida neste quadro
                origem = base_armacao if nome == "corpo" else base[nome]
                for v in list(ob.data.vertices)[::97]:
                    exportado = A @ origem @ v.co
                    real = A @ ob.matrix_world @ v.co
                    pior = max(pior, (M @ exportado - real).length)
    cena.frame_set(0)
    linhas = ["EXPORTAÇÃO DO REVÓLVER PARA O MOD", ""]
    linhas += [f"{n:<11} {t:6d} triângulos  ({f})" for n, t, f in relatorio]
    linhas += ["", "tiro: %d quadros, peças que mexem: %s" % (animacoes["tiro"]["quadros"],
                                                                ", ".join(animacoes["tiro"]["faixas"])),
               "recarregar: %d quadros, peças que mexem: %s" % (animacoes["recarregar"]["quadros"],
                                                                 ", ".join(animacoes["recarregar"]["faixas"])),
               "conferência (peça exportada + matriz do quadro contra o Blender): maior erro %.7f bloco" % pior,
               "exibição: " + json.dumps(exibicao(A))]
    (PASTA / "ferramentas" / "exportacao.txt").write_text("\n".join(linhas) + "\n", encoding="utf-8")
    print("\n".join(linhas))
    if pior > 1e-4:
        print("ERRO: a conferência falhou")
        sys.exit(1)


principal()
