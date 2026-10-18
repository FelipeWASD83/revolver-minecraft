package dev.revolver;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Bandoleira do Fim (2.3.0, pedido dele: "um item que dá munição infinita e é difícil de conseguir"): basta estar
 * no inventário para a recarga de qualquer arma não gastar cartucho ({@link Municao}). A receita leva o ovo do dragão,
 * que só existe um por mundo.
 */
public class BandoleiraItem extends Item {
    public BandoleiraItem(Properties propriedades) {
        super(propriedades);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> linhas, TooltipFlag flag) {
        linhas.add(Component.translatable("item.revolver.bandoleira_do_fim.descricao").withStyle(ChatFormatting.GRAY));
        linhas.add(Component.translatable("item.revolver.bandoleira_do_fim.tipo").withStyle(ChatFormatting.DARK_GRAY));
    }
}
