package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.TipoBala;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * De onde vem a munição da recarga, igual para as oito armas. Os cartuchos saem como as flechas do arco: primeiro
 * os da outra mão, depois na ordem do inventário. No criativo nunca falta (completa com o comum) e nada é gasto. Com
 * a {@link BandoleiraItem Bandoleira do Fim} no inventário também nada é gasto, e a arma enche toda com o melhor tipo
 * do calibre que o jogador tiver (o comum, se não tiver nenhum).
 */
public final class Municao {
    private Municao() {}

    /** As pilhas de cartucho do calibre, na ordem em que saem. */
    public static List<ItemStack> pilhas(Player jogador, Calibre calibre) {
        List<ItemStack> todas = new ArrayList<>();
        todas.add(jogador.getOffhandItem());
        todas.addAll(jogador.getInventory().items);
        List<ItemStack> certas = new ArrayList<>();
        for (ItemStack p : todas) {
            if (!p.isEmpty() && p.getItem() instanceof CartuchoItem c && c.calibre == calibre) certas.add(p);
        }
        return certas;
    }

    public static boolean temBandoleira(Player jogador) {
        return jogador.getInventory().contains(s -> s.is(Registros.BANDOLEIRA.get()));
    }

    /** Recarga que não gasta nada: criativo ou bandoleira. */
    public static boolean semGasto(Player jogador) {
        return jogador.getAbilities().instabuild || temBandoleira(jogador);
    }

    /** O melhor tipo do calibre no inventário (o de número maior), ou o comum. */
    public static int melhor(Player jogador, Calibre calibre) {
        List<Integer> tipos = new ArrayList<>();
        for (ItemStack p : pilhas(jogador, calibre)) tipos.add(((CartuchoItem) p.getItem()).tipo.ordinal());
        return TipoBala.melhor(tipos);
    }

    /** A fila de tipos para a recarga, com até {@code precisa} balas. */
    public static int[] fila(Player jogador, Calibre calibre, int precisa) {
        if (precisa <= 0) return new int[0];
        if (temBandoleira(jogador)) {
            int[] cheia = new int[precisa];
            Arrays.fill(cheia, melhor(jogador, calibre));
            return cheia;
        }
        List<Integer> fila = new ArrayList<>();
        for (ItemStack pilha : pilhas(jogador, calibre)) {
            int tipo = ((CartuchoItem) pilha.getItem()).tipo.ordinal();
            for (int k = 0; k < pilha.getCount() && fila.size() < precisa; k++) fila.add(tipo);
        }
        if (jogador.getAbilities().instabuild) {
            while (fila.size() < precisa) fila.add(TipoBala.COMUM.ordinal());
        }
        return fila.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Tira do inventário os cartuchos que a recarga usou (na mesma ordem da fila); criativo e bandoleira não gastam. */
    public static void gastar(Player jogador, Calibre calibre, int quantos) {
        if (semGasto(jogador)) return;
        for (ItemStack pilha : pilhas(jogador, calibre)) {
            if (quantos <= 0) break;
            int tira = Math.min(quantos, pilha.getCount());
            pilha.shrink(tira);
            quantos -= tira;
        }
    }

    /** Escopeta: o próximo cartucho que entra no tubo (gasta um, se for o caso), ou -1 se não tem. */
    public static int tirarUm(Player jogador, Calibre calibre) {
        int[] f = fila(jogador, calibre, 1);
        if (f.length == 0) return -1;
        gastar(jogador, calibre, 1);
        return f[0];
    }

    public static boolean temAlgum(Player jogador, Calibre calibre) {
        return semGasto(jogador) || !pilhas(jogador, calibre).isEmpty();
    }
}
