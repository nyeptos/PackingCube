package github.nyeptos.packingcube.event;

import github.nyeptos.packingcube.bag.BagRouter;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public final class BagPickupHandler {
    private BagPickupHandler() {}

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        if (player.level().isClientSide
                || player.isSpectator()
                || !player.isAlive()
                || event.canPickup().isFalse()
                || entity.isRemoved()) return;
        // Pre fires before vanilla's pickup-delay and owner checks: preserve them unless explicitly
        // overridden.
        if (!event.canPickup().isTrue()
                && (entity.hasPickUpDelay()
                        || entity.getTarget() != null
                                && !entity.getTarget().equals(player.getUUID()))) return;
        ItemStack original = entity.getItem();
        ItemStack remainder = BagRouter.route(player, original);
        int stored = original.getCount() - remainder.getCount();
        if (stored <= 0) return;
        player.take(entity, stored);
        player.awardStat(Stats.ITEM_PICKED_UP.get(original.getItem()), stored);
        player.onItemPickup(entity);
        // Pre permits shrinking the live stack, but forbids ItemEntity#setItem.
        original.shrink(stored);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        if (original.isEmpty()) {
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        }
        // Any remainder is handled by vanilla's inventory insertion and full-inventory behavior.
    }
}
