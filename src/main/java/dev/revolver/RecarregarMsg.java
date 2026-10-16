package dev.revolver;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A tecla de recarregar foi apertada com o revólver na mão principal (ou na outra). Do cliente para o servidor. */
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
        RevolverItem.recarregar(contexto.player(), msg.outraMao() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }
}
