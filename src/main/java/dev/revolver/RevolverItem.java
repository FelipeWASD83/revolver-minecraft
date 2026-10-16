package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.Tambor;
import dev.revolver.logica.TipoBala;
import dev.revolver.logica.Tempos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * O revólver. Botão direito: arma o cão (o tambor gira) e atira; sem bala na câmara, clique seco. A tecla de
 * recarregar manda uma mensagem ao servidor ({@link RevolverItem#recarregar}). Tudo segue os quadros das animações
 * do Blender: o tiro sai no quadro em que o cão cai.
 */
public class RevolverItem extends Item {
    // o dano vem da bala que está na câmara (TipoBala): 20 no cartucho comum, 200 no de netherite

    public RevolverItem(Properties propriedades) {
        super(propriedades);
    }

    public static EstadoRevolver estado(ItemStack stack) {
        return stack.getOrDefault(Registros.ESTADO.get(), EstadoRevolver.VAZIO);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
        ItemStack stack = jogador.getItemInHand(mao);
        EstadoRevolver e = estado(stack);
        if (e.acao() != EstadoRevolver.PARADO) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            int camara = Tambor.camaraDoDisparo(e.posicao());
            boolean temBala = Tambor.estado(e.camaras(), camara) == Tambor.CARREGADA;
            stack.set(Registros.ESTADO.get(),
                    e.comecarTiro(temBala ? EstadoRevolver.TIRO : EstadoRevolver.TIRO_SECO, level.getGameTime()));
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Começa a recarga, se houver o que fazer. Chamado pelo servidor quando chega a mensagem da tecla. */
    public static void recarregar(Player jogador, InteractionHand mao) {
        ItemStack stack = jogador.getItemInHand(mao);
        if (!stack.is(Registros.REVOLVER.get())) return;
        EstadoRevolver e = estado(stack);
        if (e.acao() != EstadoRevolver.PARADO) return;
        boolean criativo = jogador.getAbilities().instabuild;
        // os cartuchos saem como as flechas do arco: primeiro os da outra mão, depois na ordem do inventário
        List<ItemStack> pilhas = new ArrayList<>();
        pilhas.add(jogador.getOffhandItem());
        pilhas.addAll(jogador.getInventory().items);
        int precisa = Tambor.semBala(e.camaras());
        List<Integer> fila = new ArrayList<>();
        for (ItemStack pilha : pilhas) {
            if (!(pilha.getItem() instanceof CartuchoItem cartucho) || cartucho.calibre != Calibre.C45) continue;
            for (int k = 0; k < pilha.getCount() && fila.size() < precisa; k++) fila.add(cartucho.tipo.ordinal());
        }
        if (criativo) {
            while (fila.size() < precisa) fila.add(TipoBala.COMUM.ordinal());   // no criativo nunca falta
        }
        Tambor.Plano plano = Tambor.planejar(e.camaras(), e.posicao(), fila.stream().mapToInt(Integer::intValue).toArray());
        if (plano.carregar() == 0 && !Tambor.temDeflagrada(e.camaras())) return;
        if (!criativo) {
            int gastar = Integer.bitCount(plano.carregar());
            for (ItemStack pilha : pilhas) {
                if (gastar == 0) break;
                if (!(pilha.getItem() instanceof CartuchoItem cartucho) || cartucho.calibre != Calibre.C45) continue;
                int tira = Math.min(gastar, pilha.getCount());
                pilha.shrink(tira);
                gastar -= tira;
            }
        }
        stack.set(Registros.ESTADO.get(),
                e.comecarRecarga(jogador.level().getGameTime(), plano.carregar(), plano.tipos()));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity dono, int slot, boolean selecionado) {
        if (level.isClientSide || !(dono instanceof Player jogador)) return;
        EstadoRevolver e = estado(stack);
        if (e.acao() == EstadoRevolver.PARADO) return;
        long dt = level.getGameTime() - e.inicio();
        if (e.acao() == EstadoRevolver.RECARGA) {
            tickRecarga(stack, level, jogador, e, dt);
        } else {
            tickTiro(stack, level, jogador, e, dt);
        }
    }

    private void tickTiro(ItemStack stack, Level level, Player jogador, EstadoRevolver e, long dt) {
        if (dt == Tempos.tick(Tempos.TIRO_ENGATILHA)) {
            som(level, jogador, Registros.SOM_ENGATILHAR.get(), 1.0f, 1.0f);
        }
        if (dt == Tempos.tick(Tempos.TIRO_DISPARO)) {
            if (e.acao() == EstadoRevolver.TIRO) {
                int camara = Tambor.camaraDoDisparo(e.posicao());
                TipoBala bala = TipoBala.de(Tambor.tipo(e.tipos(), camara));
                stack.set(Registros.ESTADO.get(), e.comCamaras(Tambor.com(e.camaras(), camara, Tambor.DEFLAGRADA)));
                som(level, jogador, Registros.SOM_TIRO.get(), 4.0f, 0.95f + level.random.nextFloat() * 0.1f);
                Disparo.atirar((ServerLevel) level, jogador, jogador.getOffhandItem() == stack, bala.dano(Calibre.C45));
            } else {
                som(level, jogador, Registros.SOM_CLIQUE_SECO.get(), 1.0f, 1.0f);
            }
        }
        if (dt >= Tempos.tickFimTiro()) {
            EstadoRevolver agora = estado(stack);
            stack.set(Registros.ESTADO.get(), agora.parado(agora.camaras(), agora.posicao() + 1, agora.tipos()));
        }
    }

    private void tickRecarga(ItemStack stack, Level level, Player jogador, EstadoRevolver e, long dt) {
        if (dt == Tempos.tick(Tempos.REC_MEIO_ARMADO)) som(level, jogador, Registros.SOM_TAMBOR_GIRA.get(), 1.0f, 0.8f);
        if (dt == Tempos.tick(Tempos.REC_ABRE)) som(level, jogador, Registros.SOM_PORTINHOLA.get(), 1.0f, 1.0f);
        for (int bloco = 0; bloco < 6; bloco++) {
            int inicio = Tempos.REC_T0 + bloco * Tempos.REC_BLOCO;
            int camara = Tambor.camaraDoEncaixe(Tambor.encaixeDoBloco(bloco), e.posicao());
            boolean tinha = Tambor.estado(e.camaras(), camara) != Tambor.VAZIA;
            boolean recebe = Tambor.estado(e.camaras(), camara) == Tambor.CARREGADA
                    || (e.carregar() & (1 << camara)) != 0;
            if (tinha && dt == Tempos.tick(inicio + Tempos.BLOCO_ESTOJO_SOME)) {
                som(level, jogador, Registros.SOM_ESTOJO_CAI.get(), 1.0f, 1.0f);
            }
            if (recebe && dt == Tempos.tick(inicio + Tempos.BLOCO_BALA_ENTRA)) {
                som(level, jogador, Registros.SOM_BALA_ENTRA.get(), 1.0f, 1.0f);
            }
            if (dt == Tempos.tick(inicio + Tempos.BLOCO_GIRA)) {
                som(level, jogador, Registros.SOM_TAMBOR_GIRA.get(), 1.0f, 1.0f);
            }
        }
        if (dt == Tempos.tick(Tempos.REC_FECHA)) som(level, jogador, Registros.SOM_PORTINHOLA.get(), 1.0f, 0.9f);
        if (dt == Tempos.tick(Tempos.REC_ENGATILHA)) som(level, jogador, Registros.SOM_ENGATILHAR.get(), 1.0f, 1.0f);
        if (dt >= Tempos.tickFimRecarga()) {
            stack.set(Registros.ESTADO.get(), e.parado(Tambor.depoisDaRecarga(e.camaras(), e.carregar()), e.posicao(),
                    Tambor.tiposDepoisDaRecarga(e.camaras(), e.tipos(), e.carregar(), e.tiposNovos())));
        }
    }

    private static void som(Level level, Player jogador, SoundEvent som, float volume, float tom) {
        level.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), som, SoundSource.PLAYERS, volume, tom);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antigo, ItemStack novo, boolean trocouSlot) {
        // a cada tiro o item muda (o estado vai junto); sem isso a mão abaixaria e subiria a cada mudança
        return trocouSlot || !novo.is(antigo.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> linhas, TooltipFlag flag) {
        EstadoRevolver e = estado(stack);
        linhas.add(Component.translatable("item.revolver.revolver.balas", Tambor.carregadas(e.camaras()))
                .withStyle(ChatFormatting.GRAY));
        TipoBala[] tipos = TipoBala.values();
        for (int i = tipos.length - 1; i >= 0; i--) {     // quantas de cada tipo, da melhor para a pior
            TipoBala t = tipos[i];
            int n = 0;
            for (int c = 0; c < 6; c++) {
                if (Tambor.estado(e.camaras(), c) == Tambor.CARREGADA && Tambor.tipo(e.tipos(), c) == t.ordinal()) n++;
            }
            if (n > 0) {
                linhas.add(Component.translatable("item.revolver.revolver.tipo", n,
                        Component.translatable("item.revolver." + t.item), (int) t.dano).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
