package dev.revolver;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Itens, o componente de estado do revólver e os sons. */
public final class Registros {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(RevolverMod.ID);
    public static final DeferredRegister.DataComponents COMPONENTES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RevolverMod.ID);
    public static final DeferredRegister<SoundEvent> SONS = DeferredRegister.create(Registries.SOUND_EVENT, RevolverMod.ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EstadoRevolver>> ESTADO =
            COMPONENTES.registerComponentType("estado",
                    b -> b.persistent(EstadoRevolver.CODEC).networkSynchronized(EstadoRevolver.STREAM_CODEC));

    public static final DeferredItem<RevolverItem> REVOLVER =
            ITENS.register("revolver", () -> new RevolverItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CARTUCHO =
            ITENS.registerSimpleItem("cartucho_45", new Item.Properties().stacksTo(64));

    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_TIRO = som("tiro");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_ENGATILHAR = som("engatilhar");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_CLIQUE_SECO = som("clique_seco");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_PORTINHOLA = som("portinhola");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_ESTOJO_CAI = som("estojo_cai");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_BALA_ENTRA = som("bala_entra");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_TAMBOR_GIRA = som("tambor_gira");

    private Registros() {}

    private static DeferredHolder<SoundEvent, SoundEvent> som(String nome) {
        return SONS.register(nome, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(RevolverMod.ID, nome)));
    }

    static void registrar(IEventBus barramento) {
        COMPONENTES.register(barramento);
        ITENS.register(barramento);
        SONS.register(barramento);
    }
}
