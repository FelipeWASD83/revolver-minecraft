package dev.revolver.logica;

/**
 * Os tipos de cartucho .45, do mais básico ao melhor. O número é o que fica guardado em cada câmara (2 bits), então
 * a ordem não muda: tipo novo entra no fim. Revólver salvo antes dos tipos existirem lê tudo como {@link #COMUM}.
 */
public enum TipoBala {
    COMUM("cartucho_45", "bala_comum", 20f),
    ACO("cartucho_45_aco", "bala_aco", 50f),
    DIAMANTE("cartucho_45_diamante", "bala_diamante", 100f),
    NETHERITE("cartucho_45_netherite", "bala_netherite", 200f);

    /** Nome do item do cartucho. */
    public final String item;
    /** Nome do modelo da bala (a ponta, na cor do tipo). */
    public final String modelo;
    public final float dano;

    TipoBala(String item, String modelo, float dano) {
        this.item = item;
        this.modelo = modelo;
        this.dano = dano;
    }

    public static TipoBala de(int numero) {
        TipoBala[] todos = values();
        return numero >= 0 && numero < todos.length ? todos[numero] : COMUM;
    }
}
