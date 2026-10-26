package github.nyeptos.packingcube.bag.refill;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Read-only troubleshooting. Does not refill, save item views, or bypass any safety condition. */
public final class RefillDiagnostics {
    private RefillDiagnostics() {}

    public static String reason(Player player, int slot) {
        if (player.isCreative()) return "creative";
        if (!player.isAlive() || player.isSpectator()) return "inactive";
        if (player.containerMenu != player.inventoryMenu) return "menu";
        if (!player.containerMenu.getCarried().isEmpty()) return "cursor";
        var supplies = RefillSupply.collect(player, slot);
        if (supplies.isEmpty()) {
            if (RefillSupply.collect(player, -2).stream()
                    .anyMatch(p -> RefillSlotSettings.mapped(p.bagStack()))) return "mapped";
            return "pouch";
        }
        var held = player.getInventory().getItem(slot);
        if (held.isEmpty()) return "empty";
        int matching = 0;
        for (var pouch : supplies)
            for (int i = 0; i < pouch.getContainerSize(); i++)
                if (ItemStack.isSameItemSameComponents(pouch.getItem(i), held))
                    matching += pouch.getItem(i).getCount();
        if (matching == 0) return "stock";
        if (RefillSupply.available(supplies, held) == 0) return "filter";
        return "use";
    }
}
