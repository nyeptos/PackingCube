package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Packingcube.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BAGS =
            TABS.register(
                    "bags",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.packingcube"))
                                    .icon(() -> ModItems.LARGE_BAG.get().getDefaultInstance())
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(ModItems.LARGE_BAG.get());
                                                output.accept(ModItems.FILTER_BAG.get());
                                                output.accept(ModItems.REFILL_POUCH.get());
                                            })
                                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
