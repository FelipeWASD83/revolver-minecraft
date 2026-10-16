"""Leva uma das armas novas (projetos <id>-3d) para dentro do mod.

Roda no Blender, com o .blend da arma e o id depois de `--`:

    "C:\\Program Files\\Blender Foundation\\Blender 5.2\\blender.exe" -b ..\\ak47-3d\\modelo\\ak47.blend --python ferramentas\\exportar_arma.py -- ak47

Depende do contrato que os projetos de modelo seguem (está no CLAUDE.md de cada um): o empty raiz com o id, na
pegada da mão; as peças que mexem com os nomes combinados; as faixas de NLA; e o `modelo/animacoes.json` com as faixas,
os eventos e a lista de peças. Escreve em src/main/resources/assets/revolver/:
- models/item/<id>/<peça>.obj + .mtl + .json: o corpo parado num só, uma peça móvel por arquivo, o clarão, o estojo
  (o mesmo para o que voa, o de cima do pente e o que entra no tubo da escopeta) e uma ponta de bala por tipo;
- textures/item/<id>/: branco e as texturas das peças (clareadas para o jogo);
- textures/item/<cartucho>*.png: os ícones dos cartuchos do calibre;
- models/item/<id>.json e os dos cartuchos;
- animacoes/<id>.json: as faixas como matriz 3x4 por peça por quadro, e os eventos.

As cores saem dos materiais do Blender, clareadas: o Minecraft não tem metal nem reflexo, e o escuro vira borrão.
"""

import json
import math
import sys
from pathlib import Path

import bmesh
import bpy
import numpy as np
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
import comum  # noqa: E402
from comum import ASSETS, PASTA, TEXTURAS, m34, malha_no_item, matriz_item  # noqa: E402

ID = sys.argv[sys.argv.index("--") + 1]
ORIGEM = PASTA.parent / f"{ID}-3d" / "modelo"
MODELOS = ASSETS / "models" / "item" / ID
CABECALHO = "# {nome}: gerado por ferramentas/exportar_arma.py a partir do " + ID + "-3d"
ESPECIAIS = {"clarao", "estojo_ejetado", "estojo_topo", "bala_topo", "cartucho_carga"}
ESCONDIDAS = ["clarao", "estojo_ejetado", "cartucho_carga"]

# Por arma: o nome do item dos cartuchos, o tamanho na mão (comprimento que a arma ocupa na tela, em blocos) e o ícone
# do cartucho (pontas: base e ponta do desenho; larguras do aro e do corpo; onde começa a ponta; cores do corpo).
LATAO = ((0.86, 0.66, 0.30), (0.56, 0.39, 0.15), (0.98, 0.86, 0.55))
CONFIG = {
    "ak47": {"cartucho": "cartucho_762x39", "mao": 1.45,
             "icone": dict(a=(3.0, 13.0), b=(13.0, 3.0), aro=1.6, corpo=1.5, inicio_ponta=0.62, curva_ponta=0.8)},
    "m4a4": {"cartucho": "cartucho_556", "mao": 1.45,
             "icone": dict(a=(3.2, 12.8), b=(12.8, 3.2), aro=1.4, corpo=1.25, inicio_ponta=0.66, curva_ponta=0.82)},
    "m4a1": {"cartucho": "cartucho_556", "mao": 1.5,
             "icone": dict(a=(3.2, 12.8), b=(12.8, 3.2), aro=1.4, corpo=1.25, inicio_ponta=0.66, curva_ponta=0.82)},
    "metralhadora": {"cartucho": "cartucho_762x51", "mao": 1.7,
                     "icone": dict(a=(2.5, 13.5), b=(13.5, 2.5), aro=1.6, corpo=1.5, inicio_ponta=0.66,
                                   curva_ponta=0.82)},
    "escopeta": {"cartucho": "cartucho_12", "mao": 1.45,
                 "icone": dict(a=(4.0, 12.0), b=(12.0, 4.0), aro=2.6, corpo=2.4, inicio_ponta=0.86, curva_ponta=0.95,
                               corpo_cores=((0.78, 0.12, 0.10), (0.52, 0.06, 0.05), (0.92, 0.32, 0.26)),
                               base_ate=0.3)},
    "sniper": {"cartucho": "cartucho_338", "mao": 1.8,
               "icone": dict(a=(2.5, 13.5), b=(13.5, 2.5), aro=1.9, corpo=1.8, inicio_ponta=0.68, curva_ponta=0.82)},
}
# cor da ponta de cada tipo (mesma ordem do TipoBala.java): no modelo e no ícone (escura, clara)
TIPOS = [
    ("comum", "", (0.84, 0.52, 0.34), (0.66, 0.36, 0.20), (0.93, 0.66, 0.46)),
    ("aco", "_aco", (0.82, 0.84, 0.88), (0.66, 0.68, 0.73), (0.93, 0.94, 0.96)),
    ("diamante", "_diamante", (0.38, 0.90, 0.86), (0.20, 0.70, 0.68), (0.60, 0.97, 0.94)),
    ("netherite", "_netherite", (0.32, 0.27, 0.29), (0.22, 0.18, 0.20), (0.45, 0.40, 0.42)),
]


def clarear(rgb):
    """Cor do Cycles -> cor chapada do jogo: o brilho v vira 0,24 + 1,4 v (até 0,92), mantendo o tom. Dá o inox 0,40
    -> 0,80, o aço escuro 0,025 -> 0,27 e o latão no amarelo, como nas cores escolhidas à mão para a deagle."""
    v = max(rgb)
    if v < 1e-6:
        return (0.24, 0.24, 0.24)
    alvo = min(0.92, 0.24 + 1.4 * v)
    return tuple(min(1.0, c * alvo / v) for c in rgb)


def tabela_materiais():
    """Material -> (textura, cor no jogo). Material com imagem vira uma textura própria, clareada pixel a pixel."""
    tabela = {}
    for m in bpy.data.materials:
        if not m.use_nodes or "Principled BSDF" not in m.node_tree.nodes:
            continue
        b = m.node_tree.nodes["Principled BSDF"]
        imagem = None
        for link in m.node_tree.links:
            if link.to_node == b and link.to_socket.name == "Base Color" and link.from_node.type == 'TEX_IMAGE':
                imagem = link.from_node.image
        if imagem is not None:
            nome = "".join(ch if ch.isalnum() else "_" for ch in imagem.name.lower().rsplit(".", 1)[0])
            salvar_textura(imagem, nome)
            tabela[m.name] = (nome, (1.0, 1.0, 1.0))
        else:
            tabela[m.name] = ("branco", clarear(tuple(b.inputs["Base Color"].default_value[:3])))
    return tabela


def salvar_textura(imagem, nome, lado=128):
    """A imagem do material reduzida a 128 px e clareada como as cores."""
    w, h = imagem.size
    px = np.array(imagem.pixels[:], dtype=np.float32).reshape(h, w, 4)
    passo_y, passo_x = max(1, h // lado), max(1, w // lado)
    px = px[::passo_y, ::passo_x][:lado, :lado].copy()
    v = px[..., :3].max(axis=-1, keepdims=True)
    alvo = np.minimum(0.92, 0.24 + 1.4 * v)
    px[..., :3] = np.clip(px[..., :3] * np.where(v > 1e-6, alvo / np.maximum(v, 1e-6), 1.0), 0, 1)
    px[..., 3] = 1.0
    img = bpy.data.images.new(nome, px.shape[1], px.shape[0])
    img.pixels.foreach_set(px.ravel())
    img.filepath_raw = str(TEXTURAS / ID / f"{nome}.png")
    img.file_format = 'PNG'
    img.save()


def principal():
    cfg = CONFIG[ID]
    contrato = json.loads((ORIGEM / "animacoes.json").read_text(encoding="utf-8"))
    cena = bpy.context.scene
    cena.frame_set(0)
    MODELOS.mkdir(parents=True, exist_ok=True)
    (TEXTURAS / ID).mkdir(parents=True, exist_ok=True)
    (ASSETS / "animacoes").mkdir(parents=True, exist_ok=True)
    for velho in MODELOS.glob("*"):
        velho.unlink()
    ob = bpy.data.objects
    raiz = ob[contrato["raiz"]]
    moveis = [p for p in contrato["pecas"] if p not in ESPECIAIS]
    escondidas = [n for n in ESCONDIDAS if n in ob]
    malhas = [o for o in ob if o.type == 'MESH']
    estaticas = [o for o in malhas if o.name not in set(moveis) | ESPECIAIS]

    cantos = []
    for o in malhas:
        if o.name in escondidas:
            continue
        cantos += [raiz.matrix_world.inverted() @ o.matrix_world @ Vector(c) for c in o.bound_box]
    minimo = Vector([min(c[i] for c in cantos) for i in range(3)])
    maximo = Vector([max(c[i] for c in cantos) for i in range(3)])
    centro = (minimo + maximo) / 2
    A = matriz_item(raiz, centro)
    Ainv = A.inverted()
    comprimento = (maximo - minimo).x * comum.ESCALA
    tabela = tabela_materiais()
    texturas_json = {nome: f"revolver:item/{ID}/{nome}" for nome, _ in set(tabela.values())}
    texturas_json["branco"] = f"revolver:item/{ID}/branco"
    texturas_json["particle"] = f"revolver:item/{ID}/branco"

    def escrever(nome, bm, usados, emissivo=False, cores=None):
        return comum.escrever_obj(ID, nome, bm, usados, tabela, texturas_json, CABECALHO, emissivo, cores)

    guardadas = {}
    for nome in escondidas:
        guardadas[nome] = ob[nome].scale.copy()
        ob[nome].scale = (1, 1, 1)
    bpy.context.view_layer.update()
    relatorio = []
    bm = bmesh.new()
    usados = []
    for o in estaticas:
        malha_no_item(o, A, bm, usados, "branco")
    relatorio.append(("corpo", escrever("corpo", bm, usados), ", ".join(sorted(o.name for o in estaticas))))
    for nome in moveis + ["clarao", "estojo"]:
        fonte = "estojo_ejetado" if nome == "estojo" else nome
        bm = bmesh.new()
        usados = []
        malha_no_item(ob[fonte], A, bm, usados, "branco")
        relatorio.append((nome, escrever(nome, bm, usados, emissivo=(nome == "clarao")), fonte))
    if "bala_topo" in ob:
        mats_bala = [m.name for m in ob["bala_topo"].data.materials if m]
        for tipo, _, ponta, _, _ in TIPOS:
            bm = bmesh.new()
            usados = []
            malha_no_item(ob["bala_topo"], A, bm, usados, "branco")
            relatorio.append((f"bala_{tipo}", escrever(f"bala_{tipo}", bm, usados,
                                                       cores={m: ponta for m in mats_bala}), "bala_topo"))
    base_escondida = {nome: ob[nome].matrix_world.copy() for nome in escondidas}
    for nome, esc in guardadas.items():
        ob[nome].scale = esc
    bpy.context.view_layer.update()

    # ---- animações
    pistas = {"corpo": raiz, "clarao": ob["clarao"], "ejetado": ob["estojo_ejetado"]}
    for nome in moveis:
        pistas[nome] = ob[nome]
    if "estojo_topo" in ob:
        pistas["topo_estojo"] = ob["estojo_topo"]
        pistas["topo_bala"] = ob["bala_topo"]
    if "cartucho_carga" in ob:
        pistas["carga"] = ob["cartucho_carga"]
    base = {}
    for nome, o in pistas.items():
        if nome == "clarao":
            base[nome] = base_escondida["clarao"]
        elif nome in ("ejetado", "topo_estojo", "carga"):
            base[nome] = base_escondida["estojo_ejetado"]      # todas usam a malha do estojo exportado
        else:
            base[nome] = o.matrix_world.copy()

    def amostra():
        return {n: A @ o.matrix_world @ base[n].inverted() @ Ainv for n, o in pistas.items()}

    cena.frame_set(0)
    repouso = {n: m34(M) for n, M in amostra().items()}
    animacoes = {}
    for nome_anim, faixa in contrato["faixas"].items():
        if nome_anim == "repouso":
            continue
        faixas = {n: [] for n in pistas}
        for q in range(faixa["quadros"]):
            cena.frame_set(faixa["inicio"] + q)
            for n, M in amostra().items():
                faixas[n].append(m34(M))
        faixas = {n: f for n, f in faixas.items() if any(m != repouso[n] for m in f)}
        animacoes[nome_anim] = {"quadros": faixa["quadros"], "faixas": faixas}
    cena.frame_set(0)
    eventos = {n: f.get("eventos", {}) for n, f in contrato["faixas"].items() if n != "repouso"}
    dados = {"_leia": f"Gerado por ferramentas/exportar_arma.py a partir do {ID}-3d. Matriz 3x4 por linha, no espaço "
                      "do item, aplicada sobre a peça exportada; quadro 1 = índice 0.",
             "fps": 30, "pecas": moveis, "repouso": repouso, "animacoes": animacoes, "eventos": eventos}
    (ASSETS / "animacoes" / f"{ID}.json").write_text(json.dumps(dados, separators=(",", ":")), encoding="utf-8")

    # ---- texturas, ícones e itens
    comum.textura_branca(ID)
    for _, sufixo, _, escura, clara in TIPOS:
        comum.icone_cartucho(cfg["cartucho"] + sufixo, escura, clara, **cfg["icone"])
    palma = raiz.matrix_world.translation.copy()                  # a raiz fica na pegada da mão
    escala_mao = round(cfg["mao"] / comprimento, 3)
    exibicao = {
        "firstperson_righthand": {"giro": (0, 90, 0), "escala": escala_mao, "palma": (-0.05, 0.03, 0.08)},
        "thirdperson_righthand": {"giro": (90, 90, 0), "escala": 0.32, "palma": (0.0, 0.06, 0.02)},
        "gui": {"giro": (0, 0, 0), "escala": round(0.95 / comprimento, 3), "centro": True},
        "ground": {"giro": (0, 0, 0), "escala": 0.25, "centro": True, "desloc": (0, 0.12, 0)},
        "fixed": {"giro": (0, 180, 0), "escala": round(0.9 / comprimento, 3), "centro": True},
    }
    item = {"parent": "builtin/entity", "gui_light": "front",
            "textures": {"particle": f"revolver:item/{ID}/branco"}, "display": comum.exibicao(A, palma, exibicao)}
    (ASSETS / "models" / "item" / f"{ID}.json").write_text(json.dumps(item, indent=2) + "\n", encoding="utf-8")
    for _, sufixo, _, _, _ in TIPOS:
        nome = cfg["cartucho"] + sufixo
        (ASSETS / "models" / "item" / f"{nome}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"revolver:item/{nome}"}}, indent=2) + "\n",
            encoding="utf-8")

    # ---- conferência: peça exportada (repouso) + matriz do quadro tem que cair onde o Blender põe a peça
    pior = 0.0
    for nome_anim, faixa in contrato["faixas"].items():
        if nome_anim == "repouso":
            continue
        fx = animacoes[nome_anim]["faixas"]
        for q in sorted({0, 1, 2, faixa["quadros"] // 3, faixa["quadros"] // 2, faixa["quadros"] - 1}):
            cena.frame_set(faixa["inicio"] + q)
            for nome, o in pistas.items():
                if nome == "corpo" or o.matrix_world.to_scale().length < 1e-6:
                    continue
                lista = fx.get(nome)
                M34 = lista[q] if lista else repouso[nome]
                M = Matrix((M34[0:4], M34[4:8], M34[8:12], (0, 0, 0, 1)))
                for v in list(o.data.vertices)[::97]:
                    exportado = A @ base[nome] @ v.co
                    real = A @ o.matrix_world @ v.co
                    pior = max(pior, (M @ exportado - real).length)
    cena.frame_set(0)
    linhas = [f"EXPORTAÇÃO DA ARMA {ID} PARA O MOD", "", f"comprimento: {comprimento:.2f} blocos", ""]
    linhas += [f"{n:<15} {t:6d} triângulos  ({f})" for n, t, f in relatorio]
    linhas.append("")
    for nome_anim, a in animacoes.items():
        linhas.append("%s: %d quadros, eventos %s" % (nome_anim, a["quadros"], json.dumps(eventos.get(nome_anim))))
    linhas += ["peças que mexem: " + ", ".join(moveis),
               "conferência (peça exportada + matriz do quadro contra o Blender): maior erro %.7f bloco" % pior]
    (PASTA / "ferramentas" / f"exportacao-{ID}.txt").write_text("\n".join(linhas) + "\n", encoding="utf-8")
    print("\n".join(linhas))
    if pior > 1e-4:
        print("ERRO: a conferência falhou")
        sys.exit(1)


principal()
