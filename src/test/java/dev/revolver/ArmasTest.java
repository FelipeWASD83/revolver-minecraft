package dev.revolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.revolver.logica.Arma;
import dev.revolver.logica.Calibre;
import dev.revolver.logica.Carregador;
import dev.revolver.logica.TipoBala;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As armas novas: os dados do {@link Arma} contra o que o exportador escreveu, os arquivos de cada uma e a munição. */
class ArmasTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/revolver");
    static final Path DATA = Path.of("src/main/resources/data/revolver");

    static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void quadrosEPecasBatemComOArquivoExportado() throws IOException {
        for (Arma arma : Arma.values()) {
            JsonObject anim = json(ASSETS.resolve("animacoes/" + arma.id + ".json"));
            JsonObject animacoes = anim.getAsJsonObject("animacoes");
            JsonObject eventos = anim.getAsJsonObject("eventos");
            assertEquals(arma.tiroQuadros, animacoes.getAsJsonObject("tiro").get("quadros").getAsInt(), arma.id);
            assertEquals(arma.disparo, eventos.getAsJsonObject("tiro").get("disparo").getAsInt(), arma.id);
            String recarga = arma.faixaRecarga();
            assertEquals(arma.recargaQuadros, animacoes.getAsJsonObject(recarga).get("quadros").getAsInt(), arma.id);
            if (!arma.tubo) {
                assertEquals(arma.troca, eventos.getAsJsonObject(recarga).get("troca").getAsInt(), arma.id);
            }
            List<String> pecas = new ArrayList<>();
            for (JsonElement p : anim.getAsJsonArray("pecas")) pecas.add(p.getAsString());
            assertEquals(arma.pecas, pecas, arma.id);
            for (Arma.Som s : arma.sons) {
                JsonObject ev = eventos.getAsJsonObject(s.faixa());
                boolean achou = false;
                for (var e : ev.entrySet()) achou |= e.getValue().getAsInt() == s.quadro();
                assertTrue(achou, arma.id + ": som " + s.som() + " no quadro " + s.quadro() + " sem evento");
            }
        }
    }

    @Test
    void cadaArmaTemModeloTexturaTraducaoEReceita() throws IOException {
        JsonObject pt = json(ASSETS.resolve("lang/pt_br.json"));
        JsonObject en = json(ASSETS.resolve("lang/en_us.json"));
        JsonObject sons = json(ASSETS.resolve("sounds.json"));
        for (Arma arma : Arma.values()) {
            List<String> pecas = new ArrayList<>(List.of("corpo", "clarao", "estojo"));
            pecas.addAll(arma.pecas);
            if (!arma.tubo) for (TipoBala t : TipoBala.values()) pecas.add(t.modelo);
            RecursosTest.conferirPecas(arma.id, pecas);
            JsonObject item = json(ASSETS.resolve("models/item/" + arma.id + ".json"));
            assertEquals("builtin/entity", item.get("parent").getAsString());
            assertTrue(pt.has("item.revolver." + arma.id) && en.has("item.revolver." + arma.id), arma.id);
            assertEquals("revolver:" + arma.id,
                    json(DATA.resolve("recipe/" + arma.id + ".json")).getAsJsonObject("result").get("id").getAsString());
            assertTrue(sons.has(arma.somTiro), arma.somTiro);
            for (Arma.Som s : arma.sons) assertTrue(sons.has(s.som()), s.som());
            for (TipoBala t : TipoBala.values()) {
                String nome = t.item(arma.calibre);
                assertTrue(Files.exists(ASSETS.resolve("textures/item/" + nome + ".png")), nome);
                assertTrue(Files.exists(ASSETS.resolve("models/item/" + nome + ".json")), nome);
                assertTrue(Files.exists(DATA.resolve("recipe/" + nome + ".json")), nome);
                assertTrue(pt.has("item.revolver." + nome) && en.has("item.revolver." + nome), nome);
            }
        }
    }

    @Test
    void pentesENumerosDaProposta() {
        // a M4A4 e a M4A1-S dividem o 5,56 (escolha dele), e a M4A1-S bate um pouco mais
        for (Arma arma : Arma.values()) {
            assertTrue(arma.capacidade > 0 && arma.bagos() > 0 && arma.tiroQuadros >= arma.disparo, arma.id);
        }
        assertEquals(20f, TipoBala.COMUM.dano(Calibre.C45));
        // a proposta aprovada, no cartucho comum (aço, diamante e netherite multiplicam 2,5, 5 e 10)
        assertEquals(10f, Arma.METRALHADORA.dano(TipoBala.COMUM), 1e-4f);
        assertEquals(4f, Arma.ESCOPETA.dano(TipoBala.COMUM), 1e-4f);
        assertEquals(8, Arma.ESCOPETA.bagos());
        assertEquals(12f, Arma.AK47.dano(TipoBala.COMUM), 1e-4f);
        assertEquals(10f, Arma.M4A4.dano(TipoBala.COMUM), 1e-4f);
        assertEquals(11f, Arma.M4A1.dano(TipoBala.COMUM), 1e-4f);
        assertEquals(50f, Arma.SNIPER.dano(TipoBala.COMUM), 1e-4f);
        assertEquals(500f, Arma.SNIPER.dano(TipoBala.NETHERITE), 1e-4f);
        assertEquals(Arma.M4A4.calibre, Arma.M4A1.calibre);
        assertEquals(List.of(100, 8, 30, 30, 20, 5), List.of(Arma.METRALHADORA.capacidade, Arma.ESCOPETA.capacidade,
                Arma.AK47.capacidade, Arma.M4A4.capacidade, Arma.M4A1.capacidade, Arma.SNIPER.capacidade));
    }

    @Test
    void carregadorDePente() {
        Carregador.Plano p = Carregador.planejar(List.of(3, 2), new int[]{0, 0, 0, 0, 0}, 5);
        assertEquals(List.of(3, 2, 0, 0, 0), p.municao());
        assertEquals(3, p.usados());
        assertEquals(0, Carregador.planejar(List.of(1, 1), new int[]{0}, 2).usados());
        assertEquals(List.of(2), Carregador.semAProxima(List.of(3, 2)));
        assertEquals(List.of(), Carregador.semAProxima(List.of()));
        assertEquals(List.of(1, 0, 0), Carregador.comUmNoTubo(List.of(0, 0), 1));
        assertEquals(2, Carregador.contar(List.of(1, 0, 1), 1));
    }
}
