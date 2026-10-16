package dev.revolver.logica;

/**
 * O pente de 7 e a câmara da Desert Eagle, sem nada do Minecraft (dá para testar). O tipo de cada bala do pente fica
 * em 2 bits, a de cima (a próxima a subir para a câmara) no lugar 0.
 *
 * <p>Semiautomática: o tiro gasta a bala da câmara e o ferrolho, voltando, põe nela a de cima do pente. Sem bala no
 * pente, o ferrolho fica aberto e a câmara vazia. Na recarga o pente novo leva as balas que sobraram no velho (por
 * cima, saem primeiro) e as novas embaixo, até 7; se a câmara estava vazia, soltar o ferrolho põe nela a de cima.
 */
public final class Pente {
    public static final int CAPACIDADE = 7;

    private Pente() {}

    /** Como ficam pente e câmara. */
    public record Carga(int pente, int tipos, int camara, int tipoCamara) {}

    /** O pente novo da recarga e quantos cartuchos ela gasta. */
    public record Plano(int pente, int tipos, int usados) {}

    public static int tipo(int tipos, int lugar) {
        return (tipos >> (2 * lugar)) & 3;
    }

    public static int comTipo(int tipos, int lugar, int tipo) {
        return (tipos & ~(3 << (2 * lugar))) | ((tipo & 3) << (2 * lugar));
    }

    /** Depois do tiro (ou de soltar o ferrolho com a câmara vazia): a de cima do pente sobe para a câmara. */
    public static Carga subir(int pente, int tipos) {
        if (pente <= 0) return new Carga(0, 0, 0, 0);
        return new Carga(pente - 1, tipos >>> 2, 1, tipo(tipos, 0));
    }

    /** A recarga com a fila de cartuchos (tipos, na ordem em que saem do inventário). */
    public static Plano planejar(int pente, int tipos, int[] fila) {
        int usados = Math.min(CAPACIDADE - pente, fila.length);
        int novos = tipos & ((1 << (2 * pente)) - 1);
        for (int i = 0; i < usados; i++) novos = comTipo(novos, pente + i, fila[i]);
        return new Plano(pente + usados, novos, usados);
    }

    /** Quantas balas de cada tipo há no pente e na câmara juntos. */
    public static int contar(int pente, int tipos, int camara, int tipoCamara, int tipo) {
        int n = camara == 1 && tipoCamara == tipo ? 1 : 0;
        for (int i = 0; i < pente; i++) if (tipo(tipos, i) == tipo) n++;
        return n;
    }
}
