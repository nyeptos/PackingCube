package github.nyeptos.packingcube.client;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.client.compat.CuriosClientCompat;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
        modid = Packingcube.MODID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void setupCuriosRenderer(FMLClientSetupEvent event) {
        if (BagEquipment.available()) event.enqueueWork(CuriosClientCompat::registerRenderer);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        if (BagEquipment.available()) CuriosClientCompat.registerModels(event);
    }

    @SubscribeEvent
    public static void registerKeys(
            net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(BagKeyHandler.OPEN_RADIAL);
        event.register(BagKeyHandler.OPEN_BIG_BAG);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BAG_MENU.get(), BagScreen::new);
        event.register(ModMenus.BAG_RADIAL_MENU.get(), BagRadialScreen::new);
        event.register(ModMenus.CRAFTING_STATION_MENU.get(), CraftingStationScreen::new);
        event.register(ModMenus.STONECUTTER_STATION.get(), StonecutterScreen::new);
        event.register(ModMenus.GRINDSTONE_STATION.get(), GrindstoneScreen::new);
        event.register(ModMenus.CARTOGRAPHY_STATION.get(), CartographyTableScreen::new);
        event.register(ModMenus.LOOM_STATION.get(), LoomScreen::new);
        event.register(ModMenus.SMITHING_STATION.get(), SmithingScreen::new);
        event.register(ModMenus.ANVIL_STATION.get(), AnvilScreen::new);
        event.register(ModMenus.FURNACE_STATION.get(), FurnaceScreen::new);
        event.register(ModMenus.SMOKER_STATION.get(), SmokerScreen::new);
        event.register(ModMenus.BLAST_FURNACE_STATION.get(), BlastFurnaceScreen::new);
        event.register(ModMenus.BREWING_STAND_STATION.get(), BrewingStandScreen::new);
    }
}
