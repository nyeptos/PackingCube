package github.nyeptos.packingcube.bag.station;

import github.nyeptos.packingcube.bag.BagIdentity;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Finds and updates the station block sitting in one module slot of the opened backpack. */
public final class StationStorage {
    private StationStorage() {}

    public static ItemStack find(Player player, UUID rootId, int moduleSlot) {
        if (!valid(moduleSlot)) return ItemStack.EMPTY;
        ItemStack root = root(player, rootId);
        if (root.isEmpty()) return ItemStack.EMPTY;
        ItemStack module = new BagInventory(root, true, () -> {}).getItem(moduleSlot);
        return StationBlocks.isStation(module) ? module : ItemStack.EMPTY;
    }

    /** Writes the block back with whatever its GUI holds now. */
    public static boolean store(Player player, UUID rootId, int moduleSlot, ItemStack block) {
        if (!valid(moduleSlot)) return false;
        return store(root(player, rootId), moduleSlot, block);
    }

    /** Writes into a bag stack the caller already holds, without searching the inventory. */
    public static boolean store(ItemStack root, int moduleSlot, ItemStack block) {
        if (!valid(moduleSlot) || root.isEmpty()) return false;
        BagInventory modules = new BagInventory(root, true, () -> {});
        ItemStack module = modules.getItem(moduleSlot);
        if (!StationBlocks.isStation(module) || !ItemStack.isSameItem(module, block)) return false;
        modules.setItem(moduleSlot, block.copy());
        return true;
    }

    private static boolean valid(int moduleSlot) {
        return moduleSlot >= 0 && moduleSlot < BagInventory.MODULE_SLOTS;
    }

    /** The live backpack stack this id belongs to, or empty when it is gone. */
    public static ItemStack root(Player player, UUID rootId) {
        return BagIdentity.findUnique(
                player,
                stack -> stack.is(ModItems.BACKPACK.get()) && BagIdentity.matches(stack, rootId),
                ItemStack.EMPTY);
    }
}
