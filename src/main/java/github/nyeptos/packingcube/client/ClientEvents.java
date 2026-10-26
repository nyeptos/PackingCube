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
    }
}
