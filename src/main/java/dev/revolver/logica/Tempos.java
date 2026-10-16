package dev.revolver.logica;

/**
 * Os quadros das animações do Blender (30 por segundo) e os ticks do jogo (20 por segundo). Os números de
 * quadro são os do arquivo {@code animacoes/revolver.json} (quadro 1 = começo); o teste confere que batem.
 */
public final class Tempos {
    public static final double QUADROS_POR_TICK = 30.0 / 20.0;

    // tiro (50 quadros)
    public static final int TIRO_QUADROS = 50;
    public static final int TIRO_ENGATILHA = 11;
    public static final int TIRO_DISPARO = 22;

    // recarregar (340 quadros): portinhola, seis blocos de 46 quadros (um por câmara), fecha e desarma
    public static final int REC_QUADROS = 340;
    public static final int REC_MEIO_ARMADO = 10;
    public static final int REC_ABRE = 16;
    public static final int REC_T0 = 30;
    public static final int REC_BLOCO = 46;
    public static final int REC_FECHA = REC_T0 + 6 * REC_BLOCO + 6;
    public static final int REC_ENGATILHA = REC_T0 + 6 * REC_BLOCO + 12;
    public static final int REC_DESARMA = REC_T0 + 6 * REC_BLOCO + 28;

    // dentro de cada bloco, a partir do início dele
    public static final int BLOCO_EMPURRA = 4;
    public static final int BLOCO_ESTOJO_SOME = 21;
    public static final int BLOCO_BALA_APARECE = 22;
    public static final int BLOCO_BALA_ENTRA = 36;
    public static final int BLOCO_GIRA = 41;

    private Tempos() {}

    /** Tick (contado do começo da ação) em que cai o quadro dado. */
    public static int tick(int quadro) {
        return (int) Math.ceil((quadro - 1) / QUADROS_POR_TICK);
    }

    /** Índice de quadro (0 = quadro 1) para um tempo em ticks, com fração. */
    public static float indice(double ticks) {
        return (float) (ticks * QUADROS_POR_TICK);
    }

    /** Quadro em que começa o bloco da recarga que cuida do encaixe {@code slot}. */
    public static int inicioDoBloco(int slot) {
        return REC_T0 + Tambor.blocoDoSlot(slot) * REC_BLOCO;
    }

    public static int tickFimTiro() {
        return tick(TIRO_QUADROS);
    }

    public static int tickFimRecarga() {
        return tick(REC_QUADROS);
    }
}
