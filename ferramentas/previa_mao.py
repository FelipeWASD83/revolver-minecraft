"""Prévia de como o revólver aparece na mão, sem abrir o jogo.

Roda no Blender:

    "C:\\Program Files\\Blender Foundation\\Blender 5.2\\blender.exe" -b --factory-startup --python ferramentas\\previa_mao.py

Monta as peças exportadas (os OBJ do mod) com a mesma conta que o Minecraft 1.21.1 faz para um item na mão
direita em primeira pessoa (ItemInHandRenderer.applyItemArmTransform + a exibição do JSON do item + o
deslocamento de meio bloco) e renderiza do olho do jogador, com campo de visão de 70° e a mira no centro.
Também faz o inventário (vista "gui"). Sai em ferramentas/saida/previa-*.png. Serve para conferir posição e
ângulo; luz e cor não são as do jogo.
"""

import json
import math
from pathlib import Path

import bpy
from mathutils import Matrix, Vector

PASTA = Path(__file__).resolve().parent.parent
MODELOS = PASTA / "src" / "main" / "resources" / "assets" / "revolver" / "models" / "item"
SAIDA = PASTA / "ferramentas" / "saida"
CORES = {"madeira": (0.45, 0.22, 0.1), "tempera": (0.55, 0.55, 0.57)}


def ler_obj(nome):
    """OBJ cru (sem converter eixos): vértices, faces e a cor de cada material."""
    cores = {}
    mtl = (MODELOS / "revolver" / f"{nome}.mtl").read_text(encoding="utf-8").splitlines()
    atual = None
    for linha in mtl:
        p = linha.split()
        if not p:
            continue
        if p[0] == "newmtl":
            atual = p[1]
        elif p[0] == "Kd":
            cores[atual] = tuple(float(v) for v in p[1:4])
        elif p[0] == "map_Kd" and p[1][1:] in CORES:
            cores[atual] = CORES[p[1][1:]]
    vs, faces, mats = [], [], []
    mat = None
    for linha in (MODELOS / "revolver" / f"{nome}.obj").read_text(encoding="utf-8").splitlines():
        if linha.startswith("v "):
            vs.append(tuple(float(c) for c in linha.split()[1:4]))
        elif linha.startswith("usemtl"):
            mat = linha.split()[1]
        elif linha.startswith("f "):
            faces.append([int(c.split("/")[0]) - 1 for c in linha.split()[1:]])
            mats.append(mat)
    return vs, faces, mats, cores


def objeto(nome, M):
    vs, faces, mats, cores = ler_obj(nome)
    me = bpy.data.meshes.new(nome)
    me.from_pydata([M @ Vector(v) for v in vs], [], faces)
    nomes = sorted(set(mats))
    for n in nomes:
        m = bpy.data.materials.new(f"{nome}_{n}")
        m.diffuse_color = (*cores.get(n, (0.6, 0.6, 0.6)), 1)
        me.materials.append(m)
    for p, n in zip(me.polygons, mats):
        p.material_index = nomes.index(n)
    ob = bpy.data.objects.new(nome, me)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def exibicao(contexto):
    d = json.loads((MODELOS / "revolver.json").read_text(encoding="utf-8"))["display"][contexto]
    t = Vector(d["translation"]) / 16
    rx, ry, rz = (math.radians(a) for a in d["rotation"])
    R = (Matrix.Rotation(rx, 4, 'X') @ Matrix.Rotation(ry, 4, 'Y') @ Matrix.Rotation(rz, 4, 'Z'))
    s = d["scale"][0]
    return Matrix.Translation(t) @ R @ Matrix.Scale(s, 4) @ Matrix.Translation((-0.5, -0.5, -0.5))


def montar(M):
    for o in list(bpy.data.objects):
        if o.type == 'MESH':
            bpy.data.objects.remove(o)
    for peca in ("corpo", "cao", "gatilho", "tambor", "portinhola", "botao"):
        objeto(peca, M)


def principal():
    SAIDA.mkdir(parents=True, exist_ok=True)
    cena = bpy.context.scene
    cena.render.engine = 'BLENDER_WORKBENCH'
    cena.display.shading.light = 'STUDIO'
    cena.display.shading.color_type = 'MATERIAL'
    cena.render.resolution_x, cena.render.resolution_y = 1280, 720
    cena.world = bpy.data.worlds.new("ceu")
    cena.world.color = (0.45, 0.62, 0.85)

    cam = bpy.data.objects.new("olho", bpy.data.cameras.new("olho"))
    cena.collection.objects.link(cam)
    cena.camera = cam
    cam.data.sensor_fit = 'VERTICAL'
    cam.data.angle = math.radians(70)
    cam.data.clip_start = 0.01

    # primeira pessoa, mão direita: o espaço do Minecraft (X direita, Y cima, olhando para -Z) vai direto
    # para o Blender com a câmera na origem sem giro, que também olha para -Z com Y para cima
    braco = Matrix.Translation((0.56, -0.52, -0.72))
    montar(braco @ exibicao("firstperson_righthand"))
    cena.render.filepath = str(SAIDA / "previa-primeira-pessoa.png")
    bpy.ops.render.render(write_still=True)

    # inventário: o item visto de frente, num quadrado de um bloco
    montar(exibicao("gui"))
    cam.location = (0, 0, 3)
    cam.data.type = 'ORTHO'
    cam.data.ortho_scale = 1.0
    cena.render.resolution_x = cena.render.resolution_y = 512
    cena.render.filepath = str(SAIDA / "previa-inventario.png")
    bpy.ops.render.render(write_still=True)


principal()
