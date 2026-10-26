package github.nyeptos.packingcube.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.List;

/** Optional-dependency boundary: no Curios types may appear in common signatures. */
public final class BagEquipment {
    private BagEquipment() {}

    public static boolean available() {
        return ModList.get().isLoaded("curios");
    }

    public static void register(IEventBus bus) {
        if (available()) CuriosCompat.register(bus);
    }

    /** Functional, active back slots only, in slot-index order. Never cosmetics. */
    public static List<ItemStack> activeBackBags(Player player) {
        return available() ? CuriosCompat.activeBackBags(player) : List.of();
    }
}
