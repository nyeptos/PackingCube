package github.nyeptos.packingcube.compat;

import github.nyeptos.packingcube.bag.BagSounds;
import github.nyeptos.packingcube.gametest.CuriosBagGameTests;
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
