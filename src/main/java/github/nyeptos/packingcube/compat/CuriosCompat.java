package github.nyeptos.packingcube.compat;

import github.nyeptos.packingcube.bag.BagSounds;
import github.nyeptos.packingcube.bag.refill.RefillDestination;
import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.gametest.compat.CuriosBagGameTests;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loaded only when Curios is present. Uses only its public 9.x API. */
final class CuriosCompat {
    private CuriosCompat() {}

    static void register(IEventBus bus) {
        bus.addListener(CuriosCompat::registerCapabilities);
        bus.addListener(CuriosCompat::registerGameTests);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(CuriosBagGameTests.class);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                CuriosCapability.ITEM,
                (stack, ignored) ->
                        new ICurio() {
                            @Override
                            public ItemStack getStack() {
                                return stack;
                            }

                            @Override
                            public SoundInfo getEquipSound(SlotContext context) {
                                return new SoundInfo(
                                        BagSounds.leather(), BagSounds.VOLUME, BagSounds.PITCH);
                            }

                            @Override
                            public boolean canEquip(SlotContext context) {
                                return context.identifier().equals("back") && !context.cosmetic();
                            }
                        },
                ModItems.LARGE_BAG.get());
    }

    static List<BagEquipment.Target> refillTargets(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return List.of();
        List<BagEquipment.Target> targets = new ArrayList<>();
        // Deterministic order, but identity remains type + index across additions/removals.
        inventory.getCurios().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> {
                            String type = entry.getKey();
                            var definition = CuriosApi.getSlot(type, player.level()).orElse(null);
                            if (definition == null) return;
                            var stacks = entry.getValue().getStacks();
                            for (int i = 0; i < stacks.getSlots(); i++) {
                                String id = RefillTargets.curios(type, i);
                                if (inventory.isSlotActive(type, i) && RefillTargets.valid(id))
                                    targets.add(
                                            new BagEquipment.Target(
                                                    id,
                                                    definition.getIcon(),
                                                    stacks.getStackInSlot(i).copy()));
                            }
                        });
        return List.copyOf(targets);
    }

    static RefillDestination refillDestination(Player player, String id) {
        int split = id.lastIndexOf(':');
        String type = id.substring(7, split);
        int index = Integer.parseInt(id.substring(split + 1));
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return null;
        var handler = inventory.getStacksHandler(type).orElse(null);
        if (handler == null
                || index >= handler.getStacks().getSlots()
                || !inventory.isSlotActive(type, index)) return null;
        var stacks = handler.getStacks();
        return new RefillDestination() {
            private boolean live() {
                return inventory.getStacksHandler(type).orElse(null) == handler
                        && index < stacks.getSlots()
                        && inventory.isSlotActive(type, index);
            }

            public ItemStack read() {
                return live() ? stacks.getStackInSlot(index) : ItemStack.EMPTY;
            }

            public boolean accepts(ItemStack stack) {
                return live() && stacks.isItemValid(index, stack);
            }

            public boolean removable() {
                var current = read();
                return live()
                        && (current.isEmpty()
                                || ItemStack.matches(
                                        current,
                                        stacks.extractItem(index, current.getCount(), true)));
            }

            public int limit(ItemStack stack) {
                return live() ? Math.min(stack.getMaxStackSize(), stacks.getSlotLimit(index)) : 0;
            }

            public boolean write(ItemStack expected, ItemStack replacement) {
                if (!live()
                        || !ItemStack.matches(read(), expected)
                        || !accepts(replacement)
                        || (!expected.isEmpty()
                                && !ItemStack.isSameItemSameComponents(expected, replacement)
                                && !removable())) return false;
                stacks.setStackInSlot(index, replacement);
                // Curios handles normal equip/unequip callbacks and synchronization in its tick.
                return true;
            }
        };
    }

    static net.minecraft.world.inventory.Slot equipmentSlot(Player player, String id) {
        int split = id.lastIndexOf(':');
        String type = id.substring(7, split);
        int index = Integer.parseInt(id.substring(split + 1));
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return null;
        var handler = inventory.getStacksHandler(type).orElse(null);
        if (handler == null || index >= handler.getStacks().getSlots()) return null;
        var stacks = handler.getStacks();
        return new net.neoforged.neoforge.items.SlotItemHandler(stacks, index, -10000, -10000) {
            private boolean live() {
                return inventory.getStacksHandler(type).orElse(null) == handler
                        && index < stacks.getSlots()
                        && inventory.isSlotActive(type, index);
            }

            @Override
            public boolean isActive() {
                return false;
            } // Drawn by the shared side panel.

            @Override
            public ItemStack getItem() {
                return live() ? super.getItem() : ItemStack.EMPTY;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return live() && super.mayPlace(stack);
            }

            @Override
            public boolean mayPickup(Player actor) {
                return live() && super.mayPickup(actor);
            }

            @Override
            public boolean allowModification(Player actor) {
                return mayPickup(actor);
            }

            @Override
            public void set(ItemStack stack) {
                if (!live()) return;
                ItemStack previous = getItem().copy();
                super.set(stack);
                if (!player.level().isClientSide
                        && !stack.isEmpty()
                        && !ItemStack.matches(previous, stack))
                    CuriosApi.getCurio(stack)
                            .ifPresent(
                                    curio ->
                                            curio.onEquipFromUse(
                                                    new SlotContext(
                                                            type,
                                                            player,
                                                            index,
                                                            false,
                                                            handler.getRenders().get(index))));
            }

            @Override
            public ItemStack remove(int amount) {
                return live() ? super.remove(amount) : ItemStack.EMPTY;
            }
        };
    }

    static List<ItemStack> activeBackBags(Player player) {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return List.of();
        var back = inventory.getStacksHandler("back").orElse(null);
        if (back == null) return List.of();

        List<ItemStack> result = new ArrayList<>();
        var stacks = back.getStacks();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (inventory.isSlotActive("back", slot)
                    && stack.getCount() == 1
                    && stack.is(ModItems.LARGE_BAG.get())) {
                result.add(stack);
            }
        }
        // These are live stacks. Curios detects component changes in its own tick/sync path.
        // Never write a captured stack back into a captured equipment slot after a bag moves.
        return result;
    }
}
