package github.nyeptos.packingcube.bag.station;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** The GUI contents that travel inside a station block stack. */
public final class StationItems {
    private StationItems() {}

    public static List<ItemStack> stored(ItemStack block, int count) {
        List<ItemStack> items = new ArrayList<>(count);
        ItemContainerContents contents =
                block.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> saved = contents.stream().toList();
        for (int i = 0; i < count; i++)
            items.add(i < saved.size() ? saved.get(i).copy() : ItemStack.EMPTY);
        return items;
    }

    public static void store(ItemStack block, List<ItemStack> items) {
        block.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }
}
