package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;

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
    public static final DeferredHolder<MenuType<?>, MenuType<BagRadialMenu>> BAG_RADIAL_MENU =
            MENUS.register(
                    "bag_radial",
                    () -> new MenuType<>(BagRadialMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
