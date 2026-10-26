package github.nyeptos.packingcube;

import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.data.ModDataGenerators;
import github.nyeptos.packingcube.event.ModGameEvents;
import github.nyeptos.packingcube.network.ModNetworking;
import github.nyeptos.packingcube.registry.ModRegistries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Mod bootstrap only. Registries, data generation and runtime behavior have separate owners. */
@Mod(Packingcube.MODID)
public final class Packingcube {
    public static final String MODID = "packingcube";

    public Packingcube(IEventBus modBus) {
        ModRegistries.register(modBus);
        BagEquipment.register(modBus);
        modBus.addListener(ModNetworking::register);
        modBus.addListener(ModDataGenerators::gather);
        ModGameEvents.register(NeoForge.EVENT_BUS);
    }
}
