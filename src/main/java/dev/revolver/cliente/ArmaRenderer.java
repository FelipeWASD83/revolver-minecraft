package dev.revolver.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.revolver.ArmaItem;
import dev.revolver.EstadoArma;
import dev.revolver.logica.Animacao;
import dev.revolver.logica.Arma;
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

import java.util.List;

/**
 * Desenha uma das armas novas peça por peça, cada uma com a matriz do quadro atual da animação do Blender. As peças
 * especiais: o clarão e o estojo que voa só no tiro; o cartucho de cima do pente quando tem bala (na troca de pente,
 * o do pente novo depois do quadro da troca); e, na escopeta, o cartucho que entra no tubo durante a recarga.
 */
public class ArmaRenderer extends BlockEntityWithoutLevelRenderer {
    private final Arma arma;

    public ArmaRenderer(Arma arma) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.arma = arma;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext contexto, PoseStack pose, MultiBufferSource buffers,
                             int luz, int sobreposicao) {
        if (contexto == ItemDisplayContext.GUI) {      // no inventário, o ícone em pixel art
            RevolverCliente.desenharIcone(arma.id, stack, pose, buffers, luz, sobreposicao);
            return;
        }
        Animacao anim = RevolverCliente.animacaoArma(arma);
        if (anim == null) return;
        Minecraft mc = Minecraft.getInstance();
        EstadoArma e = ArmaItem.estado(stack);
        String nome = null;
        float indice = 0;
        if (e.acao() != EstadoArma.PARADO && mc.level != null) {
            double dt = mc.level.getGameTime() - e.inicio() + mc.getTimer().getGameTimeDeltaPartialTick(true);
            indice = Tempos.indice(Math.max(0, dt));
            nome = e.acao() == EstadoArma.TIRO ? "tiro" : arma.faixaRecarga();
        }
        VertexConsumer vc = buffers.getBuffer(Sheets.cutoutBlockSheet());
        desenhar(mc, "corpo", anim.matriz(nome, "corpo", indice), stack, pose, vc, luz, sobreposicao);
        for (String p : arma.pecas) desenhar(mc, p, anim.matriz(nome, p, indice), stack, pose, vc, luz, sobreposicao);

        if (e.acao() == EstadoArma.TIRO) {
            desenhar(mc, "clarao", anim.matriz(nome, "clarao", indice), stack, pose, vc, LightTexture.FULL_BRIGHT,
                    sobreposicao);
            desenhar(mc, "estojo", anim.matriz(nome, "ejetado", indice), stack, pose, vc, luz, sobreposicao);
        }
        if (arma.tubo) {
            if (e.acao() == EstadoArma.RECARGA) {
                desenhar(mc, "estojo", anim.matriz(nome, "carga", indice), stack, pose, vc, luz, sobreposicao);
            }
            return;
        }
        boolean novo = e.acao() == EstadoArma.RECARGA && indice + 1 >= arma.troca;
        List<Integer> municao = novo ? e.novos() : e.municao();
        if (!municao.isEmpty() && anim.repouso("topo_bala") != null) {
            desenhar(mc, "estojo", anim.matriz(nome, "topo_estojo", indice), stack, pose, vc, luz, sobreposicao);
            desenhar(mc, TipoBala.de(municao.get(0)).modelo, anim.matriz(nome, "topo_bala", indice), stack, pose, vc,
                    luz, sobreposicao);
        }
    }

    private void desenhar(Minecraft mc, String peca, float[] m, ItemStack stack, PoseStack pose, VertexConsumer vc,
                          int luz, int sobreposicao) {
        if (m == null || Animacao.escondida(m)) return;
        BakedModel modelo = mc.getModelManager().getModel(RevolverCliente.modelo(arma.id, peca));
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
