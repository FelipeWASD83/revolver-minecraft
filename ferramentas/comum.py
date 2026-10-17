"""O que os exportadores do revólver e da deagle têm em comum: levar a peça do Blender para o espaço do item,
escrever o OBJ do carregador do NeoForge, as matrizes das animações, a posição na mão e os ícones dos cartuchos.

Espaço do item: 1 = um bloco; a arma fica centrada em (0,5; 0,5; 0,5), com o cano para +X, o topo para +Y e o lado
direito para +Z, que é o lado que aparece no inventário. 250 mm do modelo = 1 bloco nas duas armas (a deagle sai
um pouco maior que o revólver, como na vida real).
"""

import json
import math
from pathlib import Path

import bmesh
import bpy
from mathutils import Matrix, Vector

PASTA = Path(__file__).resolve().parent.parent
ASSETS = PASTA / "src" / "main" / "resources" / "assets" / "revolver"
TEXTURAS = ASSETS / "textures" / "item"
ESCALA = 1 / 250.0
# Otimização (2.2.0, pedido dele): o jogo recebe uma versão leve do modelo do Blender, reduzida por colapso de
# arestas (leva UV e material junto). Cada peça tenta as frações de RAZOES em ordem e fica com a primeira cuja malha
# não se afasta da original mais que TOLERANCIA (nos dois sentidos: detalhe que sumiu e ponta que apareceu); peça
# pequena não cai abaixo de MINIMO_TRIS. Cortar a mesma fração de tudo destruía a mira da deagle e a bolinha do
# ferrolho da sniper. As texturas descem para LADO_TEXTURA px. Os projetos <arma>-3d continuam com o detalhe todo.
RAZOES = (0.25, 0.35, 0.5, 0.7)
TOLERANCIA = 0.003          # blocos: 0,75 mm no modelo, menos de um pixel na mão
MINIMO_TRIS = 160
LADO_TEXTURA = 64


def _distancia_maxima(pontos, arvore):
    pior = 0.0
    for p in pontos:
        achado = arvore.find_nearest(p)
        if achado[0] is not None:
            pior = max(pior, achado[3])
    return pior


def reduzir(bm):
    """Deixa o bmesh low poly com o menor número de triângulos que ainda respeita a TOLERANCIA."""
    from mathutils.bvhtree import BVHTree
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 3], quad_method='BEAUTY',
                          ngon_method='BEAUTY')
    original = len(bm.faces)
    if original <= MINIMO_TRIS:
        return
    arvore_original = BVHTree.FromBMesh(bm)
    pontos_originais = [v.co.copy() for v in bm.verts]
    me = bpy.data.meshes.new("_reduzir")
    bm.to_mesh(me)
    ob = bpy.data.objects.new("_reduzir", me)
    bpy.context.scene.collection.objects.link(ob)
    mod = ob.modifiers.new("reduzir", 'DECIMATE')
    mod.decimate_type = 'COLLAPSE'
    mod.use_collapse_triangulate = True
    escolhida = None
    try:
        for razao in RAZOES:
            alvo = max(MINIMO_TRIS, int(original * razao))
            if alvo >= original:
                break
            mod.ratio = alvo / original
            dg = bpy.context.evaluated_depsgraph_get()
            dg.update()
            ev = ob.evaluated_get(dg)
            reduzida = ev.to_mesh()
            vs = [v.co.copy() for v in reduzida.vertices]
            arvore = BVHTree.FromPolygons(vs, [p.vertices[:] for p in reduzida.polygons])
            erro = max(_distancia_maxima(pontos_originais, arvore), _distancia_maxima(vs, arvore_original))
            if erro <= TOLERANCIA:
                escolhida = bmesh.new()
                escolhida.from_mesh(reduzida)
            ev.to_mesh_clear()
            if escolhida is not None:
                break
    finally:
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)
    if escolhida is None:              # nenhuma fração passou: a peça fica como veio
        return
    tmp = bpy.data.meshes.new("_tmp")
    escolhida.to_mesh(tmp)
    escolhida.free()
    bm.clear()
    bm.from_mesh(tmp)
    bpy.data.meshes.remove(tmp)


def reduzir_textura(caminho, lado=LADO_TEXTURA):
    """Regrava o PNG em `lado` x `lado` (média de pixels, não o vizinho mais próximo)."""
    img = bpy.data.images.load(str(caminho))
    if img.size[0] > lado or img.size[1] > lado:
        img.scale(lado, lado)
        img.filepath_raw = str(caminho)
        img.file_format = 'PNG'
        img.save()
    bpy.data.images.remove(img)


def matriz_item(raiz, centro):
    """Espaço do .blend (mm, Z para cima) -> espaço do item (blocos, Y para cima, centrado em 0,5)."""
    troca = Matrix(((1, 0, 0, 0), (0, 0, 1, 0), (0, -1, 0, 0), (0, 0, 0, 1)))   # (x, y, z) -> (x, z, -y)
    return (Matrix.Translation((0.5, 0.5, 0.5)) @ Matrix.Scale(ESCALA, 4) @ troca
            @ Matrix.Translation(-centro) @ raiz.matrix_world.inverted())


def malha_no_item(ob, A, bm_destino, materiais_usados, padrao):
    """Põe a malha da peça (na pose de repouso) no espaço do item, dentro de um bmesh com UV. Espaço de material
    vazio (o booleano deixa alguns) vira o material `padrao`."""
    dg = bpy.context.evaluated_depsgraph_get()
    ev = ob.evaluated_get(dg)
    me = ev.to_mesh()
    M = A @ ev.matrix_world
    bm = bmesh.new()
    bm.from_mesh(me)
    bm.transform(M)
    if M.determinant() < 0:
        bmesh.ops.reverse_faces(bm, faces=bm.faces[:])
    nomes = [m.name if m else padrao for m in me.materials]
    tmp = bpy.data.meshes.new("_tmp")
    bm.to_mesh(tmp)
    bm.free()
    for i, n in enumerate(nomes):
        if n not in materiais_usados:
            materiais_usados.append(n)
    remapa = [materiais_usados.index(n) for n in nomes]
    for p in tmp.polygons:
        p.material_index = remapa[p.material_index] if remapa else 0
    bm_destino.from_mesh(tmp)
    bpy.data.meshes.remove(tmp)
    ev.to_mesh_clear()


def escrever_obj(pasta, nome, bm, materiais, tabela, texturas, cabecalho, emissivo=False, cores=None):
    """OBJ com normais lisas por canto (as quinas vivas continuam vivas) e .mtl com a cor e a textura, em
    models/item/<pasta>/. `tabela` diz a textura e a cor de cada material; `texturas` vai no JSON do modelo."""
    modelos = ASSETS / "models" / "item" / pasta
    me = bpy.data.meshes.new(nome)
    reduzir(bm)
    bmesh.ops.triangulate(bm, faces=[f for f in bm.faces if len(f.verts) > 4])
    bm.to_mesh(me)
    bm.free()
    for p in me.polygons:
        p.use_smooth = True
    bm2 = bmesh.new()
    bm2.from_mesh(me)
    for e in bm2.edges:
        e.smooth = not (len(e.link_faces) == 2 and e.calc_face_angle(0.0) > math.radians(36))
    bm2.to_mesh(me)
    bm2.free()
    uv = me.uv_layers.active.data if me.uv_layers.active else None
    normais = me.corner_normals
    linhas = [cabecalho.format(nome=nome), f"mtllib {nome}.mtl"]
    for v in me.vertices:
        linhas.append("v %.6f %.6f %.6f" % tuple(v.co))
    # No atlas do Minecraft a textura não repete: coordenada fora de 0..1 pega a textura vizinha, e a do canto
    # exato mistura com a borda transparente no mipmap (era o pontilhado no cano). Cor lisa vai no centro do
    # sprite branco; textura que passa da borda é encolhida para caber, com folga, por material.
    def textura_de(mi):
        return tabela.get(materiais[mi], ("branco", None))[0] if mi < len(materiais) else "branco"

    faixa = {}
    if uv:
        for p in me.polygons:
            if textura_de(p.material_index) == "branco":
                continue
            for li in p.loop_indices:
                u, v = uv[li].uv
                f = faixa.setdefault(p.material_index, [u, u, v, v])
                f[0], f[1], f[2], f[3] = min(f[0], u), max(f[1], u), min(f[2], v), max(f[3], v)

    def coordenada(mi, li):
        if not uv or textura_de(mi) == "branco":
            return (0.5, 0.5)
        u, v = uv[li].uv
        u0, u1, v0, v1 = faixa[mi]
        if u0 < 0.01 or u1 > 0.99:
            u = 0.01 + 0.98 * (u - u0) / max(u1 - u0, 1e-9)
        if v0 < 0.01 or v1 > 0.99:
            v = 0.01 + 0.98 * (v - v0) / max(v1 - v0, 1e-9)
        return (round(u, 6), round(v, 6))

    idx_vt, idx_vn = {}, {}
    vts, vns = [], []
    faces = []
    for p in me.polygons:
        canto = []
        for li in p.loop_indices:
            vi = me.loops[li].vertex_index
            t = coordenada(p.material_index, li)
            n = tuple(round(c, 5) for c in normais[li].vector)
            if t not in idx_vt:
                idx_vt[t] = len(vts) + 1
                vts.append(t)
            if n not in idx_vn:
                idx_vn[n] = len(vns) + 1
                vns.append(n)
            canto.append(f"{vi + 1}/{idx_vt[t]}/{idx_vn[n]}")
        faces.append((p.material_index, " ".join(canto)))
    linhas += ["vt %.6f %.6f" % t for t in vts]
    linhas += ["vn %.5f %.5f %.5f" % n for n in vns]
    for mi in sorted({f[0] for f in faces}):
        linhas.append(f"usemtl {materiais[mi]}")
        linhas += [f"f {c}" for m, c in faces if m == mi]
    (modelos / f"{nome}.obj").write_text("\n".join(linhas) + "\n", encoding="utf-8")
    mtl = []
    for m in materiais:
        textura, cor = tabela.get(m, ("branco", (0.7, 0.7, 0.7)))
        if cores and m in cores:
            cor = cores[m]
        mtl += [f"newmtl {m}", "Kd %.4f %.4f %.4f 1.0" % cor,
                "Ka 1.0 1.0 1.0" if (emissivo and m == "clarao") else "Ka 0.0 0.0 0.0",
                f"map_Kd #{textura}", ""]
    (modelos / f"{nome}.mtl").write_text("\n".join(mtl), encoding="utf-8")
    modelo = {
        "loader": "neoforge:obj",
        "model": f"revolver:models/item/{pasta}/{nome}.obj",
        "flip_v": True,
        "shade_quads": not emissivo,
        "automatic_culling": False,
        "emissive_ambient": emissivo,
        "textures": texturas,
    }
    (modelos / f"{nome}.json").write_text(json.dumps(modelo, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    tris = sum(len(p.vertices) - 2 for p in me.polygons)
    bpy.data.meshes.remove(me)
    return tris


def m34(M):
    return [round(M[i][j], 6) for i in range(3) for j in range(4)]


def rotacao_xyz(graus):
    rx, ry, rz = (math.radians(g) for g in graus)
    return (Matrix.Rotation(rx, 3, 'X') @ Matrix.Rotation(ry, 3, 'Y') @ Matrix.Rotation(rz, 3, 'Z'))


def exibicao(A, palma_blend, tabela):
    """Translação de cada contexto para a pegada cair na mão (ou o centro no meio). `tabela` tem, por contexto, o
    giro (graus, XYZ como no JSON do Minecraft), a escala e onde o centro da pegada deve cair (em blocos)."""
    palma = (A @ palma_blend.to_4d()).to_3d()
    saida = {}
    for ctx, cfg in tabela.items():
        R = rotacao_xyz(cfg["giro"])
        s = cfg["escala"]
        if cfg.get("centro"):
            t = Vector(cfg.get("desloc", (0, 0, 0)))
        else:
            t = Vector(cfg["palma"]) - R @ (s * (palma - Vector((0.5, 0.5, 0.5))))
        saida[ctx] = {"rotation": list(cfg["giro"]), "translation": [round(16 * c, 3) for c in t],
                      "scale": [s, s, s]}
    return saida


def icone_cartucho(nome, ponta_escura, ponta_clara, a=(3.2, 12.8), b=(12.8, 3.2), aro=2.1, corpo=1.6,
                   inicio_ponta=0.64, curva_ponta=0.86, corpo_cores=None, base_ate=None):
    """Ícone 16x16 do cartucho, deitado na diagonal com a ponta para cima e à direita (como as ferramentas do
    jogo): aro escuro embaixo, estojo de latão com um lado claro e outro escuro, ponta arredondada na cor do tipo e
    contorno escuro. Cada pixel é decidido pela distância ao eixo, então o corpo sai cheio. `a` e `b` são a base e a
    ponta (y cresce para baixo); `aro` e `corpo`, as meias larguras. `corpo_cores` (normal, escura, clara) pinta o
    corpo a partir de `base_ate` (o cartucho de escopeta: base de latão e tubo de plástico)."""
    n = 16
    px = [[(0, 0, 0, 0) for _ in range(n)] for _ in range(n)]
    latao, latao_esc, latao_cla = (0.86, 0.66, 0.30, 1), (0.56, 0.39, 0.15, 1), (0.98, 0.86, 0.55, 1)
    cor_aro = (0.66, 0.47, 0.18, 1)
    ponta, ponta_cla = (*ponta_escura, 1), (*ponta_clara, 1)
    contorno = (0.16, 0.11, 0.06, 1)
    ex, ey = b[0] - a[0], b[1] - a[1]
    comp = (ex * ex + ey * ey) ** 0.5
    ux, uy = ex / comp, ey / comp
    for y in range(n):
        for x in range(n):
            cx, cy = x + 0.5 - a[0], y + 0.5 - a[1]
            t = (cx * ux + cy * uy) / comp           # 0 no aro, 1 na ponta
            d = cx * uy - cy * ux                    # distância ao eixo (o sinal diz o lado)
            if t < 0 or t > 1:
                continue
            largura = aro if t < 0.1 else corpo
            if t > curva_ponta:                      # ponta arredondada
                largura *= max(0.0, 1 - ((t - curva_ponta) / (1 - curva_ponta)) ** 2) ** 0.5
            if abs(d) > largura:
                continue
            if t < 0.1:
                cor = cor_aro
            elif t < inicio_ponta:
                if corpo_cores and t >= base_ate:
                    c0, c1, c2 = ((*c, 1) for c in corpo_cores)
                    cor = c2 if d < -0.55 else (c1 if d > 0.55 else c0)
                else:
                    cor = latao_cla if d < -0.55 else (latao_esc if d > 0.55 else latao)
            else:
                cor = ponta_cla if d < -0.3 else ponta
            px[y][x] = cor
    for y in range(n):                                      # contorno escuro em volta do que tem cor
        for x in range(n):
            if px[y][x][3] == 0 and any(0 <= x + dx < n and 0 <= y + dy < n and px[y + dy][x + dx][3] == 1
                                        and px[y + dy][x + dx] != contorno
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[y][x] = contorno
    img = bpy.data.images.new(nome, n, n, alpha=True)
    plano = []
    for y in range(n - 1, -1, -1):  # imagem do Blender começa embaixo
        for x in range(n):
            plano += list(px[y][x])
    img.pixels = plano
    img.filepath_raw = str(TEXTURAS / f"{nome}.png")
    img.file_format = 'PNG'
    img.save()


def textura_branca(pasta):
    img = bpy.data.images.new("branco", 16, 16)
    img.pixels = [1.0] * (16 * 16 * 4)
    img.filepath_raw = str(TEXTURAS / pasta / "branco.png")
    img.file_format = 'PNG'
    img.save()
