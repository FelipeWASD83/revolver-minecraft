package dev.revolver;

import dev.revolver.logica.TipoBala;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Um cartucho .45 de um dos tipos; a descrição mostra o dano. */
public class CartuchoItem extends Item {
    public final TipoBala tipo;

    public CartuchoItem(TipoBala tipo, Properties propriedades) {
        super(propriedades);
        this.tipo = tipo;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> linhas, TooltipFlag flag) {
        linhas.add(Component.translatable("item.revolver.cartucho.dano", (int) tipo.dano).withStyle(ChatFormatting.GRAY));
    }
}
