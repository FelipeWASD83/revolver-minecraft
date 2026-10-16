package dev.revolver;

import dev.revolver.logica.Calibre;
import dev.revolver.logica.TipoBala;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** Itens, os componentes de estado das armas e os sons. */
public final class Registros {
    public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(RevolverMod.ID);
    public static final DeferredRegister.DataComponents COMPONENTES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RevolverMod.ID);
    public static final DeferredRegister<SoundEvent> SONS = DeferredRegister.create(Registries.SOUND_EVENT, RevolverMod.ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EstadoRevolver>> ESTADO =
            COMPONENTES.registerComponentType("estado",
                    b -> b.persistent(EstadoRevolver.CODEC).networkSynchronized(EstadoRevolver.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EstadoDeagle>> ESTADO_DEAGLE =
            COMPONENTES.registerComponentType("estado_deagle",
                    b -> b.persistent(EstadoDeagle.CODEC).networkSynchronized(EstadoDeagle.STREAM_CODEC));

    public static final DeferredItem<RevolverItem> REVOLVER =
            ITENS.register("revolver", () -> new RevolverItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeagleItem> DEAGLE =
            ITENS.register("deagle", () -> new DeagleItem(new Item.Properties().stacksTo(1)));
    /** Os cartuchos .45, um por tipo, na ordem do {@link TipoBala}; o comum continua com o nome da 1.0 ("cartucho_45"). */
    public static final Map<TipoBala, DeferredItem<CartuchoItem>> CARTUCHOS = cartuchos(Calibre.C45);
    /** Os cartuchos .50 AE da deagle. */
    public static final Map<TipoBala, DeferredItem<CartuchoItem>> CARTUCHOS_50 = cartuchos(Calibre.C50);

    private static Map<TipoBala, DeferredItem<CartuchoItem>> cartuchos(Calibre calibre) {
        Map<TipoBala, DeferredItem<CartuchoItem>> mapa = new EnumMap<>(TipoBala.class);
        for (TipoBala tipo : TipoBala.values()) {
            Item.Properties p = new Item.Properties().stacksTo(64);
            if (tipo == TipoBala.NETHERITE) p = p.fireResistant();
            Item.Properties final_ = p;
            mapa.put(tipo, ITENS.register(tipo.item(calibre), () -> new CartuchoItem(calibre, tipo, final_)));
        }
        return mapa;
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_TIRO = som("tiro");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_ENGATILHAR = som("engatilhar");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_CLIQUE_SECO = som("clique_seco");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_PORTINHOLA = som("portinhola");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_ESTOJO_CAI = som("estojo_cai");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_BALA_ENTRA = som("bala_entra");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_TAMBOR_GIRA = som("tambor_gira");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_TIRO_DEAGLE = som("tiro_deagle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_FERROLHO = som("ferrolho");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_TRAVA = som("trava");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_PENTE_SAI = som("pente_sai");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOM_PENTE_ENTRA = som("pente_entra");

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
