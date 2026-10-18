"""Os cartuchos com material de outro mod e a Bandoleira do Fim (2.3.0): receitas, nomes nas duas línguas e o ícone
da bandoleira. Roda no Python comum (precisa do Pillow), sem Blender:

    python ferramentas\\gerar_tipos_mod.py

As receitas dos cartuchos levam a condição "neoforge:mod_loaded": sem o mod do material elas não são carregadas (e
o item do outro mod nem é procurado). A tabela tem de bater com o TipoBala.java (o RecursosTest confere). Os ícones e
os modelos dos cartuchos saem dos exportadores, junto com os outros tipos.
"""

import json
from pathlib import Path

from PIL import Image

PASTA = Path(__file__).resolve().parent.parent
ASSETS = PASTA / "src" / "main" / "resources" / "assets" / "revolver"
RECEITAS = PASTA / "src" / "main" / "resources" / "data" / "revolver" / "recipe"

# sufixo, mod, material, quantos a receita dá, nome em português (depois do calibre), em inglês (antes de "Cartridge")
TIPOS = [
    ("_ardente", "twilightforest", "twilightforest:fiery_ingot", 8, "ardente", "Fiery"),
    ("_insanium", "mysticalagradditions", "mysticalagradditions:insanium_ingot", 8, "de insanium", "Insanium"),
    ("_ignitium", "cataclysm", "cataclysm:ignitium_ingot", 8, "de ignitium", "Ignitium"),
    ("_draconico", "draconicevolution", "draconicevolution:awakened_draconium_ingot", 8, "dracônico", "Draconic"),
    ("_materia_vermelha", "projecte", "projecte:red_matter", 8, "de matéria vermelha", "Red Matter"),
    ("_antimateria", "mekanism", "mekanism:pellet_antimatter", 8, "de antimatéria", "Antimatter"),
    ("_infinito", "avaritia", "avaritia:infinity_ingot", 1, "infinito", "Infinity"),
]
CALIBRES = ["cartucho_45", "cartucho_50", "cartucho_762x51", "cartucho_12", "cartucho_762x39", "cartucho_556",
            "cartucho_338"]

TEXTOS = {
    "pt_br": {
        "item.revolver.bandoleira_do_fim": "Bandoleira do Fim",
        "item.revolver.bandoleira_do_fim.descricao": "No inventário: recarregar não gasta cartucho",
        "item.revolver.bandoleira_do_fim.tipo": "Enche a arma com o melhor cartucho que você tiver (ou o comum)",
        "item.revolver.cartucho.infinito": "infinito",
        "item.revolver.cartucho.nunca_acaba": "Nunca acaba: atirar não gasta a bala",
        "item.revolver.cartucho.fogo": "Põe fogo no alvo por %s s",
        "item.revolver.cartucho.explode": "Explode onde acerta, sem quebrar bloco",
    },
    "en_us": {
        "item.revolver.bandoleira_do_fim": "End Bandolier",
        "item.revolver.bandoleira_do_fim.descricao": "In your inventory: reloading uses no cartridges",
        "item.revolver.bandoleira_do_fim.tipo": "Fills the gun with the best cartridge you carry (or the plain one)",
        "item.revolver.cartucho.infinito": "infinite",
        "item.revolver.cartucho.nunca_acaba": "Never runs out: firing doesn't use it up",
        "item.revolver.cartucho.fogo": "Sets the target on fire for %s s",
        "item.revolver.cartucho.explode": "Explodes on impact without breaking blocks",
    },
}


def gravar_json(caminho, dados):
    caminho.write_text(json.dumps(dados, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def receitas():
    for base in CALIBRES:
        for sufixo, mod, material, quantos, _, _ in TIPOS:
            gravar_json(RECEITAS / f"{base}{sufixo}.json", {
                "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod}],
                "type": "minecraft:crafting_shaped",
                "category": "equipment",
                "pattern": ["CCC", "CMC", "CCC"],
                "key": {"C": {"item": f"revolver:{base}_netherite"}, "M": {"item": material}},
                "result": {"id": f"revolver:{base}{sufixo}", "count": quantos},
            })
    gravar_json(RECEITAS / "bandoleira_do_fim.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["STS", "BEB", "SLS"],
        "key": {"S": {"item": "minecraft:nether_star"}, "T": {"item": "minecraft:totem_of_undying"},
                "B": {"item": "minecraft:netherite_block"}, "E": {"item": "minecraft:dragon_egg"},
                "L": {"item": "minecraft:elytra"}},
        "result": {"id": "revolver:bandoleira_do_fim", "count": 1},
    })


def textos():
    for lingua, extras in TEXTOS.items():
        p = ASSETS / "lang" / f"{lingua}.json"
        dados = json.loads(p.read_text(encoding="utf-8"))
        for base in CALIBRES:
            nome_base = dados[f"item.revolver.{base}"]
            for sufixo, _, _, _, pt, en in TIPOS:
                if lingua == "pt_br":
                    nome = f"{nome_base} {pt}"
                else:                                   # ".45 Cartridge" -> ".45 Fiery Cartridge"
                    antes, _, ultima = nome_base.rpartition(" ")
                    nome = f"{antes} {en} {ultima}"
                dados[f"item.revolver.{base}{sufixo}"] = nome
        dados.update(extras)
        gravar_json(p, dados)


def icone_bandoleira():
    """16x16: a correia de couro na diagonal, cinco cartuchos presos nela e a fivela de pedra do Fim no meio."""
    n = 16
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    couro, couro_esc, couro_cla = (110, 62, 30, 255), (70, 38, 18, 255), (150, 92, 48, 255)
    latao, latao_esc, ponta = (220, 170, 77, 255), (143, 100, 38, 255), (205, 120, 230, 255)
    contorno = (34, 20, 12, 255)
    # correia: de (1,14) a (14,1), 3 px de largura
    for y in range(n):
        for x in range(n):
            d = (x + y) - 15            # distância à diagonal
            if abs(d) <= 1 and 1 <= x <= 14:
                px[x, y] = couro_cla if d == -1 else couro_esc if d == 1 else couro
    # cartuchos: saem da correia para cima e à esquerda (perpendiculares a ela)
    raiz2 = 2 ** 0.5
    for cx, cy in [(4.0, 10.0), (11.0, 3.0)] + [(2.0, 12.0), (13.0, 1.0)]:
        for y in range(n):
            for x in range(n):
                vx, vy = x + 0.5 - cx, y + 0.5 - cy
                t = -(vx + vy) / raiz2                  # ao longo do cartucho (para cima e à esquerda)
                d = (vx - vy) / raiz2                   # de lado
                if 0 <= t <= 3.6 and abs(d) <= 0.95:
                    px[x, y] = ponta if t > 2.5 else (latao if d < 0.2 else latao_esc)
    # fivela de pedra do Fim (roxa, com o olho no meio)
    for (x, y), cor in {(7, 7): (40, 160, 130, 255), (6, 7): (60, 40, 90, 255), (8, 7): (60, 40, 90, 255),
                        (7, 6): (60, 40, 90, 255), (7, 8): (60, 40, 90, 255), (6, 6): (90, 60, 130, 255),
                        (8, 8): (40, 25, 60, 255), (6, 8): (75, 50, 110, 255), (8, 6): (75, 50, 110, 255)}.items():
        px[x, y] = cor
    # contorno escuro em volta do que tem cor
    cheios = {(x, y) for y in range(n) for x in range(n) if px[x, y][3]}
    for y in range(n):
        for x in range(n):
            if (x, y) not in cheios and any((x + dx, y + dy) in cheios
                                            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = contorno
    img.save(ASSETS / "textures" / "item" / "bandoleira_do_fim.png")
    gravar_json(ASSETS / "models" / "item" / "bandoleira_do_fim.json",
                {"parent": "minecraft:item/generated", "textures": {"layer0": "revolver:item/bandoleira_do_fim"}})
    img.resize((n * 16, n * 16), Image.NEAREST).save(PASTA / "ferramentas" / "saida" / "icone-bandoleira.png")


if __name__ == "__main__":
    receitas()
    textos()
    icone_bandoleira()
    print(f"{len(CALIBRES) * len(TIPOS)} receitas de cartucho, a da bandoleira, os nomes e o ícone")
