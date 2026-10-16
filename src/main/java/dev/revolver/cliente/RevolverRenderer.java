package dev.revolver.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.revolver.EstadoRevolver;
import dev.revolver.RevolverItem;
import dev.revolver.logica.Animacao;
import dev.revolver.logica.Tambor;
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
 * Desenha o revólver peça por peça, cada uma com a matriz do quadro atual da animação (a do Blender). Parado,
 * usa o repouso; as balas aparecem conforme o estado das câmaras guardado no item.
 */
public class RevolverRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String[] MOVEIS = {"cao", "gatilho", "tambor", "portinhola", "botao"};

    public RevolverRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext contexto, PoseStack pose, MultiBufferSource buffers,
                             int luz, int sobreposicao) {
        if (contexto == ItemDisplayContext.GUI) {      // no inventário, o ícone em pixel art
            RevolverCliente.desenharIcone("revolver", stack, pose, buffers, luz, sobreposicao);
            return;
        }
        Animacao anim = RevolverCliente.animacao();
        if (anim == null) return;
        Minecraft mc = Minecraft.getInstance();
        EstadoRevolver e = RevolverItem.estado(stack);
        String nome = null;
        float indice = 0;
        if (e.acao() != EstadoRevolver.PARADO && mc.level != null) {
            double dt = mc.level.getGameTime() - e.inicio() + mc.getTimer().getGameTimeDeltaPartialTick(true);
            indice = Tempos.indice(Math.max(0, dt));
            nome = e.acao() == EstadoRevolver.RECARGA ? "recarregar" : "tiro";
        }
        // tiro sem bala: o cão cai, mas não tem coice nem clarão
        boolean seco = e.acao() == EstadoRevolver.TIRO_SECO;
        float[] corpo = anim.matriz(nome, "corpo", indice);
        VertexConsumer vc = buffers.getBuffer(Sheets.cutoutBlockSheet());

        desenhar(mc, "corpo", seco ? anim.repouso("corpo") : corpo, stack, pose, vc, luz, sobreposicao);
        for (String p : MOVEIS) {
            desenhar(mc, p, ajustar(anim.matriz(nome, p, indice), corpo, seco), stack, pose, vc, luz, sobreposicao);
        }
        if (e.acao() == EstadoRevolver.TIRO) {
            desenhar(mc, "clarao", anim.matriz(nome, "clarao", indice), stack, pose, vc, LightTexture.FULL_BRIGHT,
                    sobreposicao);
        }
        for (int j = 0; j < 6; j++) {
            Tambor.Visual v = e.acao() == EstadoRevolver.RECARGA
                    ? Tambor.visualNaRecarga(e.camaras(), e.carregar(), e.posicao(), j, indice + 1)
                    : Tambor.visualParado(e.camaras(), e.posicao(), j);
            if (v == Tambor.Visual.NADA) continue;
            float[] estojo = ajustar(anim.matriz(nome, "estojo" + j, indice), corpo, seco);
            desenhar(mc, "estojo", estojo, stack, pose, vc, luz, sobreposicao);
            // a ponta tem a cor do tipo: a bala que estava na câmara, ou a nova que entra na recarga
            int c = Tambor.camaraDoEncaixe(j, e.posicao());
            boolean nova = e.acao() == EstadoRevolver.RECARGA && (e.carregar() & (1 << c)) != 0
                    && v == Tambor.Visual.ESTOJO_E_BALA;
            String bala = TipoBala.de(Tambor.tipo(nova ? e.tiposNovos() : e.tipos(), c)).modelo;
            if (v == Tambor.Visual.ESTOJO_E_BALA_JUNTOS) {
                desenhar(mc, bala, estojo, stack, pose, vc, luz, sobreposicao);
            } else if (v == Tambor.Visual.ESTOJO_E_BALA) {
                desenhar(mc, bala, ajustar(anim.matriz(nome, "bala" + j, indice), corpo, seco), stack, pose, vc,
                        luz, sobreposicao);
            }
        }
    }

    private static float[] ajustar(float[] m, float[] corpo, boolean seco) {
        return seco ? Animacao.semCoice(corpo, m) : m;
    }

    private static void desenhar(Minecraft mc, String peca, float[] m, ItemStack stack, PoseStack pose,
                                 VertexConsumer vc, int luz, int sobreposicao) {
        if (m == null || Animacao.escondida(m)) return;
        BakedModel modelo = mc.getModelManager().getModel(RevolverCliente.modelo(peca));
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
