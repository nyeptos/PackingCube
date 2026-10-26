package github.nyeptos.packingcube.event;

import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillService;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillTargets;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * Explicit consumption witnesses, not inventory-wide count-decrease polling. Server thread only.
 */
public final class RefillHandler {
    private record Pending(int slot, ItemStack before, long tick) {}

    private static final WeakHashMap<Player, Map<Integer, Pending>> PENDING = new WeakHashMap<>();

    private RefillHandler() {}

    private static void record(
            Player player, InteractionHand hand, ItemStack before, boolean totem) {
        if (player.level().isClientSide
                || player.isCreative()
                || (!totem && !player.isAlive())
                || player.isSpectator()
                || before.isEmpty()
                || player.containerMenu != player.inventoryMenu) return;
        long tick = player.level().getGameTime();
        int slot =
                hand == InteractionHand.OFF_HAND
                        ? RefillSettings.OFFHAND_SLOT
                        : player.getInventory().selected;
        var pending = PENDING.computeIfAbsent(player, ignored -> new TreeMap<>());
        var prior = pending.get(slot);
        if (prior != null
                && prior.tick() == tick
                && ItemStack.isSameItemSameComponents(prior.before(), before)) return;
        pending.put(slot, new Pending(slot, before.copy(), tick));
    }

    public static void onBlock(PlayerInteractEvent.RightClickBlock event) {
        record(event.getEntity(), event.getHand(), event.getItemStack(), false);
    }

    public static void onItem(PlayerInteractEvent.RightClickItem event) {
        record(event.getEntity(), event.getHand(), event.getItemStack(), false);
    }

    public static void onEntity(PlayerInteractEvent.EntityInteract event) {
        record(event.getEntity(), event.getHand(), event.getItemStack(), false);
    }

    public static void onSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        record(event.getEntity(), event.getHand(), event.getItemStack(), false);
    }

    public static void onUseOn(UseItemOnBlockEvent event) {
        if (event.getPlayer() != null)
            record(event.getPlayer(), event.getHand(), event.getItemStack(), false);
    }

    public static void onConsumed(PlayerDestroyItemEvent event) {
        if (event.getHand() != null && !event.getOriginal().isDamageableItem())
            record(event.getEntity(), event.getHand(), event.getOriginal(), false);
    }

    public static void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player)
            record(player, event.getHand(), event.getItem(), false);
    }

    public static void onTotem(LivingUseTotemEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Player player)
            record(player, event.getHandHolding(), event.getTotem(), true);
    }

    public static void onToss(ItemTossEvent event) {
        if (!event.getPlayer().level().isClientSide) PENDING.remove(event.getPlayer());
    }

    public static void onTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide) return;
        var pending = PENDING.remove(player);
        var consumed = new HashSet<String>();
        if (pending != null)
            for (var entry : pending.values()) {
                long age = player.level().getGameTime() - entry.tick();
                var current = player.getInventory().getItem(entry.slot());
                if (age >= 0
                        && age <= 1
                        && (current.isEmpty()
                                || !ItemStack.isSameItemSameComponents(current, entry.before())
                                || current.getCount() < entry.before().getCount()))
                    consumed.add(RefillTargets.inventory(entry.slot()));
            }
        if (pending != null)
            for (var entry : pending.values()) {
                long age = player.level().getGameTime() - entry.tick();
                if (age >= 0 && age <= 1) RefillService.hand(player, entry.slot(), entry.before());
            }
        if (!consumed.isEmpty() || player.level().getGameTime() % 5 == 0)
            RefillDispatcher.tick(player, consumed);
        RefillService.trade(player);
    }
}
