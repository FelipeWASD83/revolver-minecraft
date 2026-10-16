package dev.revolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * O que fica guardado nas armas novas (fuzis, metralhadora, escopeta, sniper): componente de dados salvo no mundo e
 * mandado aos jogadores por perto, que assim veem a mesma animação.
 *
 * @param municao o tipo de cada bala ({@link dev.revolver.logica.TipoBala}), a próxima no lugar 0
 * @param acao    o que está acontecendo ({@link #PARADO}, {@link #TIRO}, {@link #RECARGA})
 * @param inicio  tick do jogo em que a ação começou
 * @param novos   na recarga de pente, a munição do pente novo (entra no fim da animação)
 */
public record EstadoArma(List<Integer> municao, int acao, long inicio, List<Integer> novos) {
    public static final int PARADO = 0;
    public static final int TIRO = 1;
    public static final int RECARGA = 2;

    public static final EstadoArma VAZIO = new EstadoArma(List.of(), PARADO, 0L, List.of());

    public EstadoArma {
        municao = List.copyOf(municao);
        novos = List.copyOf(novos);
    }

    public static final Codec<EstadoArma> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().fieldOf("municao").forGetter(EstadoArma::municao),
            Codec.INT.fieldOf("acao").forGetter(EstadoArma::acao),
            Codec.LONG.fieldOf("inicio").forGetter(EstadoArma::inicio),
            Codec.INT.listOf().optionalFieldOf("novos", List.of()).forGetter(EstadoArma::novos)
    ).apply(i, EstadoArma::new));

    public static final StreamCodec<ByteBuf, EstadoArma> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), EstadoArma::municao,
            ByteBufCodecs.VAR_INT, EstadoArma::acao,
            ByteBufCodecs.VAR_LONG, EstadoArma::inicio,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), EstadoArma::novos,
            EstadoArma::new);

    public EstadoArma comecar(int novaAcao, long tick) {
        return new EstadoArma(municao, novaAcao, tick, List.of());
    }

    public EstadoArma comecarRecarga(long tick, List<Integer> pente) {
        return new EstadoArma(municao, RECARGA, tick, pente);
    }

    public EstadoArma comMunicao(List<Integer> nova) {
        return new EstadoArma(nova, acao, inicio, novos);
    }

    public EstadoArma parado() {
        return new EstadoArma(municao, PARADO, 0L, List.of());
    }
}
