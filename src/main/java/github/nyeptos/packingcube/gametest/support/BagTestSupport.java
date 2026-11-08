package github.nyeptos.packingcube.gametest.support;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public final class BagTestSupport {
    private BagTestSupport() {}
    public static ItemStack filter() {
        return ModItems.STORAGE_POUCH.get().getDefaultInstance();
    }

    public static ItemStack carrier() {
        return ModItems.BACKPACK.get().getDefaultInstance();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    public static BagInventory storage(ItemStack bag) {
        return new BagInventory(bag, false, () -> {});
    }

    public static BagInventory modules(ItemStack bag) {
        return new BagInventory(bag, true, () -> {});
    }

    public static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    public static int total(BagInventory inv) {
        int result = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) result += inv.getItem(i).getCount();
        return result;
    }
}
