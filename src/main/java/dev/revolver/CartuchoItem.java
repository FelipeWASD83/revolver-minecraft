package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.TipoBala;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Um cartucho de um calibre (.45 do revólver ou .50 AE da deagle) e de um dos tipos; a descrição mostra o dano. */
public class CartuchoItem extends Item {
    public final Calibre calibre;
    public final TipoBala tipo;

    public CartuchoItem(Calibre calibre, TipoBala tipo, Properties propriedades) {
        super(propriedades);
        this.calibre = calibre;
        this.tipo = tipo;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> linhas, TooltipFlag flag) {
        MutableComponent texto = calibre.bagos > 1
                ? Component.translatable("item.revolver.cartucho.dano_bagos", textoDano(tipo.dano(calibre)), calibre.bagos)
                : Component.translatable("item.revolver.cartucho.dano", textoDano(tipo.dano(calibre)));
        linhas.add(texto.withStyle(ChatFormatting.GRAY));
        if (tipo.infinito()) {
            linhas.add(Component.translatable("item.revolver.cartucho.nunca_acaba").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (tipo.fogo() > 0) {
            linhas.add(Component.translatable("item.revolver.cartucho.fogo", Math.round(tipo.fogo()))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (tipo.explode()) {
            linhas.add(Component.translatable("item.revolver.cartucho.explode").withStyle(ChatFormatting.RED));
        }
    }

    /** O dano para a descrição: o número arredondado, ou "infinito". */
    public static Component textoDano(float dano) {
        return Float.isInfinite(dano) ? Component.translatable("item.revolver.cartucho.infinito")
                : Component.literal(Long.toString(Math.round((double) dano)));
    }
}
