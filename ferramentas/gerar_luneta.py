r"""Gera a imagem da luneta da sniper (assets/revolver/textures/gui/luneta.png): preto em volta, o círculo da lente
transparente com uma borda que escurece, e a cruz fina com os pontos de medida. Só biblioteca padrão e numpy.

    .venv\Scripts\python.exe ferramentas\gerar_luneta.py
"""

import struct
import zlib
from pathlib import Path

import numpy as np

SAIDA = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "revolver" / "textures" / "gui"
N = 512


def png(caminho, rgba):
    altura, largura, _ = rgba.shape
    linhas = b"".join(b"\x00" + rgba[y].tobytes() for y in range(altura))

    def bloco(tipo, dados):
        return struct.pack(">I", len(dados)) + tipo + dados + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF)

    caminho.write_bytes(b"\x89PNG\r\n\x1a\n" + bloco(b"IHDR", struct.pack(">IIBBBBB", largura, altura, 8, 6, 0, 0, 0))
                        + bloco(b"IDAT", zlib.compress(linhas, 9)) + bloco(b"IEND", b""))


def principal():
    y, x = np.mgrid[0:N, 0:N] + 0.5
    c = N / 2
    r = np.hypot(x - c, y - c) / c                 # 0 no centro, 1 na borda do quadrado
    alfa = np.clip((r - 0.86) / 0.1, 0, 1)          # fora da lente: preto; a borda escurece aos poucos
    cruz = ((np.abs(x - c) < 1.0) | (np.abs(y - c) < 1.0)) & (r < 0.95)
    grossa = (((np.abs(x - c) < 2.5) & (np.abs(y - c) > 0.45 * c)) |
              ((np.abs(y - c) < 2.5) & (np.abs(x - c) > 0.45 * c))) & (r < 0.95)
    pontos = np.zeros_like(cruz)
    for k in range(1, 5):                           # marcas de medida nos dois braços de baixo e da direita
        d = k * 0.09 * c
        pontos |= (np.hypot(x - c, y - (c + d)) < 2.6) | (np.hypot(x - (c + d), y - c) < 2.6)
    alfa = np.maximum(alfa, np.where(cruz | grossa | pontos, 0.92, 0))
    rgba = np.zeros((N, N, 4), dtype=np.uint8)
    rgba[..., 3] = np.round(alfa * 255).astype(np.uint8)
    SAIDA.mkdir(parents=True, exist_ok=True)
    png(SAIDA / "luneta.png", rgba)
    print("luneta.png", N, "x", N)


principal()
