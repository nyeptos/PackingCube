package github.nyeptos.packingcube.gametest.support;

import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** PC45/46 saved-item shape, used only to keep legacy behavior regression coverage explicit. */
public final class LegacyRefillFixture {
    private LegacyRefillFixture() {}

    public static ItemStack pouch() {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var data = pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.remove("packingcube_refill_sources");
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return pouch;
    }
}
