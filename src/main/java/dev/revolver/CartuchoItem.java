package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.TipoBala;
import net.minecraft.ChatFormatting;
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
        linhas.add(Component.translatable("item.revolver.cartucho.dano", (int) tipo.dano(calibre))
                .withStyle(ChatFormatting.GRAY));
    }
}
