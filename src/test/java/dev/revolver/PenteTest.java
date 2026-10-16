package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.Pente;
import dev.revolver.logica.TipoBala;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PenteTest {
    static final int C = TipoBala.COMUM.ordinal(), A = TipoBala.ACO.ordinal(), D = TipoBala.DIAMANTE.ordinal(),
            N = TipoBala.NETHERITE.ordinal();

    @Test
    void danoDoPonto50EUmaVezEMeiaODoRevolver() {
        assertEquals(30f, TipoBala.COMUM.dano(Calibre.C50));
        assertEquals(75f, TipoBala.ACO.dano(Calibre.C50));
        assertEquals(150f, TipoBala.DIAMANTE.dano(Calibre.C50));
        assertEquals(300f, TipoBala.NETHERITE.dano(Calibre.C50));
        for (TipoBala t : TipoBala.values()) assertEquals(t.dano * 1.5f, t.dano(Calibre.C50));
        assertEquals("cartucho_50_diamante", TipoBala.DIAMANTE.item(Calibre.C50));
        assertEquals("cartucho_45", TipoBala.COMUM.item(Calibre.C45));
        assertEquals("cartucho_45_aco", TipoBala.ACO.item);
    }

    @Test
    void penteVazioRecebeSeteEOFerrolhoPoeAPrimeiraNaCamara() {
        Pente.Plano p = Pente.planejar(0, 0, new int[]{N, D, A, C, C, C, C, C, C});
        assertEquals(7, p.pente());
        assertEquals(7, p.usados());
        Pente.Carga c = Pente.subir(p.pente(), p.tipos());     // recarga com o ferrolho aberto
        assertEquals(1, c.camara());
        assertEquals(N, c.tipoCamara());
        assertEquals(6, c.pente());
        assertEquals(D, Pente.tipo(c.tipos(), 0));
        assertEquals(A, Pente.tipo(c.tipos(), 1));
    }

    @Test
    void seteMaisUmETiroAteAbrir() {
        // com bala na câmara a troca de pente deixa 7 + 1; cada tiro sobe a de cima; o último deixa aberta
        Pente.Plano p = Pente.planejar(0, 0, new int[]{C, C, C, C, C, C, C});
        int pente = p.pente(), tipos = p.tipos(), camara = 1, tiros = 0;
        while (camara == 1) {
            Pente.Carga c = Pente.subir(pente, tipos);
            pente = c.pente();
            tipos = c.tipos();
            camara = c.camara();
            tiros++;
        }
        assertEquals(8, tiros);
        assertEquals(0, pente);
    }

    @Test
    void sobrasDoPenteVelhoSaemPrimeiro() {
        Pente.Plano p = Pente.planejar(2, Pente.comTipo(Pente.comTipo(0, 0, N), 1, D), new int[]{C, C, C, C, C, C});
        assertEquals(7, p.pente());
        assertEquals(5, p.usados());
        assertEquals(N, Pente.tipo(p.tipos(), 0));
        assertEquals(D, Pente.tipo(p.tipos(), 1));
        for (int i = 2; i < 7; i++) assertEquals(C, Pente.tipo(p.tipos(), i));
    }

    @Test
    void penteCheioOuSemCartuchoNaoGastaNada() {
        assertEquals(0, Pente.planejar(7, 0, new int[]{C, C}).usados());
        Pente.Plano p = Pente.planejar(3, 0, new int[]{});
        assertEquals(0, p.usados());
        assertEquals(3, p.pente());
    }

    @Test
    void contaCamaraEPente() {
        int tipos = Pente.comTipo(Pente.comTipo(0, 0, N), 1, C);
        assertEquals(2, Pente.contar(2, tipos, 1, N, N));
        assertEquals(1, Pente.contar(2, tipos, 1, N, C));
        assertEquals(0, Pente.contar(2, tipos, 0, N, D));
    }
}
