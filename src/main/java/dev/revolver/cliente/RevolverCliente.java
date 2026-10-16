package dev.revolver.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.revolver.ArmaItem;
import dev.revolver.RecarregarMsg;
import dev.revolver.Registros;
import dev.revolver.RevolverMod;
import dev.revolver.logica.Animacao;
import dev.revolver.logica.Arma;
import dev.revolver.logica.TipoBala;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Só no cliente: a tecla de recarregar, os modelos das peças, as animações e os renderizadores das duas armas. */
public final class RevolverCliente {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String[] PECAS = {"corpo", "cao", "gatilho", "tambor", "portinhola", "botao", "clarao", "estojo",
            "bala_comum", "bala_aco", "bala_diamante", "bala_netherite"};
    public static final String[] PECAS_DEAGLE = {"corpo", "ferrolho", "cao", "gatilho", "trava", "botao", "pente",
            "clarao", "estojo", "bala_comum", "bala_aco", "bala_diamante", "bala_netherite"};
    public static final KeyMapping TECLA_RECARREGAR = new KeyMapping("key.revolver.recarregar",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.revolver");
    private static Animacao animacao;
    private static Animacao animacaoDeagle;
    private static final Map<Arma, Animacao> ANIMACOES_ARMAS = new EnumMap<>(Arma.class);

    private RevolverCliente() {}

    public static void iniciar(IEventBus barramento) {
        barramento.addListener(RevolverCliente::teclas);
        barramento.addListener(RevolverCliente::modelos);
        barramento.addListener(RevolverCliente::extensoes);
        barramento.addListener(RevolverCliente::recursos);
        NeoForge.EVENT_BUS.addListener(RevolverCliente::tick);
        NeoForge.EVENT_BUS.addListener(RevolverCliente::zoom);
        NeoForge.EVENT_BUS.addListener(RevolverCliente::luneta);
        NeoForge.EVENT_BUS.addListener(RevolverCliente::semMira);
        NeoForge.EVENT_BUS.addListener(RevolverCliente::semMao);
    }

    public static Animacao animacao() {
        return animacao;
    }

    public static Animacao animacaoDeagle() {
        return animacaoDeagle;
    }

    public static Animacao animacaoArma(Arma arma) {
        return ANIMACOES_ARMAS.get(arma);
    }

    /** Os modelos de uma arma nova: o corpo, as peças que mexem e as especiais. */
    public static List<String> pecasDoModelo(Arma arma) {
        List<String> pecas = new ArrayList<>();
        pecas.add("corpo");
        pecas.addAll(arma.pecas);
        pecas.add("clarao");
        pecas.add("estojo");
        if (!arma.tubo) {
            for (TipoBala t : TipoBala.values()) pecas.add(t.modelo);
        }
        return pecas;
    }

    public static ModelResourceLocation modelo(String peca) {
        return modelo("revolver", peca);
    }

    public static ModelResourceLocation modelo(String arma, String peca) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RevolverMod.ID,
                "item/" + arma + "/" + peca));
    }

    private static void teclas(RegisterKeyMappingsEvent evento) {
        evento.register(TECLA_RECARREGAR);
    }

    private static void modelos(ModelEvent.RegisterAdditional evento) {
        for (String p : PECAS) evento.register(modelo(p));
        for (String p : PECAS_DEAGLE) evento.register(modelo("deagle", p));
        for (Arma arma : Arma.values()) {
            for (String p : pecasDoModelo(arma)) evento.register(modelo(arma.id, p));
        }
    }

    private static void extensoes(RegisterClientExtensionsEvent evento) {
        evento.registerItem(new IClientItemExtensions() {
            private RevolverRenderer renderizador;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderizador == null) renderizador = new RevolverRenderer();
                return renderizador;
            }
        }, Registros.REVOLVER.get());
        evento.registerItem(new IClientItemExtensions() {
            private DeagleRenderer renderizador;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderizador == null) renderizador = new DeagleRenderer();
                return renderizador;
            }
        }, Registros.DEAGLE.get());
        for (Arma arma : Arma.values()) {
            evento.registerItem(new IClientItemExtensions() {
                private ArmaRenderer renderizador;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderizador == null) renderizador = new ArmaRenderer(arma);
                    return renderizador;
                }
            }, Registros.ARMAS.get(arma).get());
        }
    }

    private static void recursos(RegisterClientReloadListenersEvent evento) {
        evento.registerReloadListener((ResourceManagerReloadListener) gerente -> {
            animacao = ler(gerente, "animacoes/revolver.json");
            animacaoDeagle = ler(gerente, "animacoes/deagle.json");
            ANIMACOES_ARMAS.clear();
            for (Arma arma : Arma.values()) {
                Animacao a = ler(gerente, "animacoes/" + arma.id + ".json");
                if (a != null) ANIMACOES_ARMAS.put(arma, a);
            }
        });
    }

    private static Animacao ler(ResourceManager gerente, String caminho) {
        ResourceLocation arquivo = ResourceLocation.fromNamespaceAndPath(RevolverMod.ID, caminho);
        try (Reader leitor = gerente.openAsReader(arquivo)) {
            return Animacao.ler(leitor);
        } catch (IOException | RuntimeException e) {
            LOG.error("Revólver: não consegui ler {}", arquivo, e);
            return null;
        }
    }

    private static void tick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        while (TECLA_RECARREGAR.consumeClick()) {
            if (mc.player == null) continue;
            if (arma(mc.player.getMainHandItem())) {
                PacketDistributor.sendToServer(new RecarregarMsg(false));
            } else if (arma(mc.player.getOffhandItem())) {
                PacketDistributor.sendToServer(new RecarregarMsg(true));
            }
        }
    }

    /** A luneta: agachado com a arma na mão principal, o campo de visão fecha no zoom dela. */
    private static void zoom(ComputeFovModifierEvent evento) {
        Player jogador = evento.getPlayer();
        if (jogador.isCrouching() && jogador.getMainHandItem().getItem() instanceof ArmaItem item
                && item.arma.zoom < 1f) {
            evento.setNewFovModifier(evento.getNewFovModifier() * item.arma.zoom);
        }
    }

    private static final ResourceLocation LUNETA = ResourceLocation.fromNamespaceAndPath(RevolverMod.ID,
            "textures/gui/luneta.png");

    /** Olhando pela luneta: primeira pessoa, agachado, com uma arma de luneta na mão principal. */
    private static boolean mirando() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.player.isCrouching()
                && mc.player.getMainHandItem().getItem() instanceof ArmaItem item && item.arma.zoom < 1f;
    }

    /** A imagem da luneta no meio da tela, com preto dos lados. */
    private static void luneta(RenderGuiEvent.Post evento) {
        if (!mirando()) return;
        GuiGraphics g = evento.getGuiGraphics();
        int largura = g.guiWidth(), altura = g.guiHeight();
        int x = (largura - altura) / 2;
        RenderSystem.enableBlend();
        g.blit(LUNETA, x, 0, altura, altura, 0f, 0f, 512, 512, 512, 512);
        g.fill(0, 0, x, altura, 0xFF000000);
        g.fill(x + altura, 0, largura, altura, 0xFF000000);
        RenderSystem.disableBlend();
    }

    /** Pela luneta a mira do jogo some: quem mira é a cruz dela. */
    private static void semMira(RenderGuiLayerEvent.Pre evento) {
        if (evento.getName().equals(VanillaGuiLayers.CROSSHAIR) && mirando()) evento.setCanceled(true);
    }

    /** Pela luneta a arma na mão some, como no CS. */
    private static void semMao(RenderHandEvent evento) {
        if (mirando()) evento.setCanceled(true);
    }

    private static boolean arma(ItemStack stack) {
        return stack.is(Registros.REVOLVER.get()) || stack.is(Registros.DEAGLE.get())
                || stack.getItem() instanceof ArmaItem;
    }
}
