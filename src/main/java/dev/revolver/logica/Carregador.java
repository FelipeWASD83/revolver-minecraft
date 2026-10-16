package dev.revolver.logica;

import java.util.ArrayList;
import java.util.List;

/**
 * A munição das armas de pente (fuzis, metralhadora, sniper) e de tubo (escopeta), sem nada do Minecraft. A lista
 * guarda o tipo de cada bala ({@link TipoBala}), a próxima a sair no lugar 0.
 */
public final class Carregador {
    private Carregador() {}

    /** O pente novo da recarga e quantos cartuchos ela gasta. */
    public record Plano(List<Integer> municao, int usados) {}

    /**
     * Recarga de pente: as balas que sobraram no velho continuam saindo primeiro, e as novas completam até a
     * capacidade, na ordem da fila (a do inventário, como o arco pega flecha).
     */
    public static Plano planejar(List<Integer> atual, int[] fila, int capacidade) {
        int usados = Math.max(0, Math.min(capacidade - atual.size(), fila.length));
        List<Integer> novos = new ArrayList<>(atual);
        for (int i = 0; i < usados; i++) novos.add(fila[i]);
        return new Plano(List.copyOf(novos), usados);
    }

    /** Tira a próxima bala: devolve o resto (a escopeta põe e tira do mesmo lado do tubo, mas a ordem não pesa). */
    public static List<Integer> semAProxima(List<Integer> municao) {
        return municao.isEmpty() ? municao : List.copyOf(municao.subList(1, municao.size()));
    }

    /** Escopeta: um cartucho entra no tubo, na frente da fila (é o próximo a sair, como num tubo de verdade). */
    public static List<Integer> comUmNoTubo(List<Integer> municao, int tipo) {
        List<Integer> novos = new ArrayList<>(municao.size() + 1);
        novos.add(tipo);
        novos.addAll(municao);
        return List.copyOf(novos);
    }

    public static int contar(List<Integer> municao, int tipo) {
        int n = 0;
        for (int t : municao) if (t == tipo) n++;
        return n;
    }
}
