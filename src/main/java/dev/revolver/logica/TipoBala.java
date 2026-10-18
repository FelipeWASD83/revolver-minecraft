package dev.revolver.logica;

/**
 * Os tipos de cartucho, do mais básico ao melhor, iguais em todos os calibres. O número é o que fica guardado em cada
 * câmara do revólver e em cada lugar do pente da deagle (4 bits desde a 2.3.0; eram 2), então a ordem não muda: tipo
 * novo entra no fim, e cabem 16. Arma salva antes dos tipos existirem lê tudo como {@link #COMUM}.
 *
 * <p>Os sete depois do netherite (2.3.0, pedido dele) são feitos com material de outro mod: sem o mod, a receita some
 * e o cartucho sai da aba do criativo, mas o item continua registrado (arma carregada com ele não perde a bala).
 */
public enum TipoBala {
    COMUM("", "bala_comum", 20f, null, null),
    ACO("_aco", "bala_aco", 50f, null, null),
    DIAMANTE("_diamante", "bala_diamante", 100f, null, null),
    NETHERITE("_netherite", "bala_netherite", 200f, null, null),
    ARDENTE("_ardente", "bala_ardente", 250f, "twilightforest", "twilightforest:fiery_ingot"),
    INSANIUM("_insanium", "bala_insanium", 300f, "mysticalagradditions", "mysticalagradditions:insanium_ingot"),
    IGNITIUM("_ignitium", "bala_ignitium", 350f, "cataclysm", "cataclysm:ignitium_ingot"),
    DRACONICO("_draconico", "bala_draconico", 500f, "draconicevolution",
            "draconicevolution:awakened_draconium_ingot"),
    MATERIA_VERMELHA("_materia_vermelha", "bala_materia_vermelha", 600f, "projecte", "projecte:red_matter"),
    ANTIMATERIA("_antimateria", "bala_antimateria", 800f, "mekanism", "mekanism:pellet_antimatter"),
    /** Do Avaritia: dano infinito, e atirar não gasta a bala. */
    INFINITO("_infinito", "bala_infinito", Float.POSITIVE_INFINITY, "avaritia", "avaritia:infinity_ingot");

    /** Sufixo do nome do item (o comum não tem: "cartucho_45", "cartucho_50"). */
    public final String sufixo;
    /** Nome do item do cartucho .45. */
    public final String item;
    /** Nome do modelo da bala (a ponta, na cor do tipo), em cada arma. */
    public final String modelo;
    /** Dano no .45; os outros calibres multiplicam pelo {@link Calibre#multiplicador}. */
    public final float dano;
    /** O mod de onde vem o material (null nos do jogo puro). */
    public final String mod;
    /** O item do outro mod que vai no meio da receita. */
    public final String material;

    TipoBala(String sufixo, String modelo, float dano, String mod, String material) {
        this.sufixo = sufixo;
        this.item = Calibre.C45.base + sufixo;
        this.modelo = modelo;
        this.dano = dano;
        this.mod = mod;
        this.material = material;
    }

    public String item(Calibre calibre) {
        return calibre.base + sufixo;
    }

    public float dano(Calibre calibre) {
        return dano * calibre.multiplicador;
    }

    /** A bala que nunca acaba e mata o que acertar. */
    public boolean infinito() {
        return this == INFINITO;
    }

    /** Segundos de fogo no alvo (0 = não põe fogo). */
    public float fogo() {
        return this == ARDENTE ? 5f : this == IGNITIUM ? 8f : 0f;
    }

    /** Explode onde acerta, sem quebrar bloco. */
    public boolean explode() {
        return this == ANTIMATERIA;
    }

    /** O melhor tipo da lista (o de número maior), ou {@link #COMUM} se a lista estiver vazia. */
    public static int melhor(Iterable<Integer> tipos) {
        int m = -1;
        for (int t : tipos) m = Math.max(m, t);
        return m < 0 ? COMUM.ordinal() : m;
    }

    public static TipoBala de(int numero) {
        TipoBala[] todos = values();
        return numero >= 0 && numero < todos.length ? todos[numero] : COMUM;
    }
}
