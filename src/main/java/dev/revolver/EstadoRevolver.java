package dev.revolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.revolver.logica.Tambor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que fica guardado no revólver (componente de dados, salvo no mundo e mandado aos jogadores por perto, que
 * assim veem a mesma animação).
 *
 * @param camaras    estado das seis câmaras, 2 bits cada (ver {@link dev.revolver.logica.Tambor})
 * @param posicao    quanto o tambor já girou (0 a 5)
 * @param acao       o que está acontecendo agora ({@link #PARADO}, {@link #TIRO}, {@link #TIRO_SECO}, {@link #RECARGA})
 * @param inicio     tick do jogo em que a ação começou
 * @param carregar   na recarga, as câmaras que recebem bala nova (bit por câmara)
 * @param tipos      tipo da bala de cada câmara ({@link dev.revolver.logica.TipoBala}), 4 bits cada
 * @param tiposNovos na recarga, o tipo de cada bala nova, 4 bits por câmara
 */
public record EstadoRevolver(int camaras, int posicao, int acao, long inicio, int carregar, int tipos,
                             int tiposNovos) {
    public static final int PARADO = 0;
    public static final int TIRO = 1;
    public static final int TIRO_SECO = 2;
    public static final int RECARGA = 3;

    public static final EstadoRevolver VAZIO = new EstadoRevolver(0, 0, PARADO, 0L, 0, 0, 0);

    // "tipos" e "tipos_novos" são opcionais: revólver salvo na 1.0.x não tem e fica com tudo comum (0).
    // "formato_tipos" diz quantos bits cada câmara usa: sem ele é o formato até a 2.2.0 (2 bits), convertido aqui
    // para o de agora (4 bits, que cabe os 11 tipos); o que se grava daqui em diante leva formato 2.
    public static final Codec<EstadoRevolver> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("camaras").forGetter(EstadoRevolver::camaras),
            Codec.INT.fieldOf("posicao").forGetter(EstadoRevolver::posicao),
            Codec.INT.fieldOf("acao").forGetter(EstadoRevolver::acao),
            Codec.LONG.fieldOf("inicio").forGetter(EstadoRevolver::inicio),
            Codec.INT.fieldOf("carregar").forGetter(EstadoRevolver::carregar),
            Codec.INT.optionalFieldOf("tipos", 0).forGetter(EstadoRevolver::tipos),
            Codec.INT.optionalFieldOf("tipos_novos", 0).forGetter(EstadoRevolver::tiposNovos),
            Codec.INT.optionalFieldOf("formato_tipos", 1).forGetter(e -> 2)
    ).apply(i, (camaras, posicao, acao, inicio, carregar, tipos, tiposNovos, formato) -> formato >= 2
            ? new EstadoRevolver(camaras, posicao, acao, inicio, carregar, tipos, tiposNovos)
            : new EstadoRevolver(camaras, posicao, acao, inicio, carregar, Tambor.de2Para4Bits(tipos, 6),
                    Tambor.de2Para4Bits(tiposNovos, 6))));

    public static final StreamCodec<ByteBuf, EstadoRevolver> STREAM_CODEC = StreamCodec.of(
            (buf, e) -> {
                VarInt.write(buf, e.camaras);
                VarInt.write(buf, e.posicao);
                VarInt.write(buf, e.acao);
                VarLong.write(buf, e.inicio);
                VarInt.write(buf, e.carregar);
                VarInt.write(buf, e.tipos);
                VarInt.write(buf, e.tiposNovos);
            },
            buf -> new EstadoRevolver(VarInt.read(buf), VarInt.read(buf), VarInt.read(buf), VarLong.read(buf),
                    VarInt.read(buf), VarInt.read(buf), VarInt.read(buf)));

    public EstadoRevolver comecarTiro(int novaAcao, long tick) {
        return new EstadoRevolver(camaras, posicao, novaAcao, tick, 0, tipos, 0);
    }

    public EstadoRevolver comecarRecarga(long tick, int novoCarregar, int novosTipos) {
        return new EstadoRevolver(camaras, posicao, RECARGA, tick, novoCarregar, tipos, novosTipos);
    }

    public EstadoRevolver comCamaras(int novas) {
        return new EstadoRevolver(novas, posicao, acao, inicio, carregar, tipos, tiposNovos);
    }

    public EstadoRevolver parado(int novasCamaras, int novaPosicao, int novosTipos) {
        return new EstadoRevolver(novasCamaras, Math.floorMod(novaPosicao, 6), PARADO, 0L, 0, novosTipos, 0);
    }
}
