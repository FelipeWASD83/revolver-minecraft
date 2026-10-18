package dev.revolver;

import dev.revolver.cliente.RevolverCliente;
import dev.revolver.logica.Calibre;
import dev.revolver.logica.TipoBala;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Mod(RevolverMod.ID)
public final class RevolverMod {
    public static final String ID = "revolver";

    public RevolverMod(IEventBus barramento, Dist lado) {
        Registros.registrar(barramento);
        barramento.addListener(RevolverMod::abaCriativo);
        barramento.addListener(RevolverMod::mensagens);
        if (lado.isClient()) RevolverCliente.iniciar(barramento);
    }

    private static void abaCriativo(BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == CreativeModeTabs.COMBAT) {
            evento.accept(Registros.BANDOLEIRA.get());
            evento.accept(Registros.REVOLVER.get());
            cartuchos(evento, Registros.CARTUCHOS);
            evento.accept(Registros.DEAGLE.get());
            cartuchos(evento, Registros.CARTUCHOS_50);
            // cada arma seguida dos cartuchos dela; calibre dividido (o 5,56 das duas M4) entra uma vez só, senão o
            // jogo fecha ao abrir o inventário do criativo ("already exists in the tab's list")
            Set<Calibre> postos = EnumSet.noneOf(Calibre.class);
            for (var arma : Registros.ARMAS.entrySet()) {
                evento.accept(arma.getValue().get());
                if (!postos.add(arma.getKey().calibre)) continue;
                cartuchos(evento, Registros.CARTUCHOS_DAS_ARMAS.get(arma.getKey().calibre));
            }
        }
    }

    /** Os cartuchos de um calibre; os de material de outro mod só aparecem com o mod instalado. */
    private static void cartuchos(BuildCreativeModeTabContentsEvent evento,
                                  Map<TipoBala, DeferredItem<CartuchoItem>> mapa) {
        for (var e : mapa.entrySet()) {
            if (e.getKey().mod == null || ModList.get().isLoaded(e.getKey().mod)) evento.accept(e.getValue().get());
        }
    }

    private static void mensagens(RegisterPayloadHandlersEvent evento) {
        evento.registrar("1").playToServer(RecarregarMsg.TIPO, RecarregarMsg.CODEC, RecarregarMsg::receber);
    }
}
