package dev.revolver;

import dev.revolver.logica.Tambor;
import dev.revolver.logica.Tempos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TamborTest {
    private static int cheio() {
        int c = 0;
        for (int i = 0; i < 6; i++) c = Tambor.com(c, i, Tambor.CARREGADA);
        return c;
    }

    @Test
    void estadosCabemNoInt() {
        int c = Tambor.com(0, 3, Tambor.DEFLAGRADA);
        c = Tambor.com(c, 5, Tambor.CARREGADA);
        assertEquals(Tambor.DEFLAGRADA, Tambor.estado(c, 3));
        assertEquals(Tambor.CARREGADA, Tambor.estado(c, 5));
        assertEquals(Tambor.VAZIA, Tambor.estado(c, 0));
        assertEquals(1, Tambor.carregadas(c));
        assertTrue(Tambor.temDeflagrada(c));
    }

    @Test
    void seisTirosDisparamAsSeisCamarasUmaVezCada() {
        int camaras = cheio();
        int posicao = 0;
        boolean[] disparou = new boolean[6];
        for (int tiro = 0; tiro < 6; tiro++) {
            int c = Tambor.camaraDoDisparo(posicao);
            assertEquals(Tambor.CARREGADA, Tambor.estado(camaras, c), "tiro " + tiro);
            camaras = Tambor.com(camaras, c, Tambor.DEFLAGRADA);
            disparou[c] = true;
            posicao++;
        }
        for (boolean d : disparou) assertTrue(d);
        assertEquals(0, Tambor.carregadas(camaras));
    }

    @Test
    void conteudoNaoPulaQuandoOTiroAcaba() {
        // no fim do tiro a animação deixa o tambor 60° girado; voltar ao repouso com posição + 1 mostra o mesmo
        for (int p = 0; p < 6; p++) {
            for (int encaixe = 0; encaixe < 6; encaixe++) {
                int camaraNoFimDaAnimacao = Tambor.camaraDoEncaixe(encaixe, p);
                int encaixeDepois = (encaixe + 1) % 6;
                assertEquals(camaraNoFimDaAnimacao, Tambor.camaraDoEncaixe(encaixeDepois, p + 1));
            }
        }
    }

    @Test
    void recargaParcialSegueAOrdemDaPortinhola() {
        int vazio = 0;
        int carregar = Tambor.planejarRecarga(vazio, 0, 2);
        // os dois primeiros blocos cuidam dos encaixes 1 e 0 (posição 0 = câmaras 1 e 0)
        assertEquals((1 << 1) | (1 << 0), carregar);
        int depois = Tambor.depoisDaRecarga(vazio, carregar);
        assertEquals(2, Tambor.carregadas(depois));
    }

    @Test
    void recargaNaoGastaCartuchoEmCamaraCheia() {
        int camaras = Tambor.com(Tambor.com(0, 2, Tambor.CARREGADA), 4, Tambor.DEFLAGRADA);
        int carregar = Tambor.planejarRecarga(camaras, 0, 6);
        assertEquals(5, Integer.bitCount(carregar));
        assertFalse((carregar & (1 << 2)) != 0);
        int depois = Tambor.depoisDaRecarga(camaras, carregar);
        assertEquals(6, Tambor.carregadas(depois));
    }

    @Test
    void estojoDisparadoSemCartuchoSaiENaoVolta() {
        int camaras = Tambor.com(0, 1, Tambor.DEFLAGRADA);
        int encaixe = 1; // posição 0
        float antes = Tempos.inicioDoBloco(encaixe) + 2;
        float depois = Tempos.inicioDoBloco(encaixe) + Tempos.BLOCO_BALA_ENTRA;
        assertEquals(Tambor.Visual.ESTOJO, Tambor.visualNaRecarga(camaras, 0, 0, encaixe, antes));
        assertEquals(Tambor.Visual.NADA, Tambor.visualNaRecarga(camaras, 0, 0, encaixe, depois));
        assertEquals(Tambor.Visual.ESTOJO_E_BALA, Tambor.visualNaRecarga(camaras, 1 << 1, 0, encaixe, depois));
    }

    @Test
    void temposDosTicks() {
        assertEquals(0, Tempos.tick(1));
        assertEquals(14, Tempos.tick(Tempos.TIRO_DISPARO));
        assertEquals(33, Tempos.tickFimTiro());
        assertEquals(226, Tempos.tickFimRecarga());
    }
}
