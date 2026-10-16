package dev.revolver;

import dev.revolver.logica.Tambor;
import dev.revolver.logica.TipoBala;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TiposDeBalaTest {
    static final int C = TipoBala.COMUM.ordinal(), A = TipoBala.ACO.ordinal(), D = TipoBala.DIAMANTE.ordinal(),
            N = TipoBala.NETHERITE.ordinal();

    @Test
    void danos() {
        assertEquals(20f, TipoBala.COMUM.dano);
        assertEquals(200f, TipoBala.NETHERITE.dano);
        assertEquals(TipoBala.values().length, 4);
        // a ordem não pode mudar: é o número guardado em cada câmara
        assertEquals(0, C);
        assertEquals(3, N);
    }

    @Test
    void numeroDesconhecidoViraComum() {
        // revólver salvo na 1.0.x não tem "tipos": lê 0, que é o comum
        assertEquals(TipoBala.COMUM, TipoBala.de(0));
        assertEquals(TipoBala.COMUM, TipoBala.de(99));
    }

    @Test
    void filaMisturadaEntraNaOrdemDaPortinhola() {
        // tambor vazio na posição 0: os blocos passam pelas câmaras 1, 0, 5, 4, 3, 2
        Tambor.Plano p = Tambor.planejar(0, 0, new int[]{N, N, D, A, C, C});
        assertEquals(0b111111, p.carregar());
        assertEquals(N, Tambor.tipo(p.tipos(), 1));
        assertEquals(N, Tambor.tipo(p.tipos(), 0));
        assertEquals(D, Tambor.tipo(p.tipos(), 5));
        assertEquals(A, Tambor.tipo(p.tipos(), 4));
        assertEquals(C, Tambor.tipo(p.tipos(), 3));
        assertEquals(C, Tambor.tipo(p.tipos(), 2));
    }

    @Test
    void camaraJaCarregadaGuardaOTipoDela() {
        int camaras = Tambor.com(0, 2, Tambor.CARREGADA);
        int tipos = Tambor.comTipo(0, 2, D);
        Tambor.Plano p = Tambor.planejar(camaras, 0, new int[]{N, N, N, N, N, N});
        assertEquals(5, Integer.bitCount(p.carregar()));
        int depois = Tambor.tiposDepoisDaRecarga(camaras, tipos, p.carregar(), p.tipos());
        assertEquals(D, Tambor.tipo(depois, 2));
        for (int c : new int[]{0, 1, 3, 4, 5}) assertEquals(N, Tambor.tipo(depois, c));
    }

    @Test
    void cadaTiroUsaOTipoDaCamara() {
        Tambor.Plano p = Tambor.planejar(0, 0, new int[]{C, A, D, N, C, A});
        int camaras = Tambor.depoisDaRecarga(0, p.carregar());
        int tipos = Tambor.tiposDepoisDaRecarga(0, 0, p.carregar(), p.tipos());
        float total = 0;
        for (int posicao = 0; posicao < 6; posicao++) {
            int c = Tambor.camaraDoDisparo(posicao);
            assertEquals(Tambor.CARREGADA, Tambor.estado(camaras, c));
            total += TipoBala.de(Tambor.tipo(tipos, c)).dano;
            camaras = Tambor.com(camaras, c, Tambor.DEFLAGRADA);
        }
        assertEquals(20 + 50 + 100 + 200 + 20 + 50, total);
    }

    @Test
    void filaCurtaCarregaSoOQueTem() {
        Tambor.Plano p = Tambor.planejar(0, 0, new int[]{N});
        assertEquals(1 << 1, p.carregar());
        assertEquals(N, Tambor.tipo(p.tipos(), 1));
    }
}
