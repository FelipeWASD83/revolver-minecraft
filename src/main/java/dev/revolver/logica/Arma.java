package dev.revolver.logica;

import java.util.List;

/**
 * As armas novas do mod (as que vieram depois do revólver e da deagle), uma linha de dados cada: calibre, capacidade,
 * como atira e recarrega, os quadros das animações do Blender e os sons. O {@code ArmaItem} e o {@code ArmaRenderer}
 * fazem o resto. Os quadros são os do arquivo {@code animacoes/<id>.json} (quadro 1 = começo) e o teste confere que
 * batem. A ordem não importa para o mundo salvo (o item é guardado pelo nome).
 */
public enum Arma {
    // os quadros e eventos são os do modelo/animacoes.json de cada projeto <id>-3d (o teste confere)
    AK47("ak47", Calibre.C762X39, 30, false, true, 4, 2, 75, 35,
            List.of("ferrolho", "gatilho", "botao_pente", "pente"), 0f, 1f, 1f, "tiro_ak47",
            List.of(new Som("recarregar", 18, "pente_sai"), new Som("recarregar", 54, "pente_entra"),
                    new Som("recarregar", 64, "ferrolho"))),
    M4A4("m4a4", Calibre.C556, 30, false, true, 4, 2, 70, 27,
            List.of("ferrolho", "alavanca", "gatilho", "pente", "botao_pente"), 0f, 1f, 1f, "tiro_m4a4",
            List.of(new Som("recarregar", 14, "pente_sai"), new Som("recarregar", 44, "pente_entra"),
                    new Som("recarregar", 55, "ferrolho"))),
    /** A M4A1-S usa o mesmo 5,56 da M4A4 (escolha dele) e bate 10% mais, como no CS2. */
    M4A1("m4a1", Calibre.C556, 20, false, true, 4, 2, 70, 31,
            List.of("ferrolho", "alavanca", "gatilho", "pente", "botao_pente"), 0f, 1.1f, 1f, "tiro_m4a1",
            List.of(new Som("recarregar", 14, "pente_sai"), new Som("recarregar", 48, "pente_entra"),
                    new Som("recarregar", 58, "ferrolho"))),
    METRALHADORA("metralhadora", Calibre.C762X51, 100, false, true, 3, 1, 110, 45,
            List.of("ferrolho", "gatilho", "tampa", "pente"), 0f, 1f, 1f, "tiro_metralhadora",
            List.of(new Som("recarregar", 13, "tampa"), new Som("recarregar", 25, "pente_sai"),
                    new Som("recarregar", 66, "pente_entra"), new Som("recarregar", 75, "tampa"),
                    new Som("recarregar", 88, "ferrolho"))),
    ESCOPETA("escopeta", Calibre.C12, 8, true, false, 22, 3, 14, 0,
            List.of("telha", "ferrolho", "gatilho"), 5f, 1f, 1f, "tiro_escopeta",
            List.of(new Som("tiro", 12, "bomba"), new Som("inserir", 11, "bala_entra"))),
    SNIPER("sniper", Calibre.C338, 5, false, false, 40, 3, 70, 26,
            List.of("ferrolho", "gatilho", "botao_pente", "pente"), 0f, 1f, 0.2f, "tiro_sniper",
            List.of(new Som("tiro", 14, "ferrolho_abre"), new Som("tiro", 31, "ferrolho_fecha"),
                    new Som("recarregar", 12, "pente_sai"), new Som("recarregar", 40, "pente_entra"),
                    new Som("recarregar", 44, "ferrolho_abre"), new Som("recarregar", 61, "ferrolho_fecha")));

    /** Um som tocado num quadro de uma animação ("tiro", "recarregar" ou "inserir"). */
    public record Som(String faixa, int quadro, String som) {}

    /** Nome do item e da pasta do modelo ({@code models/item/<id>/}). */
    public final String id;
    public final Calibre calibre;
    public final int capacidade;
    /** Escopeta: recarrega cartucho por cartucho (faixa "inserir") em vez de trocar o pente. */
    public final boolean tubo;
    /** Atira enquanto o botão estiver segurado, uma vez a cada faixa de tiro. */
    public final boolean automatica;
    public final int tiroQuadros;
    /** Quadro da faixa de tiro em que a bala sai. */
    public final int disparo;
    /** Comprimento da faixa "recarregar" (ou "inserir", na escopeta). */
    public final int recargaQuadros;
    /** Quadro em que o pente velho já sumiu e o novo ainda não apareceu (0 na escopeta). */
    public final int troca;
    /** Peças que mexem, além do corpo e das especiais (clarão, estojo, cartucho de cima). */
    public final List<String> pecas;
    /** Desvio de cada bala, em graus (0 = vai reto na mira). */
    public final float espalhamento;
    /** Multiplica o dano do calibre (a M4A1-S bate um pouco mais que a M4A4 com o mesmo 5,56). */
    public final float fatorDano;
    /** Campo de visão com a luneta, agachado (1 = sem luneta). */
    public final float zoom;
    public final String somTiro;
    public final List<Som> sons;

    Arma(String id, Calibre calibre, int capacidade, boolean tubo, boolean automatica, int tiroQuadros, int disparo,
         int recargaQuadros, int troca, List<String> pecas, float espalhamento, float fatorDano, float zoom,
         String somTiro, List<Som> sons) {
        this.id = id;
        this.calibre = calibre;
        this.capacidade = capacidade;
        this.tubo = tubo;
        this.automatica = automatica;
        this.tiroQuadros = tiroQuadros;
        this.disparo = disparo;
        this.recargaQuadros = recargaQuadros;
        this.troca = troca;
        this.pecas = pecas;
        this.espalhamento = espalhamento;
        this.fatorDano = fatorDano;
        this.zoom = zoom;
        this.somTiro = somTiro;
        this.sons = sons;
    }

    /** Quantas balas saem por tiro: as do calibre (o 12 solta 8 bagos). */
    public int bagos() {
        return calibre.bagos;
    }

    /** Dano de uma bala (ou de um bago) do tipo dado. */
    public float dano(TipoBala tipo) {
        return tipo.dano(calibre) * fatorDano;
    }

    /** Nome da faixa de recarga no arquivo de animação. */
    public String faixaRecarga() {
        return tubo ? "inserir" : "recarregar";
    }

    public static Arma porId(String id) {
        for (Arma a : values()) if (a.id.equals(id)) return a;
        return null;
    }
}
