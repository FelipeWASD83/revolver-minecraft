package dev.revolver;

import com.google.gson.JsonObject;
import dev.revolver.logica.Animacao;
import dev.revolver.logica.TemposDeagle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimacaoDeagleTest {
    static Animacao anim;

    @BeforeAll
    static void ler() throws IOException {
        Path arquivo = Path.of("src/main/resources/assets/revolver/animacoes/deagle.json");
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            anim = Animacao.ler(r);
        }
    }

    @Test
    void quadrosEEventosBatemComOJava() {
        assertEquals(TemposDeagle.TIRO_QUADROS, anim.quadros("tiro"));
        assertEquals(TemposDeagle.TIRO_QUADROS, anim.quadros("tiro_ultimo"));
        assertEquals(TemposDeagle.SECO_QUADROS, anim.quadros("seco"));
        assertEquals(TemposDeagle.REC_QUADROS, anim.quadros("recarregar"));
        assertEquals(TemposDeagle.VAZIA_QUADROS, anim.quadros("recarregar_vazia"));
        JsonObject ev = anim.eventos();
        assertEquals(TemposDeagle.TIRO_DISPARO, ev.getAsJsonObject("tiro").get("disparo").getAsInt());
        assertEquals(TemposDeagle.TIRO_ESTOJO_CAI, ev.getAsJsonObject("tiro").get("estojo_cai").getAsInt());
        assertEquals(TemposDeagle.TIRO_TRAVA, ev.getAsJsonObject("tiro_ultimo").get("trava").getAsInt());
        assertEquals(TemposDeagle.SECO_CLIQUE, ev.getAsJsonObject("seco").get("clique").getAsInt());
        for (String rec : new String[]{"recarregar", "recarregar_vazia"}) {
            JsonObject r = ev.getAsJsonObject(rec);
            assertEquals(TemposDeagle.REC_BOTAO, r.get("botao").getAsInt());
            assertEquals(TemposDeagle.REC_PENTE_SAI, r.get("pente_sai").getAsInt());
            assertEquals(TemposDeagle.REC_TROCA, r.get("troca").getAsInt());
            assertEquals(TemposDeagle.REC_PENTE_ENTRA, r.get("pente_entra").getAsInt());
        }
        JsonObject vazia = ev.getAsJsonObject("recarregar_vazia");
        assertEquals(TemposDeagle.VAZIA_SOLTA_TRAVA, vazia.get("solta_trava").getAsInt());
        assertEquals(TemposDeagle.VAZIA_FERROLHO, vazia.get("ferrolho").getAsInt());
    }

    @Test
    void corpoEmRepousoNaoMexeEAsEscondidasEstaoEscondidas() {
        float[] id = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
        assertArrayEquals(id, anim.repouso("corpo"), 1e-5f);
        assertTrue(Animacao.escondida(anim.repouso("clarao")));
        assertTrue(Animacao.escondida(anim.repouso("ejetado")));
        assertFalse(Animacao.escondida(anim.repouso("topo_bala")));
    }

    @Test
    void oPenteSomeNaTrocaEVoltaNoLugar() {
        // entre o pente velho sumir e o novo aparecer ele fica escondido; no fim volta ao repouso
        assertTrue(Animacao.escondida(anim.matriz("recarregar", "pente", TemposDeagle.REC_TROCA - 1)));
        float[] fim = anim.matriz("recarregar", "pente", TemposDeagle.REC_QUADROS - 1);
        assertArrayEquals(anim.repouso("pente"), fim, 1e-4f);
    }

    @Test
    void abertaTemOFerrolhoAtrasEOTiroComumVoltaAoLugar() {
        float[] aberta = anim.matriz("recarregar_vazia", "ferrolho", 0);
        float[] repouso = anim.repouso("ferrolho");
        assertTrue(aberta[3] < repouso[3] - 0.1f, "o ferrolho aberto fica para trás (-X)");
        assertArrayEquals(aberta, anim.matriz("tiro_ultimo", "ferrolho", TemposDeagle.TIRO_QUADROS - 1), 1e-4f);
        assertArrayEquals(repouso, anim.matriz("tiro", "ferrolho", TemposDeagle.TIRO_QUADROS - 1), 1e-4f);
        assertArrayEquals(repouso, anim.matriz("recarregar_vazia", "ferrolho", TemposDeagle.VAZIA_QUADROS - 1), 1e-4f);
    }
}
