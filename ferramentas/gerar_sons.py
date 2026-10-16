"""Gera os sons do revólver por síntese (sem gravação nem FL Studio) e grava em OGG Vorbis, que é o que o
Minecraft aceita.

    .venv\\Scripts\\python.exe ferramentas\\gerar_sons.py

Precisa do .venv do projeto (numpy 2.5.2 e av 17.0.1, versões que passam pelo Smart App Control nesta
máquina). Os sons são mono: no Minecraft só som mono diminui com a distância.
"""

import json
from pathlib import Path

import av
import numpy as np

PASTA = Path(__file__).resolve().parent.parent
SONS = PASTA / "src" / "main" / "resources" / "assets" / "revolver" / "sounds"
TAXA = 44100


def tempo(seg):
    return np.arange(int(TAXA * seg)) / TAXA


def passa_baixa(x, corte):
    a = np.exp(-2 * np.pi * corte / TAXA)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def passa_alta(x, corte):
    return x - passa_baixa(x, corte)


def ping(t, freq, decai, fase=0.0):
    """Ressonância metálica curta."""
    return np.sin(2 * np.pi * freq * t + fase) * np.exp(-t / decai)


def clique(rng, freq=3200, decai=0.012, ruido=0.6, dur=0.08):
    t = tempo(dur)
    r = passa_alta(rng.standard_normal(t.size), 1800) * np.exp(-t / 0.004) * ruido
    return r + ping(t, freq, decai) + 0.5 * ping(t, freq * 1.53, decai * 0.7) + 0.3 * ping(t, freq * 2.37, decai * 0.5)


def colar(base, trecho, inicio, ganho=1.0):
    i = max(0, int(inicio * TAXA))
    fim = min(base.size, i + trecho.size)
    base[i:fim] += trecho[:fim - i] * ganho
    return base


def tiro(semente):
    rng = np.random.default_rng(semente)
    t = tempo(1.4)
    estalo = passa_alta(rng.standard_normal(t.size), 600) * np.exp(-t / 0.018)
    corpo = passa_baixa(rng.standard_normal(t.size), 2500) * np.exp(-t / 0.07) * 1.6
    freq = 95 * np.exp(-t / 0.25) + 42
    estrondo = np.sin(2 * np.pi * np.cumsum(freq) / TAXA) * np.exp(-t / 0.22) * 1.3
    cauda = passa_baixa(rng.standard_normal(t.size), 900) * np.exp(-t / 0.45) * 0.35
    ataque = np.minimum(1.0, t / 0.0008)
    som = (estalo + corpo + estrondo + cauda) * ataque
    return np.tanh(som * 1.4)


def engatilhar(semente):
    rng = np.random.default_rng(semente)
    som = np.zeros(int(TAXA * 0.35))
    colar(som, clique(rng, 2600, 0.014), 0.0, 0.7)
    colar(som, clique(rng, 3400, 0.010), 0.13, 1.0)     # o tambor trava
    return som


def clique_seco(semente):
    rng = np.random.default_rng(semente)
    som = np.zeros(int(TAXA * 0.25))
    colar(som, clique(rng, 2300, 0.02, ruido=0.9), 0.0, 1.0)
    return som


def portinhola(semente):
    rng = np.random.default_rng(semente)
    som = np.zeros(int(TAXA * 0.3))
    colar(som, clique(rng, 1700, 0.025, ruido=0.5, dur=0.15), 0.0, 0.8)
    t = tempo(0.08)
    raspa = passa_alta(rng.standard_normal(t.size), 3000) * np.exp(-t / 0.03) * 0.15
    colar(som, raspa, 0.02)
    return som


def estojo_cai(semente):
    rng = np.random.default_rng(semente)
    som = np.zeros(int(TAXA * 0.9))
    quiques = [(0.0, 1.0), (0.21, 0.55), (0.34, 0.3), (0.42, 0.15)]
    desvio = rng.uniform(0.95, 1.05)
    for inicio, ganho in quiques:
        t = tempo(0.25)
        tink = (ping(t, 4600 * desvio, 0.05) + 0.7 * ping(t, 6900 * desvio, 0.035)
                + 0.4 * ping(t, 9300 * desvio, 0.02) + 0.3 * ping(t, 2400 * desvio, 0.06))
        colar(som, tink, inicio + rng.uniform(-0.01, 0.01), ganho * 0.6)
    return som


def bala_entra(semente):
    rng = np.random.default_rng(semente)
    som = np.zeros(int(TAXA * 0.35))
    t = tempo(0.16)
    desliza = passa_baixa(passa_alta(rng.standard_normal(t.size), 1200), 5000) * np.sin(np.pi * t / t[-1]) * 0.25
    colar(som, desliza, 0.0)
    colar(som, clique(rng, 2000, 0.02, ruido=0.4), 0.15, 0.7)
    return som


def tambor_gira(semente):
    rng = np.random.default_rng(semente)
    som = np.zeros(int(TAXA * 0.2))
    colar(som, clique(rng, 3800, 0.008, ruido=0.5, dur=0.06), 0.0, 0.45)
    return som


def gravar_ogg(nome, sinal):
    sinal = sinal / max(1e-9, np.abs(sinal).max()) * 0.92
    caminho = SONS / f"{nome}.ogg"
    sai = av.open(str(caminho), "w", format="ogg")
    fluxo = sai.add_stream("libvorbis", rate=TAXA, layout="mono")
    reamostra = av.AudioResampler(format="fltp", layout="mono", rate=TAXA)
    quadro = av.AudioFrame.from_ndarray(np.int16(sinal * 32767)[None, :], format="s16", layout="mono")
    quadro.sample_rate = TAXA
    for q in reamostra.resample(quadro) + reamostra.resample(None):
        for pac in fluxo.encode(q):
            sai.mux(pac)
    for pac in fluxo.encode(None):
        sai.mux(pac)
    sai.close()
    volta = av.open(str(caminho))
    dur = sum(q.samples for q in volta.decode(audio=0)) / TAXA
    volta.close()
    return dur


def principal():
    SONS.mkdir(parents=True, exist_ok=True)
    for velho in SONS.glob("*.ogg"):
        velho.unlink()
    eventos = {
        "tiro": (tiro, 3, "Revólver dispara"),
        "engatilhar": (engatilhar, 1, "Cão armado"),
        "clique_seco": (clique_seco, 1, "Revólver dá clique seco"),
        "portinhola": (portinhola, 1, "Portinhola do revólver"),
        "estojo_cai": (estojo_cai, 3, "Estojo cai"),
        "bala_entra": (bala_entra, 1, "Bala entra no tambor"),
        "tambor_gira": (tambor_gira, 1, "Tambor gira"),
    }
    sons_json = {}
    for evento, (funcao, variacoes, legenda) in eventos.items():
        arquivos = []
        for v in range(variacoes):
            nome = evento if variacoes == 1 else f"{evento}{v + 1}"
            dur = gravar_ogg(nome, funcao(1000 * v + len(evento)))
            print(f"{nome}.ogg  {dur:.2f} s")
            arquivos.append(f"revolver:{nome}")
        sons_json[evento] = {"subtitle": f"subtitles.revolver.{evento}", "sounds": arquivos}
    (SONS.parent / "sounds.json").write_text(json.dumps(sons_json, indent=2, ensure_ascii=False) + "\n",
                                            encoding="utf-8")
    legendas = {f"subtitles.revolver.{e}": l for e, (_, _, l) in eventos.items()}
    (PASTA / "ferramentas" / "legendas_sons.json").write_text(json.dumps(legendas, indent=2, ensure_ascii=False)
                                                              + "\n", encoding="utf-8")


principal()
