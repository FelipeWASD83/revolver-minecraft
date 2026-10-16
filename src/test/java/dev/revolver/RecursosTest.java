package dev.revolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.revolver.logica.Calibre;
import dev.revolver.logica.TipoBala;
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
            "estojo", "bala_comum", "bala_aco", "bala_diamante", "bala_netherite");
    // o mesmo que dev.revolver.cliente.RevolverCliente.PECAS_DEAGLE
    static final List<String> PECAS_DEAGLE = List.of("corpo", "ferrolho", "cao", "gatilho", "trava", "botao", "pente",
            "clarao", "estojo", "bala_comum", "bala_aco", "bala_diamante", "bala_netherite");

    static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void cadaPecaTemModeloObjMtlETexturas() throws IOException {
        conferirPecas("revolver", PECAS);
        conferirPecas("deagle", PECAS_DEAGLE);
    }

    static void conferirPecas(String arma, List<String> pecas) throws IOException {
        for (String peca : pecas) {
            Path dir = ASSETS.resolve("models/item/" + arma);
            JsonObject modelo = json(dir.resolve(peca + ".json"));
            assertEquals("neoforge:obj", modelo.get("loader").getAsString(), peca);
            assertEquals("revolver:models/item/" + arma + "/" + peca + ".obj", modelo.get("model").getAsString());
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
            // no atlas a textura não repete: coordenada fora (ou no canto exato) do sprite deixa a peça pontilhada
            for (String l : obj) {
                if (!l.startsWith("vt ")) continue;
                String[] p = l.split(" ");
                for (int k = 1; k <= 2; k++) {
                    double c = Double.parseDouble(p[k]);
                    assertTrue(c >= 0.005 && c <= 0.995, peca + ": coordenada de textura na borda: " + l);
                }
            }
        }
    }

    @Test
    void itensTemModeloETextura() throws IOException {
        for (String arma : List.of("revolver", "deagle")) {
            JsonObject item = json(ASSETS.resolve("models/item/" + arma + ".json"));
            assertEquals("builtin/entity", item.get("parent").getAsString());
            for (String ctx : List.of("firstperson_righthand", "thirdperson_righthand", "gui", "ground", "fixed")) {
                assertTrue(item.getAsJsonObject("display").has(ctx), arma + " " + ctx);
            }
        }
        for (Calibre calibre : Calibre.values()) {
            for (TipoBala tipo : TipoBala.values()) {
                String nome = tipo.item(calibre);
                JsonObject cartucho = json(ASSETS.resolve("models/item/" + nome + ".json"));
                assertEquals("revolver:item/" + nome, cartucho.getAsJsonObject("textures").get("layer0").getAsString());
                assertTrue(Files.exists(ASSETS.resolve("textures/item/" + nome + ".png")), nome);
                assertTrue(PECAS.contains(tipo.modelo) && PECAS_DEAGLE.contains(tipo.modelo), tipo.modelo);
            }
        }
    }

    @Test
    void cadaSomTemArquivo() throws IOException {
        JsonObject sons = json(ASSETS.resolve("sounds.json"));
        for (String evento : List.of("tiro", "engatilhar", "clique_seco", "portinhola", "estojo_cai", "bala_entra",
                "tambor_gira", "tiro_deagle", "ferrolho", "trava", "pente_sai", "pente_entra")) {
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
        for (String chave : List.of("item.revolver.revolver", "key.revolver.recarregar", "key.categories.revolver",
                "item.revolver.revolver.balas", "item.revolver.revolver.tipo", "item.revolver.cartucho.dano",
                "item.revolver.deagle", "item.revolver.deagle.balas")) {
            assertTrue(pt.has(chave), chave);
        }
        for (Calibre calibre : Calibre.values()) {
            for (TipoBala tipo : TipoBala.values()) {
                assertTrue(pt.has("item.revolver." + tipo.item(calibre)), tipo.item(calibre));
            }
        }
    }

    @Test
    void receitasSeLeem() throws IOException {
        for (String r : List.of("revolver", "cartucho_45", "cartucho_45_aco", "cartucho_45_diamante",
                "cartucho_45_netherite", "deagle", "cartucho_50", "cartucho_50_aco", "cartucho_50_diamante",
                "cartucho_50_netherite")) {
            JsonObject receita = json(DATA.resolve("recipe/" + r + ".json"));
            assertEquals("minecraft:crafting_shaped", receita.get("type").getAsString());
            assertEquals("revolver:" + r, receita.getAsJsonObject("result").get("id").getAsString());
        }
    }
}
