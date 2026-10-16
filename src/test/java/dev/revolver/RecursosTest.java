package dev.revolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confere que tudo o que o jogo vai procurar existe e se lê, sem abrir o jogo. */
class RecursosTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/revolver");
    static final Path DATA = Path.of("src/main/resources/data/revolver");
    // o mesmo que dev.revolver.cliente.RevolverCliente.PECAS (essa classe é só do cliente)
    static final List<String> PECAS = List.of("corpo", "cao", "gatilho", "tambor", "portinhola", "botao", "clarao",
            "estojo", "bala");

    static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void cadaPecaTemModeloObjMtlETexturas() throws IOException {
        for (String peca : PECAS) {
            Path dir = ASSETS.resolve("models/item/revolver");
            JsonObject modelo = json(dir.resolve(peca + ".json"));
            assertEquals("neoforge:obj", modelo.get("loader").getAsString(), peca);
            assertEquals("revolver:models/item/revolver/" + peca + ".obj", modelo.get("model").getAsString());
            List<String> obj = Files.readAllLines(dir.resolve(peca + ".obj"));
            assertTrue(obj.stream().anyMatch(l -> l.startsWith("f ")), peca + " sem faces");
            assertTrue(obj.contains("mtllib " + peca + ".mtl"), peca + " sem mtllib");
            JsonObject texturas = modelo.getAsJsonObject("textures");
            for (String linha : Files.readAllLines(dir.resolve(peca + ".mtl"))) {
                if (!linha.startsWith("map_Kd")) continue;
                String nome = linha.substring(linha.indexOf('#') + 1).trim();
                assertTrue(texturas.has(nome), peca + ": textura #" + nome + " não está no JSON");
                String caminho = texturas.get(nome).getAsString().replace("revolver:", "");
                assertTrue(Files.exists(ASSETS.resolve("textures/" + caminho + ".png")), caminho);
            }
            // nenhuma face com mais de 4 vértices (o carregador do NeoForge corta)
            for (String l : obj) {
                if (l.startsWith("f ")) assertTrue(l.split(" ").length <= 5, peca + ": face grande demais");
            }
        }
    }

    @Test
    void itensTemModeloETextura() throws IOException {
        JsonObject revolver = json(ASSETS.resolve("models/item/revolver.json"));
        assertEquals("builtin/entity", revolver.get("parent").getAsString());
        for (String ctx : List.of("firstperson_righthand", "thirdperson_righthand", "gui", "ground", "fixed")) {
            assertTrue(revolver.getAsJsonObject("display").has(ctx), ctx);
        }
        JsonObject cartucho = json(ASSETS.resolve("models/item/cartucho_45.json"));
        assertEquals("revolver:item/cartucho_45", cartucho.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(Files.exists(ASSETS.resolve("textures/item/cartucho_45.png")));
    }

    @Test
    void cadaSomTemArquivo() throws IOException {
        JsonObject sons = json(ASSETS.resolve("sounds.json"));
        for (String evento : List.of("tiro", "engatilhar", "clique_seco", "portinhola", "estojo_cai", "bala_entra",
                "tambor_gira")) {
            assertTrue(sons.has(evento), evento);
            for (JsonElement s : sons.getAsJsonObject(evento).getAsJsonArray("sounds")) {
                String nome = s.getAsString().replace("revolver:", "");
                Path ogg = ASSETS.resolve("sounds/" + nome + ".ogg");
                assertTrue(Files.exists(ogg), nome);
                byte[] cabeca = Files.readAllBytes(ogg);
                assertEquals("OggS", new String(cabeca, 0, 4, StandardCharsets.US_ASCII), nome);
            }
        }
    }

    @Test
    void traducoesCompletasNasDuasLinguas() throws IOException {
        JsonObject pt = json(ASSETS.resolve("lang/pt_br.json"));
        JsonObject en = json(ASSETS.resolve("lang/en_us.json"));
        assertEquals(pt.keySet(), en.keySet());
        JsonObject sons = json(ASSETS.resolve("sounds.json"));
        for (Map.Entry<String, JsonElement> e : sons.entrySet()) {
            assertTrue(pt.has(e.getValue().getAsJsonObject().get("subtitle").getAsString()), e.getKey());
        }
        for (String chave : List.of("item.revolver.revolver", "item.revolver.cartucho_45", "key.revolver.recarregar",
                "key.categories.revolver", "item.revolver.revolver.balas")) {
            assertTrue(pt.has(chave), chave);
        }
    }

    @Test
    void receitasSeLeem() throws IOException {
        for (String r : List.of("revolver", "cartucho_45")) {
            JsonObject receita = json(DATA.resolve("recipe/" + r + ".json"));
            assertEquals("minecraft:crafting_shaped", receita.get("type").getAsString());
            assertEquals("revolver:" + r, receita.getAsJsonObject("result").get("id").getAsString());
        }
    }
}
