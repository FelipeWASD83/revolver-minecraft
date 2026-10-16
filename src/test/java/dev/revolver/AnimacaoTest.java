package dev.revolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.revolver.logica.Animacao;
import dev.revolver.logica.Tambor;
import dev.revolver.logica.Tempos;
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

class AnimacaoTest {
    static Animacao anim;

    @BeforeAll
    static void ler() throws IOException {
        Path arquivo = Path.of("src/main/resources/assets/revolver/animacoes/revolver.json");
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            anim = Animacao.ler(r);
        }
    }

    @Test
    void quadrosEEventosBatemComOJava() {
        assertEquals(Tempos.TIRO_QUADROS, anim.quadros("tiro"));
        assertEquals(Tempos.REC_QUADROS, anim.quadros("recarregar"));
        JsonObject tiro = anim.eventos().getAsJsonObject("tiro");
        assertEquals(Tempos.TIRO_ENGATILHA, tiro.get("engatilha").getAsInt());
        assertEquals(Tempos.TIRO_DISPARO, tiro.get("disparo").getAsInt());
        JsonObject rec = anim.eventos().getAsJsonObject("recarregar");
        assertEquals(Tempos.REC_MEIO_ARMADO, rec.get("meio_armado").getAsInt());
        assertEquals(Tempos.REC_ABRE, rec.get("abre_portinhola").getAsInt());
        assertEquals(Tempos.REC_FECHA, rec.get("fecha_portinhola").getAsInt());
        assertEquals(Tempos.REC_ENGATILHA, rec.get("engatilha").getAsInt());
        JsonArray blocos = rec.getAsJsonArray("blocos");
        for (int i = 0; i < 6; i++) {
            JsonObject b = blocos.get(i).getAsJsonObject();
            assertEquals(Tambor.encaixeDoBloco(i), b.get("camara").getAsInt());
            int inicio = b.get("inicio").getAsInt();
            assertEquals(Tempos.REC_T0 + i * Tempos.REC_BLOCO, inicio);
            assertEquals(inicio + Tempos.BLOCO_ESTOJO_SOME, b.get("estojo_some").getAsInt());
            assertEquals(inicio + Tempos.BLOCO_BALA_APARECE, b.get("bala_aparece").getAsInt());
            assertEquals(inicio + Tempos.BLOCO_BALA_ENTRA, b.get("bala_entra").getAsInt());
        }
    }

    @Test
    void corpoEmRepousoNaoMexe() {
        float[] id = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
        assertArrayEquals(id, anim.repouso("corpo"), 1e-5f);
        assertArrayEquals(id, anim.matriz(null, "corpo", 0), 1e-5f);
    }

    @Test
    void aBalaDoEncaixeDoDisparoSomeNoQuadroDoTiro() {
        int i = Tempos.TIRO_DISPARO - 1;
        assertFalse(Animacao.escondida(anim.matriz("tiro", "bala5", i - 1)));
        assertTrue(Animacao.escondida(anim.matriz("tiro", "bala5", i)));
        assertFalse(Animacao.escondida(anim.matriz("tiro", "bala4", i)));
    }

    @Test
    void oClaraoSoApareceNoTiro() {
        assertTrue(Animacao.escondida(anim.repouso("clarao")));
        assertTrue(Animacao.escondida(anim.matriz("tiro", "clarao", Tempos.TIRO_DISPARO - 2)));
        assertFalse(Animacao.escondida(anim.matriz("tiro", "clarao", Tempos.TIRO_DISPARO - 1)));
    }

    @Test
    void oCaoArmaEOCorpoDaOCoice() {
        float[] armado = anim.matriz("tiro", "cao", Tempos.TIRO_DISPARO - 3);
        assertFalse(java.util.Arrays.equals(armado, anim.repouso("cao")));
        float[] coice = anim.matriz("tiro", "corpo", Tempos.TIRO_DISPARO + 1);
        assertFalse(java.util.Arrays.equals(coice, anim.repouso("corpo")));
    }

    @Test
    void semCoiceDeVoltaAoCorpoDaIdentidade() {
        float[] corpo = anim.matriz("tiro", "corpo", Tempos.TIRO_DISPARO + 1);
        float[] id = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
        assertArrayEquals(id, Animacao.semCoice(corpo, corpo), 1e-4f);
    }

    @Test
    void misturaEntreQuadrosFicaNoMeio() {
        float[] a = anim.matriz("recarregar", "portinhola", 12);
        float[] b = anim.matriz("recarregar", "portinhola", 13);
        float[] meio = anim.matriz("recarregar", "portinhola", 12.5f);
        for (int k = 0; k < 12; k++) assertEquals((a[k] + b[k]) / 2, meio[k], 1e-5f);
    }
}
