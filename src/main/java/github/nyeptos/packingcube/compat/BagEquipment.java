package github.nyeptos.packingcube.compat;

import github.nyeptos.packingcube.bag.refill.RefillDestination;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.List;

/** Optional-dependency boundary: no Curios types may appear in common signatures. */
public final class BagEquipment {
    public record Target(String id, ResourceLocation icon, ItemStack stack) {}

    public static List<Target> refillTargets(Player player) {
        return available() ? CuriosCompat.refillTargets(player) : List.of();
    }

    public static RefillDestination refillDestination(Player player, String id) {
        return available() ? CuriosCompat.refillDestination(player, id) : null;
    }

    public static net.minecraft.world.inventory.Slot equipmentSlot(Player player, String id) {
        return available() ? CuriosCompat.equipmentSlot(player, id) : null;
    }

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
