package dev.revolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.revolver.logica.Tambor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que fica guardado na Desert Eagle (componente de dados, salvo no mundo e mandado aos jogadores por perto).
 *
 * @param pente      quantas balas há no pente (0 a 7)
 * @param tipos      tipo de cada bala do pente ({@link dev.revolver.logica.TipoBala}), 4 bits cada, a de cima no lugar 0
 * @param camara     1 se tem bala na câmara; 0 é o ferrolho aberto
 * @param tipoCamara tipo da bala da câmara
 * @param acao       o que está acontecendo agora ({@link #PARADO}, {@link #TIRO}...)
 * @param inicio     tick do jogo em que a ação começou
 * @param novoPente  na recarga, quantas balas o pente novo tem
 * @param novosTipos na recarga, os tipos do pente novo
 */
public record EstadoDeagle(int pente, int tipos, int camara, int tipoCamara, int acao, long inicio, int novoPente,
                           int novosTipos) {
    public static final int PARADO = 0;
    public static final int TIRO = 1;
    /** O tiro que gasta a última bala: o ferrolho fica aberto. */
    public static final int TIRO_ULTIMO = 2;
    /** Gatilho com o ferrolho aberto: só o clique. */
    public static final int SECO = 3;
    /** Troca de pente com bala na câmara. */
    public static final int RECARGA = 4;
    /** Troca de pente com o ferrolho aberto: no fim ele fecha e põe bala na câmara. */
    public static final int RECARGA_VAZIA = 5;

    public static final EstadoDeagle VAZIO = new EstadoDeagle(0, 0, 0, 0, PARADO, 0L, 0, 0);

    public static final Codec<EstadoDeagle> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("pente").forGetter(EstadoDeagle::pente),
            Codec.INT.fieldOf("tipos").forGetter(EstadoDeagle::tipos),
            Codec.INT.fieldOf("camara").forGetter(EstadoDeagle::camara),
            Codec.INT.fieldOf("tipo_camara").forGetter(EstadoDeagle::tipoCamara),
            Codec.INT.fieldOf("acao").forGetter(EstadoDeagle::acao),
            Codec.LONG.fieldOf("inicio").forGetter(EstadoDeagle::inicio),
            Codec.INT.fieldOf("novo_pente").forGetter(EstadoDeagle::novoPente),
            Codec.INT.fieldOf("novos_tipos").forGetter(EstadoDeagle::novosTipos),
            // sem "formato_tipos" é o formato até a 2.2.0 (2 bits por bala do pente): convertido para 4 bits
            Codec.INT.optionalFieldOf("formato_tipos", 1).forGetter(e -> 2)
    ).apply(i, (pente, tipos, camara, tipoCamara, acao, inicio, novoPente, novosTipos, formato) -> formato >= 2
            ? new EstadoDeagle(pente, tipos, camara, tipoCamara, acao, inicio, novoPente, novosTipos)
            : new EstadoDeagle(pente, Tambor.de2Para4Bits(tipos, 7), camara, tipoCamara, acao, inicio, novoPente,
                    Tambor.de2Para4Bits(novosTipos, 7))));

    public static final StreamCodec<ByteBuf, EstadoDeagle> STREAM_CODEC = StreamCodec.of(
            (buf, e) -> {
                VarInt.write(buf, e.pente);
                VarInt.write(buf, e.tipos);
                VarInt.write(buf, e.camara);
                VarInt.write(buf, e.tipoCamara);
                VarInt.write(buf, e.acao);
                VarLong.write(buf, e.inicio);
                VarInt.write(buf, e.novoPente);
                VarInt.write(buf, e.novosTipos);
            },
            buf -> new EstadoDeagle(VarInt.read(buf), VarInt.read(buf), VarInt.read(buf), VarInt.read(buf),
                    VarInt.read(buf), VarLong.read(buf), VarInt.read(buf), VarInt.read(buf)));

    public EstadoDeagle comecar(int novaAcao, long tick) {
        return new EstadoDeagle(pente, tipos, camara, tipoCamara, novaAcao, tick, 0, 0);
    }

    public EstadoDeagle comecarRecarga(long tick, int pente2, int tipos2) {
        return new EstadoDeagle(pente, tipos, camara, tipoCamara, camara == 1 ? RECARGA : RECARGA_VAZIA, tick, pente2,
                tipos2);
    }

    /** Mesma ação, com a carga trocada (o tiro já saiu, a próxima bala subiu). */
    public EstadoDeagle comCarga(int pente2, int tipos2, int camara2, int tipoCamara2) {
        return new EstadoDeagle(pente2, tipos2, camara2, tipoCamara2, acao, inicio, novoPente, novosTipos);
    }

    public EstadoDeagle parado() {
        return new EstadoDeagle(pente, tipos, camara, tipoCamara, PARADO, 0L, 0, 0);
    }
}
