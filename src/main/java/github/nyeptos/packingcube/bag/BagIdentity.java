package github.nyeptos.packingcube.bag;

import github.nyeptos.packingcube.compat.BagEquipment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/** A new identity per opened root avoids accidentally following a pre-existing creative copy. */
public final class BagIdentity {
    private static final String KEY = "packingcube_open_id";

    private BagIdentity() {}

    public static UUID renew(ItemStack bag) {
        UUID id = UUID.randomUUID();
        var data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putUUID(KEY, id);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return id;
    }

    public static boolean matches(ItemStack bag, UUID id) {
        if (id == null || bag.isEmpty()) return false;
        var data = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.hasUUID(KEY) && data.getUUID(KEY).equals(id);
    }

    public static ItemStack findUnique(
            net.minecraft.world.entity.player.Player player,
            java.util.function.Predicate<ItemStack> matches,
            ItemStack cursor) {
        if (!player.isAlive()) return ItemStack.EMPTY;
        ItemStack found = ItemStack.EMPTY;
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (matches.test(candidate)) {
                found = candidate;
                count++;
            }
        }
        for (ItemStack candidate : BagEquipment.activeBackBags(player)) {
            if (matches.test(candidate)) {
                found = candidate;
                count++;
            }
        }
        if (matches.test(cursor)) {
            found = cursor;
            count++;
        }
        return count == 1 && found.getCount() == 1 ? found : ItemStack.EMPTY;
    }
}
