package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.Pente;
import dev.revolver.logica.Tambor;
import dev.revolver.logica.TipoBala;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os cartuchos com material de outro mod, a bala infinita e o formato de 4 bits por bala (2.3.0). */
class TiposDeModTest {
    static final int C = TipoBala.COMUM.ordinal(), A = TipoBala.ACO.ordinal(), D = TipoBala.DIAMANTE.ordinal(),
            N = TipoBala.NETHERITE.ordinal(), INF = TipoBala.INFINITO.ordinal();

    @Test
    void ordemEDanos() {
        // os quatro de antes não mudam de número, os novos vêm depois, e tudo cabe em 4 bits
        assertEquals(List.of(0, 1, 2, 3), List.of(C, A, D, N));
        assertEquals(11, TipoBala.values().length);
        assertTrue(TipoBala.values().length <= 16);
        float anterior = 0;
        for (TipoBala t : TipoBala.values()) {
            assertTrue(t.dano > anterior, t + " tem que bater mais que o anterior");
            anterior = t.dano;
            assertEquals(t.mod == null, t.ordinal() <= N, t + ": só os novos vêm de outro mod");
            if (t.mod != null) assertTrue(t.material.startsWith(t.mod + ":"), t.material);
        }
        assertTrue(Float.isInfinite(TipoBala.INFINITO.dano(Calibre.C12)));
        assertTrue(TipoBala.INFINITO.infinito());
        assertFalse(TipoBala.ANTIMATERIA.infinito());
        assertNull(TipoBala.COMUM.mod);
    }

    @Test
    void efeitos() {
        assertEquals(5f, TipoBala.ARDENTE.fogo());
        assertEquals(8f, TipoBala.IGNITIUM.fogo());
        assertEquals(0f, TipoBala.NETHERITE.fogo());
        assertTrue(TipoBala.ANTIMATERIA.explode());
        assertFalse(TipoBala.INFINITO.explode());
    }

    @Test
    void melhorTipo() {
        assertEquals(C, TipoBala.melhor(List.of()));
        assertEquals(D, TipoBala.melhor(List.of(A, D, C)));
        assertEquals(INF, TipoBala.melhor(List.of(N, INF, A)));
    }

    @Test
    void tamborGuardaOsTiposNovos() {
        Tambor.Plano p = Tambor.planejar(0, 0, new int[]{INF, 9, 8, 7, 6, 5});
        int tipos = Tambor.tiposDepoisDaRecarga(0, 0, p.carregar(), p.tipos());
        // blocos passam pelas câmaras 1, 0, 5, 4, 3, 2
        assertEquals(INF, Tambor.tipo(tipos, 1));
        assertEquals(9, Tambor.tipo(tipos, 0));
        assertEquals(5, Tambor.tipo(tipos, 2));
    }

    @Test
    void penteCheioDeTiposNovosSobeNaOrdem() {
        int[] fila = {INF, 9, 8, 7, 6, 5, 4};
        Pente.Plano p = Pente.planejar(0, 0, fila);
        int pente = p.pente(), tipos = p.tipos();
        for (int esperado : fila) {
            Pente.Carga c = Pente.subir(pente, tipos);
            assertEquals(esperado, c.tipoCamara());
            pente = c.pente();
            tipos = c.tipos();
        }
        assertEquals(0, pente);
    }

    @Test
    void conversaoDoFormatoAntigo() {
        // 2 bits por lugar: N, D, A, C, N, D
        int antigo = 0;
        int[] lugares = {N, D, A, C, N, D};
        for (int i = 0; i < lugares.length; i++) antigo |= lugares[i] << (2 * i);
        int novo = Tambor.de2Para4Bits(antigo, 6);
        for (int i = 0; i < lugares.length; i++) assertEquals(lugares[i], Tambor.tipo(novo, i));
        for (int i = 0; i < lugares.length; i++) assertEquals(lugares[i], Pente.tipo(novo, i));
        // o pente da deagle tem 7 lugares: o sétimo também converte (28 bits)
        assertEquals(N, Pente.tipo(Tambor.de2Para4Bits(N << 12, 7), 6));
        // o codec dos estados (EstadoRevolver, EstadoDeagle) chama esta conversão quando falta "formato_tipos";
        // ele não roda aqui porque os testes não têm as classes do Minecraft
    }
}
