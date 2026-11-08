package github.nyeptos.packingcube.registry;

import net.neoforged.bus.api.IEventBus;

/** Single entry point for deferred registries on the mod lifecycle bus. */
public final class ModRegistries {
    private ModRegistries() {}

    public static void register(IEventBus modBus) {
        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModRecipes.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        ModMenus.register(modBus);
    }
}
