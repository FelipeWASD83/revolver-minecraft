package dev.revolver.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.revolver.RecarregarMsg;
import dev.revolver.Registros;
import dev.revolver.RevolverMod;
import dev.revolver.logica.Animacao;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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

    private RevolverCliente() {}

    public static void iniciar(IEventBus barramento) {
        barramento.addListener(RevolverCliente::teclas);
        barramento.addListener(RevolverCliente::modelos);
        barramento.addListener(RevolverCliente::extensoes);
        barramento.addListener(RevolverCliente::recursos);
        NeoForge.EVENT_BUS.addListener(RevolverCliente::tick);
    }

    public static Animacao animacao() {
        return animacao;
    }

    public static Animacao animacaoDeagle() {
        return animacaoDeagle;
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
    }

    private static void recursos(RegisterClientReloadListenersEvent evento) {
        evento.registerReloadListener((ResourceManagerReloadListener) gerente -> {
            animacao = ler(gerente, "animacoes/revolver.json");
            animacaoDeagle = ler(gerente, "animacoes/deagle.json");
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

    private static boolean arma(ItemStack stack) {
        return stack.is(Registros.REVOLVER.get()) || stack.is(Registros.DEAGLE.get());
    }
}
