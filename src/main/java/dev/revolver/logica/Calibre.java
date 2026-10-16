package dev.revolver.logica;

/**
 * Os dois calibres do mod. Cada um tem os mesmos quatro tipos de bala ({@link TipoBala}); o dano do tipo é o do .45 e
 * o .50 AE da Desert Eagle multiplica por 1,5 (pedido do Felipe: "1,5x mais dano que o revólver").
 */
public enum Calibre {
    C45("cartucho_45", 1.0f),
    C50("cartucho_50", 1.5f);

    /** Nome do item do cartucho comum; os outros tipos acrescentam o sufixo do tipo. */
    public final String base;
    public final float multiplicador;

    Calibre(String base, float multiplicador) {
        this.base = base;
        this.multiplicador = multiplicador;
    }
}
