package dev.revolver.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.revolver.DeagleItem;
import dev.revolver.EstadoDeagle;
import dev.revolver.logica.Animacao;
import dev.revolver.logica.Pente;
import dev.revolver.logica.TemposDeagle;
import dev.revolver.logica.Tempos;
import dev.revolver.logica.TipoBala;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Desenha a Desert Eagle peça por peça, cada uma com a matriz do quadro atual da animação (a do Blender). Parada com a
 * câmara vazia, ela fica aberta: é o primeiro quadro de "recarregar_vazia" (ferrolho atrás, trava erguida).
 */
public class DeagleRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String[] MOVEIS = {"ferrolho", "cao", "gatilho", "trava", "botao", "pente"};

    public DeagleRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static String animacaoDe(int acao) {
        return switch (acao) {
            case EstadoDeagle.TIRO -> "tiro";
            case EstadoDeagle.TIRO_ULTIMO -> "tiro_ultimo";
            case EstadoDeagle.SECO -> "seco";
            case EstadoDeagle.RECARGA -> "recarregar";
            case EstadoDeagle.RECARGA_VAZIA -> "recarregar_vazia";
            default -> null;
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext contexto, PoseStack pose, MultiBufferSource buffers,
                             int luz, int sobreposicao) {
        Animacao anim = RevolverCliente.animacaoDeagle();
        if (anim == null) return;
        Minecraft mc = Minecraft.getInstance();
        EstadoDeagle e = DeagleItem.estado(stack);
        String nome = null;
        float indice = 0;
        if (e.acao() != EstadoDeagle.PARADO && mc.level != null) {
            double dt = mc.level.getGameTime() - e.inicio() + mc.getTimer().getGameTimeDeltaPartialTick(true);
            indice = Tempos.indice(Math.max(0, dt));
            nome = animacaoDe(e.acao());
        } else if (e.camara() == 0) {
            nome = "recarregar_vazia";          // aberta: o primeiro quadro tem o ferrolho atrás e a trava erguida
        }
        VertexConsumer vc = buffers.getBuffer(Sheets.cutoutBlockSheet());
        desenhar(mc, "corpo", anim.matriz(nome, "corpo", indice), stack, pose, vc, luz, sobreposicao);
        for (String p : MOVEIS) desenhar(mc, p, anim.matriz(nome, p, indice), stack, pose, vc, luz, sobreposicao);

        boolean tiro = e.acao() == EstadoDeagle.TIRO || e.acao() == EstadoDeagle.TIRO_ULTIMO;
        if (tiro) {
            desenhar(mc, "clarao", anim.matriz(nome, "clarao", indice), stack, pose, vc, LightTexture.FULL_BRIGHT,
                    sobreposicao);
            desenhar(mc, "estojo", anim.matriz(nome, "ejetado", indice), stack, pose, vc, luz, sobreposicao);
        }
        // a bala de cima do pente: até a troca, a do pente velho; depois, a do novo
        boolean recarga = e.acao() == EstadoDeagle.RECARGA || e.acao() == EstadoDeagle.RECARGA_VAZIA;
        boolean novo = recarga && indice + 1 >= TemposDeagle.REC_TROCA;
        int balas = novo ? e.novoPente() : e.pente();
        if (balas > 0) {
            String bala = TipoBala.de(Pente.tipo(novo ? e.novosTipos() : e.tipos(), 0)).modelo;
            desenhar(mc, "estojo", anim.matriz(nome, "topo_estojo", indice), stack, pose, vc, luz, sobreposicao);
            desenhar(mc, bala, anim.matriz(nome, "topo_bala", indice), stack, pose, vc, luz, sobreposicao);
        }
    }

    private static void desenhar(Minecraft mc, String peca, float[] m, ItemStack stack, PoseStack pose,
                                 VertexConsumer vc, int luz, int sobreposicao) {
        if (m == null || Animacao.escondida(m)) return;
        BakedModel modelo = mc.getModelManager().getModel(RevolverCliente.modelo("deagle", peca));
        pose.pushPose();
        pose.mulPose(new Matrix4f(
                m[0], m[4], m[8], 0f,
                m[1], m[5], m[9], 0f,
                m[2], m[6], m[10], 0f,
                m[3], m[7], m[11], 1f));
        mc.getItemRenderer().renderModelLists(modelo, stack, luz, sobreposicao, pose, vc);
        pose.popPose();
    }
}
