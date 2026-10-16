package dev.revolver;

import dev.revolver.cliente.RevolverCliente;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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
            evento.accept(Registros.REVOLVER.get());
            for (var cartucho : Registros.CARTUCHOS.values()) evento.accept(cartucho.get());
            evento.accept(Registros.DEAGLE.get());
            for (var cartucho : Registros.CARTUCHOS_50.values()) evento.accept(cartucho.get());
        }
    }

    private static void mensagens(RegisterPayloadHandlersEvent evento) {
        evento.registrar("1").playToServer(RecarregarMsg.TIPO, RecarregarMsg.CODEC, RecarregarMsg::receber);
    }
}
