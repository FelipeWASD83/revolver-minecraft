package dev.revolver.logica;

/**
 * Os calibres do mod. Cada um tem os mesmos quatro tipos de bala ({@link TipoBala}); o dano do tipo é o do .45 (20 no
 * comum) vezes o multiplicador do calibre: o .50 AE da Desert Eagle dá 1,5 vez (pedido do Felipe), e os das armas
 * novas seguem a proposta que ele aprovou (metralhadora 10, AK 12, 5,56 10, sniper 50, e a 12 com 8 bagos de 4).
 */
public enum Calibre {
    C45("cartucho_45", 1.0f, 1),
    C50("cartucho_50", 1.5f, 1),
    C762X51("cartucho_762x51", 0.5f, 1),
    C12("cartucho_12", 0.2f, 8),
    C762X39("cartucho_762x39", 0.6f, 1),
    C556("cartucho_556", 0.5f, 1),
    C338("cartucho_338", 2.5f, 1);

    /** Nome do item do cartucho comum; os outros tipos acrescentam o sufixo do tipo. */
    public final String base;
    public final float multiplicador;
    /** Quantas balas saem por tiro (o cartucho de escopeta solta vários bagos). */
    public final int bagos;

    Calibre(String base, float multiplicador, int bagos) {
        this.base = base;
        this.multiplicador = multiplicador;
        this.bagos = bagos;
    }
}
