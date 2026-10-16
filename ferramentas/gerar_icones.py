r"""Gera o ícone em pixel art de cada arma (o que aparece na barra de itens e no inventário), a partir do próprio modelo.

Roda no Blender:

    "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" -b --factory-startup --python ferramentas\gerar_icones.py

Para cada arma, monta as peças exportadas (os OBJ do mod, na pose parada), fotografa de lado em projeção ortográfica
com as cores do jogo, reduz a 32 x 32 (cada pixel é a média do bloco que ele cobre), arredonda as cores para poucos
tons e põe o contorno escuro em volta, como os itens do Minecraft. Grava:
- textures/item/icone/<arma>.png (32 x 32) e models/item/icone/<arma>.json (item/generated);
- ferramentas/saida/icone-<arma>.png, o mesmo ícone ampliado 16 vezes, para conferir.
"""

import json
import math
import struct
import zlib
from pathlib import Path

import bpy
import numpy as np
from mathutils import Vector

PASTA = Path(__file__).resolve().parent.parent
ASSETS = PASTA / "src" / "main" / "resources" / "assets" / "revolver"
MODELOS = ASSETS / "models" / "item"
SAIDA = PASTA / "ferramentas" / "saida"
LADO = 32                       # o ícone
CELULA = 20                     # pixels do render por pixel do ícone
ARMAS = ["revolver", "deagle", "ak47", "m4a4", "m4a1", "metralhadora", "escopeta", "sniper"]
PECAS_FIXAS = {
    "revolver": ["corpo", "cao", "gatilho", "tambor", "portinhola", "botao"],
    "deagle": ["corpo", "ferrolho", "cao", "gatilho", "trava", "botao", "pente"],
}


def pecas_de(arma):
    if arma in PECAS_FIXAS:
        return PECAS_FIXAS[arma]
    anim = json.loads((ASSETS / "animacoes" / f"{arma}.json").read_text(encoding="utf-8"))
    return ["corpo"] + anim["pecas"]


def cor_media(caminho_textura):
    """Cor média de uma textura (para a peça de madeira ou borracha virar uma cor só)."""
    img = bpy.data.images.load(str(caminho_textura), check_existing=True)
    px = np.array(img.pixels[:], dtype=np.float32).reshape(-1, 4)
    return tuple(float(c) for c in px[:, :3].mean(axis=0))


def ler_obj(arma, nome):
    """Vértices, faces e a cor de cada material (Kd vezes a cor média da textura) de uma peça exportada."""
    texturas = json.loads((MODELOS / arma / f"{nome}.json").read_text(encoding="utf-8"))["textures"]
    cores, atual, kd = {}, None, {}
    for linha in (MODELOS / arma / f"{nome}.mtl").read_text(encoding="utf-8").splitlines():
        p = linha.split()
        if not p:
            continue
        if p[0] == "newmtl":
            atual = p[1]
        elif p[0] == "Kd":
            kd[atual] = tuple(float(v) for v in p[1:4])
        elif p[0] == "map_Kd":
            ref = texturas[p[1][1:]].split(":")[1]
            media = cor_media(ASSETS / "textures" / f"{ref}.png")
            cores[atual] = tuple(a * b for a, b in zip(kd[atual], media))
    vs, faces, mats, mat = [], [], [], None
    for linha in (MODELOS / arma / f"{nome}.obj").read_text(encoding="utf-8").splitlines():
        if linha.startswith("v "):
            vs.append(tuple(float(c) for c in linha.split()[1:4]))
        elif linha.startswith("usemtl"):
            mat = linha.split()[1]
        elif linha.startswith("f "):
            faces.append([int(c.split("/")[0]) - 1 for c in linha.split()[1:]])
            mats.append(mat)
    return vs, faces, mats, cores


def montar(arma):
    for o in list(bpy.data.objects):
        if o.type == 'MESH':
            bpy.data.objects.remove(o)
    todos = []
    for nome in pecas_de(arma):
        vs, faces, mats, cores = ler_obj(arma, nome)
        me = bpy.data.meshes.new(nome)
        me.from_pydata(vs, [], faces)
        nomes = sorted(set(mats))
        for n in nomes:
            m = bpy.data.materials.new(f"{arma}_{nome}_{n}")
            m.diffuse_color = (*cores.get(n, (0.6, 0.6, 0.6)), 1)
            me.materials.append(m)
        for pol, n in zip(me.polygons, mats):
            pol.material_index = nomes.index(n)
        ob = bpy.data.objects.new(nome, me)
        bpy.context.scene.collection.objects.link(ob)
        todos += vs
    return Vector([min(v[i] for v in todos) for i in range(3)]), Vector([max(v[i] for v in todos) for i in range(3)])


def png(caminho, rgba8):
    altura, largura, _ = rgba8.shape
    linhas = b"".join(b"\x00" + rgba8[y].tobytes() for y in range(altura))

    def bloco(tipo, dados):
        return struct.pack(">I", len(dados)) + tipo + dados + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF)

    caminho.write_bytes(b"\x89PNG\r\n\x1a\n" + bloco(b"IHDR", struct.pack(">IIBBBBB", largura, altura, 8, 6, 0, 0, 0))
                        + bloco(b"IDAT", zlib.compress(linhas, 9)) + bloco(b"IEND", b""))


def pixelar(render):
    """Render (alto, com alfa) -> ícone 32 x 32: média por bloco, poucos tons e contorno escuro."""
    h, w, _ = render.shape
    blocos = render.reshape(LADO, h // LADO, LADO, w // LADO, 4)
    alfa = blocos[..., 3].mean(axis=(1, 3))
    peso = blocos[..., 3:4]
    cor = (blocos[..., :3] * peso).sum(axis=(1, 3)) / np.maximum(peso.sum(axis=(1, 3)), 1e-6)
    cheio = alfa > 0.3                                # 0,3 e não 0,5: o cano fino não some
    cor = np.clip((cor - 0.5) * 1.15 + 0.5, 0, 1)     # um pouco mais de contraste, como pixel art
    cinza = cor.mean(axis=-1, keepdims=True)
    cor = np.clip(cinza + (cor - cinza) * 1.35, 0, 1)  # e mais cor (a madeira, o polímero, a caixa verde)
    # o chanfro do pixel art: a borda de cima clareia e a de baixo escurece
    cima = cheio & ~np.roll(cheio, 1, axis=0)
    baixo = cheio & ~np.roll(cheio, -1, axis=0)
    cor[cima] = cor[cima] + (1 - cor[cima]) * 0.35
    cor[baixo] = cor[baixo] * 0.72
    cor = np.round(cor * 14) / 14                     # poucos tons
    icone = np.zeros((LADO, LADO, 4), dtype=np.float32)
    icone[cheio, :3] = cor[cheio]
    icone[cheio, 3] = 1.0
    # contorno: o vizinho vazio de um pixel cheio fica escuro, puxado da cor dele
    contorno = np.zeros_like(cheio)
    tom = np.zeros((LADO, LADO, 3), dtype=np.float32)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        viz = np.roll(np.roll(cheio, dy, axis=0), dx, axis=1)
        cor_viz = np.roll(np.roll(icone[..., :3], dy, axis=0), dx, axis=1)
        novo = viz & ~cheio & ~contorno
        tom[novo] = cor_viz[novo]
        contorno |= novo
    icone[contorno, :3] = tom[contorno] * 0.3
    icone[contorno, 3] = 1.0
    return icone


def principal():
    SAIDA.mkdir(parents=True, exist_ok=True)
    (ASSETS / "textures" / "item" / "icone").mkdir(parents=True, exist_ok=True)
    (MODELOS / "icone").mkdir(parents=True, exist_ok=True)
    cena = bpy.context.scene
    cena.render.engine = 'BLENDER_WORKBENCH'
    cena.display.shading.light = 'STUDIO'
    cena.display.shading.color_type = 'MATERIAL'
    cena.display.shading.show_specular_highlight = False
    cena.view_settings.view_transform = 'Standard'
    cena.render.film_transparent = True
    cena.render.resolution_x = cena.render.resolution_y = LADO * CELULA
    cena.render.image_settings.color_mode = 'RGBA'
    cam = bpy.data.objects.new("olho", bpy.data.cameras.new("olho"))
    cena.collection.objects.link(cam)
    cena.camera = cam
    cam.data.type = 'ORTHO'
    for arma in ARMAS:
        minimo, maximo = montar(arma)
        centro = (minimo + maximo) / 2
        extensao = max(maximo.x - minimo.x, maximo.y - minimo.y)
        # o lado direito da arma fica em +Z no espaço do item: a câmera olha dali, com uma margem de um pixel
        cam.location = (centro.x, centro.y, maximo.z + 5)
        cam.rotation_euler = (0, 0, 0)
        cam.data.ortho_scale = extensao * LADO / (LADO - 2)
        cam.data.clip_end = 100
        arquivo = SAIDA / f"_render-{arma}.png"
        cena.render.filepath = str(arquivo)
        bpy.ops.render.render(write_still=True)
        img = bpy.data.images.load(str(arquivo), check_existing=False)
        px = np.array(img.pixels[:], dtype=np.float32).reshape(LADO * CELULA, LADO * CELULA, 4)[::-1]
        bpy.data.images.remove(img)
        arquivo.unlink()
        icone = pixelar(px)
        rgba8 = np.round(icone * 255).astype(np.uint8)
        png(ASSETS / "textures" / "item" / "icone" / f"{arma}.png", rgba8)
        png(SAIDA / f"icone-{arma}.png", np.repeat(np.repeat(rgba8, 16, axis=0), 16, axis=1))
        modelo = {"parent": "minecraft:item/generated", "textures": {"layer0": f"revolver:item/icone/{arma}"}}
        (MODELOS / "icone" / f"{arma}.json").write_text(json.dumps(modelo, indent=2) + "\n", encoding="utf-8")
        print(arma, "ok")
        barra.append(rgba8)
    # a barra de itens de mentira, no tamanho da tela com a interface em 4 (o ícone com 64 pixels)
    slot, borda = 72, 4
    imagem = np.zeros((slot + 2 * borda, len(barra) * slot + 2 * borda, 4), dtype=np.uint8)
    imagem[..., :3], imagem[..., 3] = 60, 255
    for k, rgba8 in enumerate(barra):
        x0, y0 = borda + k * slot, borda
        imagem[y0 + 2:y0 + slot - 2, x0 + 2:x0 + slot - 2, :3] = 139
        grande = np.repeat(np.repeat(rgba8, 2, axis=0), 2, axis=1).astype(np.float32)
        a = grande[..., 3:4] / 255
        alvo = imagem[y0 + 4:y0 + 68, x0 + 4:x0 + 68, :3].astype(np.float32)
        imagem[y0 + 4:y0 + 68, x0 + 4:x0 + 68, :3] = (grande[..., :3] * a + alvo * (1 - a)).astype(np.uint8)
    png(SAIDA / "icones-barra.png", imagem)


barra = []
principal()
