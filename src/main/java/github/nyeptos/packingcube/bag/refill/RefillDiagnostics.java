package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.bag.BagInventory;

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

    /** One line per configured rule: what it supplies, what it targets and what the limit says. */
    public static String rule(Player player, BagInventory pouch, int source) {
        var rule = RefillSlotSettings.get(pouch.bagStack(), source);
        var supply = pouch.getItem(source);
        var text = new StringBuilder(rule.enabled() ? "+" : "-");
        text.append(" #").append(source + 1).append(' ').append(rule.destination());
        text.append(" replace=").append(rule.replace() ? "on" : "off");
        text.append(" wear=").append(rule.durabilityMode()).append('/').append(rule.durability());
        // A returned item keeps its slot out of supply, which looks like a silent rule.
        boolean locked = RefillSettings.locked(pouch.bagStack(), source);
        text.append(" lock=").append(locked ? "on" : "off");
        text.append(" supply=").append(describe(supply));
        var destination =
                rule.destination().isEmpty()
                        ? null
                        : RefillDestination.resolve(player, rule.destination());
        if (destination == null) return text.append(" -> no-destination").toString();
        var old = destination.read();
        text.append(" dest=").append(describe(old));
        if (locked) return text.append(" -> locked").toString();
        if (old.isEmpty()) return text.append(" -> fill").toString();
        if (!rule.replace()) return text.append(" -> keep").toString();
        // The limit is measured on the item in place, so it is always worth printing.
        text.append(" left=").append(remaining(old, rule));
        boolean swap = RefillDispatcher.spentEnough(old, rule);
        text.append(swap ? " -> swap" : " -> wait");
        if (supply.isEmpty()) text.append(" (source empty)");
        else if (ItemStack.isSameItemSameComponents(supply, old)) text.append(" (identical)");
        return text.toString();
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        var id = stack.getItem().toString();
        return stack.isDamageableItem()
                ? id + "@" + stack.getDamageValue() + "/" + stack.getMaxDamage()
                : id + "x" + stack.getCount();
    }

    private static String remaining(ItemStack old, RefillSlotSettings.Rule rule) {
        int max = old.getMaxDamage();
        if (!old.isDamageableItem() || max <= 0) return "none";
        int left = max - old.getDamageValue();
        if (rule.durabilityMode().equals("percent")) return ((long) left * 100 / max) + "%";
        return Integer.toString(left);
    }
}
