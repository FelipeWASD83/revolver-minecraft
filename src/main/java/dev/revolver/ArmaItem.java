package dev.revolver;

import dev.revolver.logica.Arma;
import dev.revolver.logica.Carregador;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * As armas novas, todas pela mesma classe e cada uma pelos dados do {@link Arma}. Botão direito atira: as automáticas
 * enquanto ele estiver segurado (uma bala a cada faixa de tiro), as outras uma vez por clique, no quadro do disparo.
 * A tecla de recarregar troca o pente ou, na escopeta, põe cartucho por cartucho até encher. Tudo segue os quadros das
 * animações do Blender.
 */
public class ArmaItem extends Item {
    public final Arma arma;

    public ArmaItem(Arma arma, Properties propriedades) {
        super(propriedades);
        this.arma = arma;
    }

    public static EstadoArma estado(ItemStack stack) {
        return stack.getOrDefault(Registros.ESTADO_ARMA.get(), EstadoArma.VAZIO);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
        ItemStack stack = jogador.getItemInHand(mao);
        EstadoArma e = estado(stack);
        if (e.acao() == EstadoArma.RECARGA) return InteractionResultHolder.fail(stack);
        if (arma.automatica) {
            jogador.startUsingItem(mao);              // o tiro sai no onUseTick, enquanto o botão estiver segurado
            return InteractionResultHolder.consume(stack);
        }
        if (e.acao() != EstadoArma.PARADO) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            if (e.municao().isEmpty()) {
                som(level, jogador, Registros.SOM_CLIQUE_SECO.get());
            } else {
                stack.set(Registros.ESTADO_ARMA.get(), e.comecar(EstadoArma.TIRO, level.getGameTime()));
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity quem) {
        return arma.automatica ? 72000 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity quem, ItemStack stack, int restante) {
        if (level.isClientSide || !arma.automatica || !(quem instanceof Player jogador)) return;
        EstadoArma e = estado(stack);
        if (e.acao() == EstadoArma.RECARGA) return;
        long agora = level.getGameTime();
        if (e.acao() == EstadoArma.TIRO && agora - e.inicio() < Tempos.tick(arma.tiroQuadros)) return;
        if (e.municao().isEmpty()) {
            som(level, jogador, Registros.SOM_CLIQUE_SECO.get());
            stack.set(Registros.ESTADO_ARMA.get(), e.parado());
            jogador.stopUsingItem();
            return;
        }
        disparar(stack, (ServerLevel) level, jogador, e.comecar(EstadoArma.TIRO, agora));
    }

    /** A bala de cima sai: o dano é o do tipo dela. */
    private void disparar(ItemStack stack, ServerLevel level, Player jogador, EstadoArma e) {
        TipoBala tipo = TipoBala.de(e.municao().get(0));
        stack.set(Registros.ESTADO_ARMA.get(), e.comMunicao(Carregador.semAProxima(e.municao())));
        level.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), Registros.somPorNome(arma.somTiro),
                SoundSource.PLAYERS, 4.0f, 0.95f + level.random.nextFloat() * 0.1f);
        Disparo.atirar(level, jogador, jogador.getOffhandItem() == stack, arma.dano(tipo), arma.bagos(),
                arma.espalhamento);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity dono, int slot, boolean selecionado) {
        if (level.isClientSide || !(dono instanceof Player jogador)) return;
        EstadoArma e = estado(stack);
        if (e.acao() == EstadoArma.PARADO) return;
        long dt = level.getGameTime() - e.inicio();
        if (e.acao() == EstadoArma.TIRO) {
            if (!arma.automatica && dt == Tempos.tick(arma.disparo) && !e.municao().isEmpty()) {
                disparar(stack, (ServerLevel) level, jogador, e);
            }
            tocarSons(level, jogador, "tiro", dt);
            if (dt >= Tempos.tick(arma.tiroQuadros)) stack.set(Registros.ESTADO_ARMA.get(), estado(stack).parado());
            return;
        }
        tocarSons(level, jogador, arma.faixaRecarga(), dt);
        if (dt < Tempos.tick(arma.recargaQuadros)) return;
        if (!arma.tubo) {
            stack.set(Registros.ESTADO_ARMA.get(), e.comMunicao(e.novos()).parado());
            return;
        }
        // escopeta: o cartucho desta volta entra no tubo; se ainda cabe e tem cartucho, começa a próxima
        int tipo = tirarCartucho(jogador);
        EstadoArma depois = tipo < 0 ? e.parado() : e.comMunicao(Carregador.comUmNoTubo(e.municao(), tipo));
        if (tipo >= 0 && depois.municao().size() < arma.capacidade && temCartucho(jogador)) {
            depois = depois.comecar(EstadoArma.RECARGA, level.getGameTime());
        } else {
            depois = depois.parado();
        }
        stack.set(Registros.ESTADO_ARMA.get(), depois);
    }

    private void tocarSons(Level level, Player jogador, String faixa, long dt) {
        for (Arma.Som s : arma.sons) {
            if (s.faixa().equals(faixa) && dt == Tempos.tick(s.quadro())) som(level, jogador, Registros.somPorNome(s.som()));
        }
    }

    private static void som(Level level, Player jogador, SoundEvent som) {
        level.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), som, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** Começa a recarga, se houver o que fazer. Chamado pelo servidor quando chega a mensagem da tecla. */
    public static void recarregar(Player jogador, InteractionHand mao) {
        ItemStack stack = jogador.getItemInHand(mao);
        if (!(stack.getItem() instanceof ArmaItem item)) return;
        Arma arma = item.arma;
        EstadoArma e = estado(stack);
        if (e.acao() != EstadoArma.PARADO) return;
        long agora = jogador.level().getGameTime();
        if (arma.tubo) {
            if (e.municao().size() >= arma.capacidade || !item.temCartucho(jogador)) return;
            stack.set(Registros.ESTADO_ARMA.get(), e.comecar(EstadoArma.RECARGA, agora));
            return;
        }
        int precisa = arma.capacidade - e.municao().size();
        List<Integer> fila = new ArrayList<>();
        for (ItemStack pilha : item.pilhas(jogador)) {
            for (int k = 0; k < pilha.getCount() && fila.size() < precisa; k++) {
                fila.add(((CartuchoItem) pilha.getItem()).tipo.ordinal());
            }
        }
        boolean criativo = jogador.getAbilities().instabuild;
        if (criativo) {
            while (fila.size() < precisa) fila.add(TipoBala.COMUM.ordinal());   // no criativo nunca falta
        }
        Carregador.Plano plano = Carregador.planejar(e.municao(), fila.stream().mapToInt(Integer::intValue).toArray(),
                arma.capacidade);
        if (plano.usados() == 0) return;
        if (!criativo) {
            int gastar = plano.usados();
            for (ItemStack pilha : item.pilhas(jogador)) {
                if (gastar == 0) break;
                int tira = Math.min(gastar, pilha.getCount());
                pilha.shrink(tira);
                gastar -= tira;
            }
        }
        stack.set(Registros.ESTADO_ARMA.get(), e.comecarRecarga(agora, plano.municao()));
    }

    /** As pilhas de cartucho do calibre desta arma, como o arco procura flecha: outra mão, depois o inventário. */
    private List<ItemStack> pilhas(Player jogador) {
        List<ItemStack> todas = new ArrayList<>();
        todas.add(jogador.getOffhandItem());
        todas.addAll(jogador.getInventory().items);
        List<ItemStack> certas = new ArrayList<>();
        for (ItemStack p : todas) {
            if (p.getItem() instanceof CartuchoItem c && c.calibre == arma.calibre && !p.isEmpty()) certas.add(p);
        }
        return certas;
    }

    private boolean temCartucho(Player jogador) {
        return jogador.getAbilities().instabuild || !pilhas(jogador).isEmpty();
    }

    /** Escopeta: tira um cartucho do inventário (no criativo não gasta) e devolve o tipo, ou -1 se não tem. */
    private int tirarCartucho(Player jogador) {
        List<ItemStack> pilhas = pilhas(jogador);
        if (pilhas.isEmpty()) return jogador.getAbilities().instabuild ? TipoBala.COMUM.ordinal() : -1;
        ItemStack p = pilhas.get(0);
        int tipo = ((CartuchoItem) p.getItem()).tipo.ordinal();
        if (!jogador.getAbilities().instabuild) p.shrink(1);
        return tipo;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antigo, ItemStack novo, boolean trocouSlot) {
        // a cada tiro o item muda (o estado vai junto); sem isso a mão abaixaria e subiria a cada mudança
        return trocouSlot || !novo.is(antigo.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> linhas, TooltipFlag flag) {
        EstadoArma e = estado(stack);
        linhas.add(Component.translatable("item.revolver.arma.municao", e.municao().size(), arma.capacidade)
                .withStyle(ChatFormatting.GRAY));
        TipoBala[] tipos = TipoBala.values();
        for (int i = tipos.length - 1; i >= 0; i--) {     // quantas de cada tipo, da melhor para a pior
            TipoBala t = tipos[i];
            int n = Carregador.contar(e.municao(), t.ordinal());
            if (n > 0) {
                linhas.add(Component.translatable("item.revolver.revolver.tipo", n,
                                Component.translatable("item.revolver." + t.item(arma.calibre)),
                                Math.round(arma.dano(t) * arma.bagos()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
