package dev.revolver;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A tecla de recarregar foi apertada com uma arma na mão principal (ou na outra). Do cliente para o servidor. */
public record RecarregarMsg(boolean outraMao) implements CustomPacketPayload {
    public static final Type<RecarregarMsg> TIPO =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RevolverMod.ID, "recarregar"));
    public static final StreamCodec<ByteBuf, RecarregarMsg> CODEC =
            ByteBufCodecs.BOOL.map(RecarregarMsg::new, RecarregarMsg::outraMao);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TIPO;
    }

    public static void receber(RecarregarMsg msg, IPayloadContext contexto) {
        InteractionHand mao = msg.outraMao() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (contexto.player().getItemInHand(mao).getItem() instanceof DeagleItem) {
            DeagleItem.recarregar(contexto.player(), mao);
        } else if (contexto.player().getItemInHand(mao).getItem() instanceof ArmaItem) {
            ArmaItem.recarregar(contexto.player(), mao);
        } else {
            RevolverItem.recarregar(contexto.player(), mao);
        }
    }
}
