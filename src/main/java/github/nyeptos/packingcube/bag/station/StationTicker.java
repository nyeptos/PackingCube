package github.nyeptos.packingcube.bag.station;

import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.menu.station.StationOwner;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Runs the furnaces carried in backpacks, GUI open or closed. A station whose menu is open is
 * ticked through that menu's own state so the screen and the block never disagree.
 */
public final class StationTicker {
    private StationTicker() {}

    public static void tick(Player player) {
        Level level = player.level();
        var seen = Collections.newSetFromMap(new IdentityHashMap<ItemStack, Boolean>());
        List<ItemStack> roots = new ArrayList<>(BagEquipment.activeBackBags(player));
        for (int i = 0; i < 36; i++) roots.add(player.getInventory().getItem(i));
        for (ItemStack root : roots) {
            if (!root.is(ModItems.LARGE_BAG.get()) || root.getCount() != 1 || !seen.add(root))
                continue;
            List<ItemStack> modules =
                    root.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                            .stream()
                            .toList();
            boolean touched = false;
            for (int slot = 0; slot < modules.size(); slot++) {
                ItemStack block = modules.get(slot);
                StationBlocks.Kind kind = StationBlocks.kind(block);
                if (kind == null || !StationBlocks.ticking(kind)) continue;
                TickingStation station = open(player, root, slot);
                if (station == null) station = create(player, root, slot, block, kind);
                if (!station.tick(level)) continue;
                station.flush();
                touched = true;
            }
            if (touched) player.getInventory().setChanged();
        }
    }

    private static TickingStation create(
            Player player,
            ItemStack root,
            int moduleSlot,
            ItemStack block,
            StationBlocks.Kind kind) {
        return kind == StationBlocks.Kind.BREWING_STAND
                ? StationBrewingStand.of(player, () -> root, moduleSlot, block)
                : StationFurnace.of(player, () -> root, moduleSlot, block, kind);
    }

    private static TickingStation open(Player player, ItemStack root, int moduleSlot) {
        if (player.containerMenu instanceof StationOwner owner
                && owner.ownsStation(root, moduleSlot)) return owner.station();
        return null;
    }
}
