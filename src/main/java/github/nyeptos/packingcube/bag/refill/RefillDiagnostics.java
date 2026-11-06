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
                    .anyMatch(p -> RefillSlotDestinations.mapped(p.bagStack()))) return "mapped";
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

    /** One line per configured rule: what it supplies, what it targets and what the limit says. */
    public static String rule(Player player, ItemStack supply, RefillSlotDestinations.Rule rule) {
        var text = new StringBuilder(rule.enabled() ? "+" : "-");
        text.append(' ').append(rule.destination());
        text.append(" replace=").append(rule.replace() ? "on" : "off");
        text.append(" wear=").append(rule.durabilityMode()).append('/').append(rule.durability());
        text.append(" supply=").append(describe(supply));
        var destination =
                rule.destination().isEmpty()
                        ? null
                        : RefillDestination.resolve(player, rule.destination());
        if (destination == null) return text.append(" -> no-destination").toString();
        var old = destination.read();
        text.append(" dest=").append(describe(old));
        // An empty source cannot supply anything, so it must never read as a swap.
        if (supply.isEmpty()) return text.append(" -> no-supply").toString();
        if (old.isEmpty()) return text.append(" -> fill").toString();
        if (ItemStack.isSameItemSameComponents(supply, old))
            return text.append(" -> same").toString();
        if (!rule.replace()) return text.append(" -> keep").toString();
        text.append(" left=").append(remaining(old, rule));
        return text.append(RefillDispatcher.wornEnough(supply, old, rule) ? " -> swap" : " -> wait")
                .toString();
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        var id = stack.getItem().toString();
        return stack.isDamageableItem()
                ? id + "@" + stack.getDamageValue() + "/" + stack.getMaxDamage()
                : id + "x" + stack.getCount();
    }

    private static String remaining(ItemStack old, RefillSlotDestinations.Rule rule) {
        int max = old.getMaxDamage();
        if (!old.isDamageableItem() || max <= 0) return "none";
        int left = max - old.getDamageValue();
        if (rule.durabilityMode().equals("percent")) return ((long) left * 100 / max) + "%";
        return Integer.toString(left);
    }
}
