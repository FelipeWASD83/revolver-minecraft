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
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
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

/** Só no cliente: a tecla de recarregar, os modelos das peças, a animação e o renderizador do item. */
public final class RevolverCliente {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String[] PECAS = {"corpo", "cao", "gatilho", "tambor", "portinhola", "botao", "clarao", "estojo", "bala"};
    public static final KeyMapping TECLA_RECARREGAR = new KeyMapping("key.revolver.recarregar",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.revolver");
    private static Animacao animacao;

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

    public static ModelResourceLocation modelo(String peca) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RevolverMod.ID, "item/revolver/" + peca));
    }

    private static void teclas(RegisterKeyMappingsEvent evento) {
        evento.register(TECLA_RECARREGAR);
    }

    private static void modelos(ModelEvent.RegisterAdditional evento) {
        for (String p : PECAS) evento.register(modelo(p));
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
    }

    private static void recursos(RegisterClientReloadListenersEvent evento) {
        evento.registerReloadListener((ResourceManagerReloadListener) gerente -> {
            ResourceLocation arquivo = ResourceLocation.fromNamespaceAndPath(RevolverMod.ID, "animacoes/revolver.json");
            try (Reader leitor = gerente.openAsReader(arquivo)) {
                animacao = Animacao.ler(leitor);
            } catch (IOException | RuntimeException e) {
                LOG.error("Revólver: não consegui ler {}", arquivo, e);
                animacao = null;
            }
        });
    }

    private static void tick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        while (TECLA_RECARREGAR.consumeClick()) {
            if (mc.player == null) continue;
            if (mc.player.getMainHandItem().is(Registros.REVOLVER.get())) {
                PacketDistributor.sendToServer(new RecarregarMsg(false));
            } else if (mc.player.getOffhandItem().is(Registros.REVOLVER.get())) {
                PacketDistributor.sendToServer(new RecarregarMsg(true));
            }
        }
    }
}
