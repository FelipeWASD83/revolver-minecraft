package dev.revolver.logica;

/**
 * Os tipos de cartucho, do mais básico ao melhor, iguais nos dois calibres. O número é o que fica guardado em cada
 * câmara do revólver e em cada lugar do pente da deagle (2 bits), então a ordem não muda: tipo novo entra no fim.
 * Arma salva antes dos tipos existirem lê tudo como {@link #COMUM}.
 */
public enum TipoBala {
    COMUM("", "bala_comum", 20f),
    ACO("_aco", "bala_aco", 50f),
    DIAMANTE("_diamante", "bala_diamante", 100f),
    NETHERITE("_netherite", "bala_netherite", 200f);

    /** Sufixo do nome do item (o comum não tem: "cartucho_45", "cartucho_50"). */
    public final String sufixo;
    /** Nome do item do cartucho .45. */
    public final String item;
    /** Nome do modelo da bala (a ponta, na cor do tipo), em cada arma. */
    public final String modelo;
    /** Dano no .45; o .50 AE multiplica pelo {@link Calibre#multiplicador}. */
    public final float dano;

    TipoBala(String sufixo, String modelo, float dano) {
        this.sufixo = sufixo;
        this.item = Calibre.C45.base + sufixo;
        this.modelo = modelo;
        this.dano = dano;
    }

    public String item(Calibre calibre) {
        return calibre.base + sufixo;
    }

    public float dano(Calibre calibre) {
        return dano * calibre.multiplicador;
    }

    public static TipoBala de(int numero) {
        TipoBala[] todos = values();
        return numero >= 0 && numero < todos.length ? todos[numero] : COMUM;
    }
}
