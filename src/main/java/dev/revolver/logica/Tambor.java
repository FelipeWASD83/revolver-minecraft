package dev.revolver.logica;

/**
 * O tambor de seis câmaras. Câmara = o furo físico (0 a 5, guardado no item); encaixe = o lugar onde ela está
 * na animação (o encaixe 0 é o de cima, embaixo do cão; o 1 é o da portinhola). Com o tambor na posição p,
 * a câmara c está no encaixe (c + p) mod 6. O tiro sempre dispara o encaixe 5, que o cão traz para cima ao
 * armar; depois do tiro a posição anda um.
 *
 * <p>O estado das seis câmaras cabe num int: 2 bits por câmara (vazia, carregada, deflagrada).
 */
public final class Tambor {
    public static final int VAZIA = 0;
    public static final int CARREGADA = 1;
    public static final int DEFLAGRADA = 2;
    public static final int ENCAIXE_DO_DISPARO = 5;

    private Tambor() {}

    public static int estado(int camaras, int camara) {
        return (camaras >> (2 * camara)) & 3;
    }

    public static int com(int camaras, int camara, int estado) {
        int deslocamento = 2 * camara;
        return (camaras & ~(3 << deslocamento)) | ((estado & 3) << deslocamento);
    }

    public static int camaraDoEncaixe(int encaixe, int posicao) {
        return Math.floorMod(encaixe - posicao, 6);
    }

    public static int encaixeDaCamara(int camara, int posicao) {
        return Math.floorMod(camara + posicao, 6);
    }

    public static int camaraDoDisparo(int posicao) {
        return camaraDoEncaixe(ENCAIXE_DO_DISPARO, posicao);
    }

    /** O bloco i da recarga cuida do encaixe (1 - i) mod 6: começa na portinhola e o tambor gira a cada bloco. */
    public static int encaixeDoBloco(int bloco) {
        return Math.floorMod(1 - bloco, 6);
    }

    public static int blocoDoSlot(int encaixe) {
        return Math.floorMod(1 - encaixe, 6);
    }

    public static int carregadas(int camaras) {
        int n = 0;
        for (int c = 0; c < 6; c++) {
            if (estado(camaras, c) == CARREGADA) n++;
        }
        return n;
    }

    public static boolean temDeflagrada(int camaras) {
        for (int c = 0; c < 6; c++) {
            if (estado(camaras, c) == DEFLAGRADA) return true;
        }
        return false;
    }

    /**
     * Quais câmaras recebem bala nova nesta recarga (bit por câmara), na ordem em que a animação passa por elas,
     * até acabarem os cartuchos. Câmara que já tem bala não gasta cartucho.
     */
    public static int planejarRecarga(int camaras, int posicao, int cartuchos) {
        int carregar = 0;
        for (int bloco = 0; bloco < 6 && cartuchos > 0; bloco++) {
            int c = camaraDoEncaixe(encaixeDoBloco(bloco), posicao);
            if (estado(camaras, c) != CARREGADA) {
                carregar |= 1 << c;
                cartuchos--;
            }
        }
        return carregar;
    }

    /** Depois da recarga: o que já tinha bala continua; o planejado recebe bala; estojo vazio sai do tambor. */
    public static int depoisDaRecarga(int camaras, int carregar) {
        int novo = 0;
        for (int c = 0; c < 6; c++) {
            boolean fica = estado(camaras, c) == CARREGADA || (carregar & (1 << c)) != 0;
            novo = com(novo, c, fica ? CARREGADA : VAZIA);
        }
        return novo;
    }

    /** O que desenhar num encaixe durante a recarga, no quadro (1 = começo) dado. */
    public enum Visual { NADA, ESTOJO, ESTOJO_E_BALA_JUNTOS, ESTOJO_E_BALA }

    public static Visual visualNaRecarga(int camaras, int carregar, int posicao, int encaixe, float quadro) {
        int c = camaraDoEncaixe(encaixe, posicao);
        int antes = estado(camaras, c);
        boolean depois = antes == CARREGADA || (carregar & (1 << c)) != 0;
        int inicio = Tempos.inicioDoBloco(encaixe);
        if (quadro < inicio + Tempos.BLOCO_ESTOJO_SOME) {
            // ainda no tambor (ou saindo): o estojo que estava lá, com a bala se ela não tinha sido disparada
            if (antes == VAZIA) return Visual.NADA;
            return antes == CARREGADA ? Visual.ESTOJO_E_BALA_JUNTOS : Visual.ESTOJO;
        }
        if (quadro < inicio + Tempos.BLOCO_BALA_APARECE) return Visual.NADA;
        return depois ? Visual.ESTOJO_E_BALA : Visual.NADA;
    }

    /** O que desenhar num encaixe parado ou no tiro. */
    public static Visual visualParado(int camaras, int posicao, int encaixe) {
        int e = estado(camaras, camaraDoEncaixe(encaixe, posicao));
        return e == VAZIA ? Visual.NADA : e == CARREGADA ? Visual.ESTOJO_E_BALA : Visual.ESTOJO;
    }
}
