package dev.revolver.logica;

/**
 * Os quadros das animações da Desert Eagle (30 por segundo), contados do começo de cada uma (quadro 1 = começo). São
 * os do arquivo {@code animacoes/deagle.json}; o teste confere que batem. A conversão para ticks é a do
 * {@link Tempos}.
 */
public final class TemposDeagle {
    public static final int TIRO_QUADROS = 14;
    public static final int TIRO_DISPARO = 4;
    public static final int TIRO_TRAVA = 7;          // só no último tiro: a trava sobe e segura o ferrolho aberto
    public static final int TIRO_ESTOJO_CAI = 14;

    public static final int SECO_QUADROS = 10;
    public static final int SECO_CLIQUE = 3;

    public static final int REC_QUADROS = 76;
    public static final int VAZIA_QUADROS = 86;
    public static final int REC_BOTAO = 15;
    public static final int REC_PENTE_SAI = 16;
    public static final int REC_TROCA = 34;          // o pente velho já sumiu e o novo ainda não apareceu
    public static final int REC_PENTE_ENTRA = 54;
    public static final int VAZIA_SOLTA_TRAVA = 62;
    public static final int VAZIA_FERROLHO = 64;

    private TemposDeagle() {}
}
