package dev.revolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que fica guardado no revólver (componente de dados, salvo no mundo e mandado aos jogadores por perto, que
 * assim veem a mesma animação).
 *
 * @param camaras  estado das seis câmaras, 2 bits cada (ver {@link dev.revolver.logica.Tambor})
 * @param posicao  quanto o tambor já girou (0 a 5)
 * @param acao     o que está acontecendo agora ({@link #PARADO}, {@link #TIRO}, {@link #TIRO_SECO}, {@link #RECARGA})
 * @param inicio   tick do jogo em que a ação começou
 * @param carregar na recarga, as câmaras que recebem bala nova (bit por câmara)
 */
public record EstadoRevolver(int camaras, int posicao, int acao, long inicio, int carregar) {
    public static final int PARADO = 0;
    public static final int TIRO = 1;
    public static final int TIRO_SECO = 2;
    public static final int RECARGA = 3;

    public static final EstadoRevolver VAZIO = new EstadoRevolver(0, 0, PARADO, 0L, 0);

    public static final Codec<EstadoRevolver> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("camaras").forGetter(EstadoRevolver::camaras),
            Codec.INT.fieldOf("posicao").forGetter(EstadoRevolver::posicao),
            Codec.INT.fieldOf("acao").forGetter(EstadoRevolver::acao),
            Codec.LONG.fieldOf("inicio").forGetter(EstadoRevolver::inicio),
            Codec.INT.fieldOf("carregar").forGetter(EstadoRevolver::carregar)
    ).apply(i, EstadoRevolver::new));

    public static final StreamCodec<ByteBuf, EstadoRevolver> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EstadoRevolver::camaras,
            ByteBufCodecs.VAR_INT, EstadoRevolver::posicao,
            ByteBufCodecs.VAR_INT, EstadoRevolver::acao,
            ByteBufCodecs.VAR_LONG, EstadoRevolver::inicio,
            ByteBufCodecs.VAR_INT, EstadoRevolver::carregar,
            EstadoRevolver::new);

    public EstadoRevolver comecar(int novaAcao, long tick, int novoCarregar) {
        return new EstadoRevolver(camaras, posicao, novaAcao, tick, novoCarregar);
    }

    public EstadoRevolver comCamaras(int novas) {
        return new EstadoRevolver(novas, posicao, acao, inicio, carregar);
    }

    public EstadoRevolver parado(int novasCamaras, int novaPosicao) {
        return new EstadoRevolver(novasCamaras, Math.floorMod(novaPosicao, 6), PARADO, 0L, 0);
    }
}
