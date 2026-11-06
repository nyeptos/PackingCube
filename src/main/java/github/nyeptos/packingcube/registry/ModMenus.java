package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.menu.CraftingStationMenu;
import github.nyeptos.packingcube.menu.station.AnvilStationMenu;
import github.nyeptos.packingcube.menu.station.BlastFurnaceStationMenu;
import github.nyeptos.packingcube.menu.station.BrewingStandStationMenu;
import github.nyeptos.packingcube.menu.station.FurnaceStationMenu;
import github.nyeptos.packingcube.menu.station.CartographyStationMenu;
import github.nyeptos.packingcube.menu.station.GrindstoneStationMenu;
import github.nyeptos.packingcube.menu.station.LoomStationMenu;
import github.nyeptos.packingcube.menu.station.SmokerStationMenu;
import github.nyeptos.packingcube.menu.station.SmithingStationMenu;
import github.nyeptos.packingcube.menu.station.StonecutterStationMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Packingcube.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BagMenu>> BAG_MENU =
            MENUS.register("bag", () -> IMenuTypeExtension.create(BagMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<CraftingStationMenu>>
            CRAFTING_STATION_MENU =
                    MENUS.register(
                            "crafting_station",
                            () -> IMenuTypeExtension.create(CraftingStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<StonecutterStationMenu>>
            STONECUTTER_STATION =
                    MENUS.register(
                            "stonecutter_station",
                            () -> IMenuTypeExtension.create(StonecutterStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<GrindstoneStationMenu>>
            GRINDSTONE_STATION =
                    MENUS.register(
                            "grindstone_station",
                            () -> IMenuTypeExtension.create(GrindstoneStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<CartographyStationMenu>>
            CARTOGRAPHY_STATION =
                    MENUS.register(
                            "cartography_station",
                            () -> IMenuTypeExtension.create(CartographyStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<LoomStationMenu>> LOOM_STATION =
            MENUS.register(
                    "loom_station",
                    () -> IMenuTypeExtension.create(LoomStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<SmithingStationMenu>>
            SMITHING_STATION =
                    MENUS.register(
                            "smithing_station",
                            () -> IMenuTypeExtension.create(SmithingStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<AnvilStationMenu>> ANVIL_STATION =
            MENUS.register(
                    "anvil_station",
                    () -> IMenuTypeExtension.create(AnvilStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<FurnaceStationMenu>>
            FURNACE_STATION =
                    MENUS.register(
                            "furnace_station",
                            () -> IMenuTypeExtension.create(FurnaceStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<SmokerStationMenu>> SMOKER_STATION =
            MENUS.register(
                    "smoker_station",
                    () -> IMenuTypeExtension.create(SmokerStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceStationMenu>>
            BLAST_FURNACE_STATION =
                    MENUS.register(
                            "blast_furnace_station",
                            () ->
                                    IMenuTypeExtension.create(
                                            BlastFurnaceStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<BrewingStandStationMenu>>
            BREWING_STAND_STATION =
                    MENUS.register(
                            "brewing_stand_station",
                            () ->
                                    IMenuTypeExtension.create(
                                            BrewingStandStationMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<BagRadialMenu>> BAG_RADIAL_MENU =
            MENUS.register(
                    "bag_radial",
                    () -> new MenuType<>(BagRadialMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
