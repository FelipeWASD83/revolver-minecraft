package dev.revolver.logica;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * As animações exportadas do Blender ({@code animacoes/revolver.json}): para cada peça, uma matriz 3x4 (em
 * linhas) por quadro, no espaço do item, aplicada sobre a peça na pose de repouso. Peça que não mexe numa
 * animação usa a matriz de repouso.
 */
public final class Animacao {
    private final Map<String, float[]> repouso = new HashMap<>();
    private final Map<String, Map<String, float[][]>> faixas = new HashMap<>();
    private final Map<String, Integer> quadros = new HashMap<>();
    private JsonObject eventos;

    public static Animacao ler(Reader leitor) {
        JsonObject raiz = JsonParser.parseReader(leitor).getAsJsonObject();
        Animacao a = new Animacao();
        for (Map.Entry<String, JsonElement> e : raiz.getAsJsonObject("repouso").entrySet()) {
            a.repouso.put(e.getKey(), vetor(e.getValue().getAsJsonArray()));
        }
        for (Map.Entry<String, JsonElement> anim : raiz.getAsJsonObject("animacoes").entrySet()) {
            JsonObject obj = anim.getValue().getAsJsonObject();
            a.quadros.put(anim.getKey(), obj.get("quadros").getAsInt());
            Map<String, float[][]> porPeca = new HashMap<>();
            for (Map.Entry<String, JsonElement> f : obj.getAsJsonObject("faixas").entrySet()) {
                JsonArray lista = f.getValue().getAsJsonArray();
                float[][] ms = new float[lista.size()][];
                for (int i = 0; i < ms.length; i++) ms[i] = vetor(lista.get(i).getAsJsonArray());
                porPeca.put(f.getKey(), ms);
            }
            a.faixas.put(anim.getKey(), porPeca);
        }
        a.eventos = raiz.getAsJsonObject("eventos");
        return a;
    }

    private static float[] vetor(JsonArray arr) {
        float[] v = new float[arr.size()];
        for (int i = 0; i < v.length; i++) v[i] = arr.get(i).getAsFloat();
        return v;
    }

    public int quadros(String animacao) {
        return quadros.getOrDefault(animacao, 0);
    }

    public JsonObject eventos() {
        return eventos;
    }

    public float[] repouso(String peca) {
        return repouso.get(peca);
    }

    public boolean temFaixa(String animacao, String peca) {
        Map<String, float[][]> f = faixas.get(animacao);
        return f != null && f.containsKey(peca);
    }

    /**
     * Matriz da peça no índice de quadro dado (0 = quadro 1; com fração, mistura os dois vizinhos). Fora da
     * animação ou sem animação, a de repouso. Na troca entre escondida e visível não mistura: fica com a mais perto.
     */
    public float[] matriz(String animacao, String peca, float indice) {
        Map<String, float[][]> f = animacao == null ? null : faixas.get(animacao);
        float[][] ms = f == null ? null : f.get(peca);
        if (ms == null) return repouso.get(peca);
        if (indice <= 0) return ms[0];
        if (indice >= ms.length - 1) return ms[ms.length - 1];
        int i = (int) Math.floor(indice);
        float t = indice - i;
        float[] a = ms[i], b = ms[i + 1];
        if (escondida(a) || escondida(b)) return t < 0.5f ? a : b;
        float[] r = new float[12];
        for (int k = 0; k < 12; k++) r[k] = a[k] + (b[k] - a[k]) * t;
        return r;
    }

    /** Peça com escala zero (escondida na animação). */
    public static boolean escondida(float[] m) {
        return Math.abs(determinante(m)) < 1e-6f;
    }

    static float determinante(float[] m) {
        return m[0] * (m[5] * m[10] - m[6] * m[9]) - m[1] * (m[4] * m[10] - m[6] * m[8])
                + m[2] * (m[4] * m[9] - m[5] * m[8]);
    }

    /** inversa(a) * b, para tirar o coice (movimento do corpo) de uma peça: usado no tiro sem bala. */
    public static float[] semCoice(float[] corpo, float[] peca) {
        return multiplicar(inversa(corpo), peca);
    }

    static float[] multiplicar(float[] a, float[] b) {
        float[] r = new float[12];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                float s = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j];
                if (j == 3) s += a[i * 4 + 3];
                r[i * 4 + j] = s;
            }
        }
        return r;
    }

    static float[] inversa(float[] m) {
        float det = determinante(m);
        float[] r = new float[12];
        float inv = 1f / det;
        r[0] = (m[5] * m[10] - m[6] * m[9]) * inv;
        r[1] = (m[2] * m[9] - m[1] * m[10]) * inv;
        r[2] = (m[1] * m[6] - m[2] * m[5]) * inv;
        r[4] = (m[6] * m[8] - m[4] * m[10]) * inv;
        r[5] = (m[0] * m[10] - m[2] * m[8]) * inv;
        r[6] = (m[2] * m[4] - m[0] * m[6]) * inv;
        r[8] = (m[4] * m[9] - m[5] * m[8]) * inv;
        r[9] = (m[1] * m[8] - m[0] * m[9]) * inv;
        r[10] = (m[0] * m[5] - m[1] * m[4]) * inv;
        for (int i = 0; i < 3; i++) {
            r[i * 4 + 3] = -(r[i * 4] * m[3] + r[i * 4 + 1] * m[7] + r[i * 4 + 2] * m[11]);
        }
        return r;
    }
}
