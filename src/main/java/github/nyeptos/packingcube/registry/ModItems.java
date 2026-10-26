package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.item.BagItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Packingcube.MODID);

    public static final DeferredItem<BagItem> LARGE_BAG =
            ITEMS.register("large_bag", () -> new BagItem(true, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BagItem> FILTER_BAG =
            ITEMS.register(
                    "filter_bag", () -> new BagItem(false, new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
