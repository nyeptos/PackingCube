package github.nyeptos.packingcube.bag.refill;

import github.nyeptos.packingcube.compat.BagEquipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-side live destination adapter; all armor rules come from vanilla's actual Slot. */
public interface RefillDestination {
    ItemStack read();

    boolean accepts(ItemStack stack);

    boolean removable();

    int limit(ItemStack stack);

    boolean write(ItemStack expected, ItemStack replacement);

    static RefillDestination resolve(Player player, String id) {
        if (!RefillTargets.valid(id)) return null;
        if (id.startsWith("curios:")) return BagEquipment.refillDestination(player, id);
        int index = Integer.parseInt(id.substring(10));
        int menuIndex = index < 9 ? 36 + index : index < 36 ? index : index < 40 ? 44 - index : 45;
        var slot = player.inventoryMenu.getSlot(menuIndex);
        return new RefillDestination() {
            public ItemStack read() {
                return slot.getItem();
            }

            public boolean accepts(ItemStack stack) {
                return slot.mayPlace(stack);
            }

            public boolean removable() {
                return slot.mayPickup(player);
            }

            public int limit(ItemStack stack) {
                return slot.getMaxStackSize(stack);
            }

            public boolean write(ItemStack expected, ItemStack replacement) {
                if (!ItemStack.matches(read(), expected)
                        || !accepts(replacement)
                        || (!expected.isEmpty()
                                && !ItemStack.isSameItemSameComponents(expected, replacement)
                                && !removable())) return false;
                slot.setByPlayer(replacement, expected);
                player.getInventory().setChanged();
                // Correct client-side consumption predictions even when final count is unchanged.
                player.inventoryMenu.setRemoteSlot(menuIndex, ItemStack.EMPTY);
                return true;
            }
        };
    }
}
