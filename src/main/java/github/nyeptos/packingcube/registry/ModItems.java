package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.refill.RefillPouchSettings;
import github.nyeptos.packingcube.item.BagItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PackingCube.MODID);

    public static final DeferredItem<BagItem> BACKPACK =
            ITEMS.register("backpack", () -> new BagItem(true, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BagItem> STORAGE_POUCH =
            ITEMS.register(
                    "storage_pouch", () -> new BagItem(false, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BagItem> BULK_POUCH =
            ITEMS.register("bulk_pouch", () -> new BagItem(false, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BagItem> REFILL_POUCH =
            ITEMS.register(
                    "refill_pouch",
                    () ->
                            new BagItem(
                                    false,
                                    new Item.Properties()
                                            .stacksTo(1)
                                            .component(
                                                    DataComponents.CUSTOM_DATA,
                                                    RefillPouchSettings.defaults())));

    /**
     * The pouch was registered as {@code packingcube:filter_bag} before PC-58. The alias keeps
     * saved inventories, bag contents and datapack references resolving to the renamed item.
     */
    private static final ResourceLocation LEGACY_STORAGE_POUCH =
            ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, "filter_bag");

    /**
     * The backpack was registered as {@code packingcube:large_bag} before PC-135. The alias keeps
     * saved inventories, bag contents and datapack references resolving to the renamed item.
     */
    private static final ResourceLocation LEGACY_BACKPACK =
            ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, "large_bag");

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.addAlias(
                LEGACY_STORAGE_POUCH,
                ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, "storage_pouch"));
        ITEMS.addAlias(
                LEGACY_BACKPACK,
                ResourceLocation.fromNamespaceAndPath(PackingCube.MODID, "backpack"));
        ITEMS.register(bus);
    }
}
