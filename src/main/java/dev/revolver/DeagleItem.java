package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.Pente;
import dev.revolver.logica.TemposDeagle;
import dev.revolver.logica.Tempos;
import dev.revolver.logica.TipoBala;
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
 * A Desert Eagle .50 AE. Botão direito: atira (semiautomática, o tiro sai logo); no último o ferrolho fica aberto, e
 * aberta ela só dá o clique. A tecla de recarregar troca o pente ({@link #recarregar}). Dano: o do tipo de bala vezes
 * 1,5 ({@link Calibre#C50}). Tudo segue os quadros das animações do Blender ({@link TemposDeagle}).
 */
public class DeagleItem extends Item {
    public DeagleItem(Properties propriedades) {
        super(propriedades);
    }

    public static EstadoDeagle estado(ItemStack stack) {
        return stack.getOrDefault(Registros.ESTADO_DEAGLE.get(), EstadoDeagle.VAZIO);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
        ItemStack stack = jogador.getItemInHand(mao);
        EstadoDeagle e = estado(stack);
        if (e.acao() != EstadoDeagle.PARADO) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            int acao = e.camara() == 0 ? EstadoDeagle.SECO
                    : (e.pente() > 0 ? EstadoDeagle.TIRO : EstadoDeagle.TIRO_ULTIMO);
            stack.set(Registros.ESTADO_DEAGLE.get(), e.comecar(acao, level.getGameTime()));
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Troca o pente, se houver cartucho .50 para pôr. Chamado pelo servidor quando chega a mensagem da tecla. */
    public static void recarregar(Player jogador, InteractionHand mao) {
        ItemStack stack = jogador.getItemInHand(mao);
        if (!stack.is(Registros.DEAGLE.get())) return;
        EstadoDeagle e = estado(stack);
        if (e.acao() != EstadoDeagle.PARADO) return;
        boolean criativo = jogador.getAbilities().instabuild;
        // os cartuchos saem como as flechas do arco: primeiro os da outra mão, depois na ordem do inventário
        List<ItemStack> pilhas = new ArrayList<>();
        pilhas.add(jogador.getOffhandItem());
        pilhas.addAll(jogador.getInventory().items);
        int precisa = Pente.CAPACIDADE - e.pente();
        List<Integer> fila = new ArrayList<>();
        for (ItemStack pilha : pilhas) {
            if (!(pilha.getItem() instanceof CartuchoItem cartucho) || cartucho.calibre != Calibre.C50) continue;
            for (int k = 0; k < pilha.getCount() && fila.size() < precisa; k++) fila.add(cartucho.tipo.ordinal());
        }
        if (criativo) {
            while (fila.size() < precisa) fila.add(TipoBala.COMUM.ordinal());   // no criativo nunca falta
        }
        Pente.Plano plano = Pente.planejar(e.pente(), e.tipos(), fila.stream().mapToInt(Integer::intValue).toArray());
        if (plano.usados() == 0) return;
        if (!criativo) {
            int gastar = plano.usados();
            for (ItemStack pilha : pilhas) {
                if (gastar == 0) break;
                if (!(pilha.getItem() instanceof CartuchoItem cartucho) || cartucho.calibre != Calibre.C50) continue;
                int tira = Math.min(gastar, pilha.getCount());
                pilha.shrink(tira);
                gastar -= tira;
            }
        }
        stack.set(Registros.ESTADO_DEAGLE.get(),
                e.comecarRecarga(jogador.level().getGameTime(), plano.pente(), plano.tipos()));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity dono, int slot, boolean selecionado) {
        if (level.isClientSide || !(dono instanceof Player jogador)) return;
        EstadoDeagle e = estado(stack);
        if (e.acao() == EstadoDeagle.PARADO) return;
        long dt = level.getGameTime() - e.inicio();
        switch (e.acao()) {
            case EstadoDeagle.TIRO, EstadoDeagle.TIRO_ULTIMO -> tickTiro(stack, level, jogador, e, dt);
            case EstadoDeagle.SECO -> {
                if (dt == Tempos.tick(TemposDeagle.SECO_CLIQUE)) som(level, jogador, Registros.SOM_CLIQUE_SECO.get(), 1.1f);
                if (dt >= Tempos.tick(TemposDeagle.SECO_QUADROS)) stack.set(Registros.ESTADO_DEAGLE.get(), e.parado());
            }
            default -> tickRecarga(stack, level, jogador, e, dt);
        }
    }

    private void tickTiro(ItemStack stack, Level level, Player jogador, EstadoDeagle e, long dt) {
        if (dt == Tempos.tick(TemposDeagle.TIRO_DISPARO)) {
            TipoBala bala = TipoBala.de(e.tipoCamara());
            // o ferrolho volta trazendo a de cima do pente para a câmara (ou fica aberto, se o pente acabou)
            Pente.Carga depois = Pente.subir(e.pente(), e.tipos());
            stack.set(Registros.ESTADO_DEAGLE.get(),
                    e.comCarga(depois.pente(), depois.tipos(), depois.camara(), depois.tipoCamara()));
            level.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), Registros.SOM_TIRO_DEAGLE.get(),
                    SoundSource.PLAYERS, 4.0f, 0.95f + level.random.nextFloat() * 0.1f);
            Disparo.atirar((ServerLevel) level, jogador, jogador.getOffhandItem() == stack, bala.dano(Calibre.C50));
        }
        if (e.acao() == EstadoDeagle.TIRO_ULTIMO && dt == Tempos.tick(TemposDeagle.TIRO_TRAVA)) {
            som(level, jogador, Registros.SOM_TRAVA.get(), 1.0f);
        }
        if (dt == Tempos.tick(TemposDeagle.TIRO_ESTOJO_CAI)) som(level, jogador, Registros.SOM_ESTOJO_CAI.get(), 1.0f);
        if (dt >= Tempos.tick(TemposDeagle.TIRO_QUADROS)) stack.set(Registros.ESTADO_DEAGLE.get(), estado(stack).parado());
    }

    private void tickRecarga(ItemStack stack, Level level, Player jogador, EstadoDeagle e, long dt) {
        boolean vazia = e.acao() == EstadoDeagle.RECARGA_VAZIA;
        if (dt == Tempos.tick(TemposDeagle.REC_BOTAO)) som(level, jogador, Registros.SOM_PENTE_SAI.get(), 1.0f);
        if (dt == Tempos.tick(TemposDeagle.REC_PENTE_ENTRA)) som(level, jogador, Registros.SOM_PENTE_ENTRA.get(), 1.0f);
        if (vazia && dt == Tempos.tick(TemposDeagle.VAZIA_FERROLHO)) som(level, jogador, Registros.SOM_FERROLHO.get(), 1.0f);
        if (dt >= Tempos.tick(vazia ? TemposDeagle.VAZIA_QUADROS : TemposDeagle.REC_QUADROS)) {
            EstadoDeagle novo = e.comCarga(e.novoPente(), e.novosTipos(), e.camara(), e.tipoCamara());
            if (vazia) {            // soltar o ferrolho põe na câmara a de cima do pente novo
                Pente.Carga c = Pente.subir(e.novoPente(), e.novosTipos());
                novo = e.comCarga(c.pente(), c.tipos(), c.camara(), c.tipoCamara());
            }
            stack.set(Registros.ESTADO_DEAGLE.get(), novo.parado());
        }
    }

    private static void som(Level level, Player jogador, SoundEvent som, float tom) {
        level.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), som, SoundSource.PLAYERS, 1.0f, tom);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antigo, ItemStack novo, boolean trocouSlot) {
        // a cada tiro o item muda (o estado vai junto); sem isso a mão abaixaria e subiria a cada mudança
        return trocouSlot || !novo.is(antigo.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> linhas, TooltipFlag flag) {
        EstadoDeagle e = estado(stack);
        linhas.add(Component.translatable("item.revolver.deagle.balas", e.pente(), e.camara())
                .withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("item.revolver.arma.calibre",
                Component.translatable("item.revolver." + Calibre.C50.base)).withStyle(ChatFormatting.GRAY));
        TipoBala[] tipos = TipoBala.values();
        for (int i = tipos.length - 1; i >= 0; i--) {     // quantas de cada tipo, da melhor para a pior
            TipoBala t = tipos[i];
            int n = Pente.contar(e.pente(), e.tipos(), e.camara(), e.tipoCamara(), t.ordinal());
            if (n > 0) {
                linhas.add(Component.translatable("item.revolver.revolver.tipo", n,
                                Component.translatable("item.revolver." + t.item(Calibre.C50)), (int) t.dano(Calibre.C50))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
