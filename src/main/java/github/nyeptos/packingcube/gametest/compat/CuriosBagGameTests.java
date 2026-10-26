package github.nyeptos.packingcube.gametest.compat;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.bag.pickup.BagPickupSettings;
import github.nyeptos.packingcube.bag.pickup.BagRouter;
import github.nyeptos.packingcube.compat.BagEquipment;
import github.nyeptos.packingcube.event.BagPickupHandler;
import github.nyeptos.packingcube.gametest.refill.RefillOpeningGameTests;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Explicitly registered only with Curios installed. No annotation-based unconditional class
 * loading.
 */
@PrefixGameTestTemplate(false)
public final class CuriosBagGameTests {
    private static Player player(GameTestHelper helper) {
        Player result = helper.makeMockPlayer(GameType.SURVIVAL);
        inventory(result).reset();
        return result;
    }

    private static ICuriosItemHandler inventory(Player player) {
        return CuriosApi.getCuriosInventory(player).orElseThrow();
    }

    private static IDynamicStackHandler back(Player player) {
        return inventory(player).getStacksHandler("back").orElseThrow().getStacks();
    }

    private static ItemStack bag() {
        return ModItems.LARGE_BAG.get().getDefaultInstance();
    }

    private static BagInventory modules(ItemStack stack) {
        return new BagInventory(stack, true, () -> {});
    }

    private static BagInventory storage(ItemStack stack) {
        return new BagInventory(stack, false, () -> {});
    }

    private static ItemStack stoneModule() {
        ItemStack module = ModItems.FILTER_BAG.get().getDefaultInstance();
        BagFilter.add(module, "items", ResourceLocation.withDefaultNamespace("stone"));
        return module;
    }

    private static ItemStack configuredBag() {
        ItemStack stack = bag();
        modules(stack).setItem(0, stoneModule());
        return stack;
    }

    private static int stored(ItemStack root) {
        BagInventory contents = storage(modules(root).getItem(0));
        int result = 0;
        for (int i = 0; i < contents.getContainerSize(); i++)
            result += contents.getItem(i).getCount();
        return result;
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void backAcceptsBigBagOnly(GameTestHelper helper) {
        Player player = player(helper);
        helper.assertTrue(
                back(player).getSlots() == 1, "One back slot must be supplied by the datapack");
        SlotContext context = new SlotContext("back", player, 0, false, true);
        helper.assertTrue(CuriosApi.isStackValid(context, bag()), "Big Bag is tagged for back");
        helper.assertFalse(
                CuriosApi.isStackValid(context, stoneModule()), "Filter Bag is not a Curios item");
        var curio = CuriosApi.getCurio(bag()).orElseThrow();
        helper.assertTrue(curio.canEquip(context), "Functional back is allowed");
        helper.assertFalse(
                curio.canEquip(new SlotContext("ring", player, 0, false, true)),
                "Other slots are denied");
        helper.assertFalse(
                curio.canEquip(new SlotContext("back", player, 0, true, true)),
                "Cosmetic equipment is denied");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void equippedRoutingFirstThenInventory(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack equipped = configuredBag();
        back(player).setStackInSlot(0, equipped);
        ItemStack carried = configuredBag();
        player.getInventory().setItem(0, carried);
        BagInventory parent = modules(equipped);
        BagInventory full = new BagInventory(parent.getItem(0), false, parent);
        for (int i = 0; i < 9; i++) full.setItem(i, new ItemStack(Items.STONE, i == 8 ? 60 : 64));
        ItemStack source = new ItemStack(Items.STONE, 16);
        helper.assertTrue(
                BagRouter.route(player, source).isEmpty(),
                "Both equipment and inventory participate");
        helper.assertTrue(source.getCount() == 16, "Incoming stack is not mutated");
        helper.assertTrue(
                stored(equipped) == 576 && stored(carried) == 12,
                "Equipment fills first, remainder enters inventory bag");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void equippedPickupAndCuriosSaveRoundTrip(GameTestHelper helper) {
        Player player = player(helper);
        back(player).setStackInSlot(0, configuredBag());
        ItemEntity entity =
                new ItemEntity(helper.getLevel(), 0, 1, 0, new ItemStack(Items.STONE, 23));
        entity.setNoPickUpDelay();
        BagPickupHandler.onPickup(new ItemEntityPickupEvent.Pre(player, entity));
        helper.assertTrue(
                entity.isRemoved(), "Pickup hook stores and removes the real ground item");
        helper.assertTrue(
                stored(back(player).getStackInSlot(0)) == 23, "Items persist on equipped stack");
        var saved = inventory(player).saveInventory(false);
        back(player).setStackInSlot(0, ItemStack.EMPTY);
        inventory(player).loadInventory(saved);
        helper.assertTrue(
                stored(back(player).getStackInSlot(0)) == 23,
                "Curios inventory NBT preserves nested contents");
        helper.assertTrue(
                BagFilter.matches(
                        modules(back(player).getStackInSlot(0)).getItem(0),
                        new ItemStack(Items.STONE)),
                "Filter rules survive the Curios save round trip");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void equippedMenuTracksMovesAndRefusesDuplicates(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = configuredBag();
        back(player).setStackInSlot(0, root);
        BagInventory contents = modules(root);
        BagMenu menu = new BagMenu(1, player.getInventory(), contents, root, -1, true);
        player.containerMenu = menu;
        helper.assertTrue(menu.stillValid(player), "Equipped root is valid");
        ItemStack moved = root.copy();
        back(player).setStackInSlot(0, ItemStack.EMPTY);
        player.getInventory().setItem(4, moved);
        helper.assertTrue(menu.stillValid(player), "Unequipping to inventory keeps identity");
        contents.setItem(3, stoneModule());
        helper.assertTrue(
                modules(moved).getItem(3).is(ModItems.FILTER_BAG.get()),
                "Updates target new stack");
        helper.assertTrue(
                back(player).getStackInSlot(0).isEmpty(),
                "Save never re-equips a stale captured stack");
        ItemStack reequipped = moved.copy();
        player.getInventory().setItem(4, ItemStack.EMPTY);
        back(player).setStackInSlot(0, reequipped);
        helper.assertTrue(menu.stillValid(player), "Moving back to equipment works");
        player.getInventory().setItem(5, reequipped.copy());
        helper.assertFalse(
                menu.stillValid(player), "Inventory/equipment duplicate IDs are ambiguous");
        player.getInventory().setItem(5, ItemStack.EMPTY);
        back(player).setStackInSlot(0, ItemStack.EMPTY);
        helper.assertFalse(menu.stillValid(player), "Missing root ends editing");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void equippedModuleEditingAndPickupIsolation(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack root = configuredBag();
        back(player).setStackInSlot(0, root);
        BagInventory parent = modules(root);
        BagInventory inner = new BagInventory(parent.getItem(0), false, parent);
        BagMenu menu = new BagMenu(2, player.getInventory(), inner, root, -1, false);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.editFilter(player, 3, -1, "mode", "exclude"),
                "Mounted filter is editable while worn");
        helper.assertTrue(
                BagFilter.isExcluding(modules(root).getItem(0)),
                "Rule change saves to equipped parent");
        helper.assertTrue(
                BagRouter.route(player, new ItemStack(Items.DIAMOND, 5)).getCount() == 5,
                "Open equipped parent is excluded from independent automatic writers");
        ItemStack moved = root.copy();
        back(player).setStackInSlot(0, ItemStack.EMPTY);
        player.getInventory().setItem(1, moved);
        helper.assertTrue(menu.stillValid(player), "Mounted menu follows unequipped parent");
        inner.setItem(0, new ItemStack(Items.DIAMOND, 6));
        helper.assertTrue(
                storage(modules(moved).getItem(0)).getItem(0).getCount() == 6,
                "Nested contents save to moved parent, not stale equipment");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void cosmeticAndInactiveBackDoNotFunction(GameTestHelper helper) {
        Player player = player(helper);
        var handler = inventory(player).getStacksHandler("back").orElseThrow();
        handler.getCosmeticStacks().setStackInSlot(0, configuredBag());
        helper.assertTrue(
                BagEquipment.activeBackBags(player).isEmpty(), "Cosmetics are never functional");
        back(player).setStackInSlot(0, configuredBag());
        handler.getActiveStates().set(0, false);
        helper.assertTrue(
                BagEquipment.activeBackBags(player).isEmpty(), "Disabled slots are ignored");
        helper.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 8)).getCount() == 8,
                "Cosmetic/inactive equipment cannot swallow items");
        helper.assertFalse(EquippedBagAccess.open(player), "Inactive bag cannot open");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void openRequestResolvesServerEquipmentAndGuardsMenus(GameTestHelper helper) {
        // Mock only menu transport; the actual equipment capability, data, and menu factory are
        // real.
        Player player =
                new Player(
                        helper.getLevel(),
                        BlockPos.ZERO,
                        0,
                        new GameProfile(UUID.randomUUID(), "bag-open-test")) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }

                    @Override
                    public OptionalInt openMenu(MenuProvider provider) {
                        containerMenu = provider.createMenu(12, getInventory(), this);
                        return OptionalInt.of(12);
                    }

                    @Override
                    public OptionalInt openMenu(
                            MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
                        var buffer =
                                new RegistryFriendlyByteBuf(
                                        io.netty.buffer.Unpooled.buffer(),
                                        level().registryAccess());
                        try {
                            extra.accept(buffer);
                            helper.assertTrue(
                                    buffer.readVarInt() == -1 && !buffer.readBoolean(),
                                    "Radial selection sends the installed Filter Bag menu format");
                        } finally {
                            buffer.release();
                        }
                        containerMenu = provider.createMenu(13, getInventory(), this);
                        return OptionalInt.of(13);
                    }
                };
        inventory(player).reset();
        helper.assertFalse(
                EquippedBagAccess.open(player),
                "No client-supplied item can bypass absent equipment");
        ItemStack root = configuredBag();
        back(player).setStackInSlot(0, root);
        player.getInventory()
                .setItem(0, configuredBag()); // Equipped root still wins over inventory fallback.
        player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
        helper.assertFalse(
                EquippedBagAccess.open(player), "Cursor stack must not be lost on a key request");
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(EquippedBagAccess.open(player), "Server opens its actual equipped bag");
        helper.assertTrue(
                player.containerMenu instanceof BagRadialMenu menu
                        && menu.ownsRoot(root)
                        && menu.stillValid(player),
                "Factory receives the real equipped stack");
        helper.assertFalse(
                EquippedBagAccess.open(player), "Repeated requests cannot replace an active menu");
        var radial = (BagRadialMenu) player.containerMenu;
        helper.assertTrue(
                radial.select(player, 0), "Equipped module opens through the real selector");
        helper.assertTrue(
                player.containerMenu instanceof BagMenu selected
                        && !selected.isModules()
                        && selected.stillValid(player),
                "Selected module keeps the equipped parent valid");
        helper.assertTrue(
                ((BagMenu) player.containerMenu).editFilter(player, 3, -1, "mode", "exclude"),
                "Equipped module is editable after radial transition");
        helper.assertTrue(
                BagFilter.isExcluding(modules(root).getItem(0)),
                "Selected rule saves to actual worn root");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void directKeyOpensWornBagBeforeInventoryFallback(GameTestHelper helper) {
        Player player =
                new Player(
                        helper.getLevel(),
                        BlockPos.ZERO,
                        0,
                        new GameProfile(UUID.randomUUID(), "direct-bag-test")) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }

                    @Override
                    public OptionalInt openMenu(
                            MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
                        var buffer =
                                new RegistryFriendlyByteBuf(
                                        io.netty.buffer.Unpooled.buffer(),
                                        level().registryAccess());
                        try {
                            extra.accept(buffer);
                            helper.assertTrue(
                                    buffer.readVarInt() == -1 && buffer.readBoolean(),
                                    "Direct key sends management format");
                        } finally {
                            buffer.release();
                        }
                        containerMenu = provider.createMenu(14, getInventory(), this);
                        return OptionalInt.of(14);
                    }
                };
        inventory(player).reset();
        ItemStack root = configuredBag();
        back(player).setStackInSlot(0, root);
        player.getInventory().setItem(0, configuredBag());
        helper.assertTrue(
                EquippedBagAccess.openDirect(player),
                "Separate direct request opens actual worn bag");
        helper.assertTrue(
                player.containerMenu instanceof BagMenu menu
                        && menu.isModules()
                        && menu.rootBag() == root
                        && menu.stillValid(player),
                "Equipped bag wins over inventory fallback");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void equippedBagNavigationKeepsParentAcrossRadialAndDirect(
            GameTestHelper helper) {
        Player player =
                new Player(
                        helper.getLevel(),
                        BlockPos.ZERO,
                        0,
                        new GameProfile(UUID.randomUUID(), "bag-navigation-test")) {
                    private int nextId = 40;

                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }

                    @Override
                    public OptionalInt openMenu(MenuProvider provider) {
                        containerMenu = provider.createMenu(++nextId, getInventory(), this);
                        return OptionalInt.of(containerMenu.containerId);
                    }

                    @Override
                    public OptionalInt openMenu(
                            MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
                        containerMenu = provider.createMenu(++nextId, getInventory(), this);
                        return OptionalInt.of(containerMenu.containerId);
                    }
                };
        inventory(player).reset();
        ItemStack root = configuredBag();
        back(player).setStackInSlot(0, root);
        player.getInventory().setItem(0, configuredBag());
        helper.assertTrue(EquippedBagAccess.openDirect(player), "Open worn bag");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, true),
                "C opens worn radial");
        var radial = (BagRadialMenu) player.containerMenu;
        helper.assertTrue(radial.select(player, 0), "Open worn Filter Bag");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, false),
                "B returns to worn parent");
        helper.assertTrue(
                player.containerMenu instanceof BagMenu parent
                        && parent.isModules()
                        && parent.rootBag() == root,
                "Parent remains the worn bag, not inventory fallback");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, false),
                "Second B closes");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Back to gameplay");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void curiosEquipUsesLeatherWithoutContentChangeSpam(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = bag();
        var curio = CuriosApi.getCurio(stack).orElseThrow();
        var context = new SlotContext("back", player, 0, false, true);
        var sound = curio.getEquipSound(context);
        helper.assertTrue(
                sound.soundEvent() == SoundEvents.ARMOR_EQUIP_LEATHER.value()
                        && sound.volume() == 1.0F
                        && sound.pitch() == 1.0F,
                "Curios receives vanilla leather equip sound");
        var emitted = new ArrayList<PlayLevelSoundEvent.AtPosition>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener =
                event -> {
                    if (event.getLevel() == helper.getLevel()) emitted.add(event);
                };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            curio.onEquip(context, ItemStack.EMPTY);
            modules(stack).setItem(0, stoneModule());
            curio.onEquip(context, stack.copy());
            helper.assertTrue(emitted.isEmpty(), "Data changes do not trigger equip audio");
            curio.onEquipFromUse(context);
            helper.assertTrue(
                    emitted.size() == 1
                            && emitted.getFirst().getSound() != null
                            && emitted.getFirst().getSound().value() == sound.soundEvent(),
                    "Standard Curios equip path emits leather once");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void pickupPriorityAndSwitchesAcrossCuriosAndInventory(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack equipped = configuredBag();
        back(player).setStackInSlot(0, equipped);
        ItemStack carried = configuredBag();
        player.getInventory().setItem(0, carried);
        var parent = modules(carried);
        BagPickupSettings.setPriority(parent.getItem(0), 9);
        parent.setChanged();
        BagRouter.route(player, new ItemStack(Items.STONE, 5));
        helper.assertTrue(
                stored(equipped) == 0 && stored(carried) == 5,
                "Explicit priority overrides equipment-first tie-breaker");
        BagPickupSettings.setEnabled(carried, false);
        BagRouter.route(player, new ItemStack(Items.STONE, 3));
        helper.assertTrue(
                stored(equipped) == 0 && stored(carried) == 8,
                "Legacy backpack OFF is ignored; pouch priority still wins");
        var carriedParent = modules(carried);
        BagPickupSettings.setEnabled(carriedParent.getItem(0), false);
        carriedParent.setChanged();
        BagRouter.route(player, new ItemStack(Items.STONE, 2));
        helper.assertTrue(
                stored(equipped) == 2 && stored(carried) == 8,
                "Disabling carried pouch routes to the equipped pouch");
        var equippedParent = modules(equipped);
        BagPickupSettings.setEnabled(equippedParent.getItem(0), false);
        equippedParent.setChanged();
        helper.assertTrue(
                BagRouter.route(player, new ItemStack(Items.STONE, 2)).getCount() == 2,
                "Disabled equipped pouch leaves remainder for vanilla");
        var saved = inventory(player).saveInventory(false);
        back(player).setStackInSlot(0, ItemStack.EMPTY);
        inventory(player).loadInventory(saved);
        helper.assertFalse(
                BagPickupSettings.enabled(modules(back(player).getStackInSlot(0)).getItem(0)),
                "Curios serialization preserves pouch settings");
        helper.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void manualStowUsesTheOpenCuriosBackpackOnly(GameTestHelper h) {
        var player = player(h);
        var root = configuredBag();
        back(player).setStackInSlot(0, root);
        var other = configuredBag();
        player.getInventory().setItem(0, other);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 13));
        var menu = new BagMenu(42, player.getInventory(), modules(root), root, -1, true);
        player.containerMenu = menu;
        h.assertTrue(
                menu.stowAll(player, 42) == 13
                        && stored(back(player).getStackInSlot(0)) == 13
                        && stored(other) == 0
                        && player.getInventory().getItem(9).isEmpty(),
                "Equipped root resolves, saves and syncs without routing to another carried"
                        + " backpack");
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void equippedBackpackSuppliesRefillPouchButPouchCannotEquip(GameTestHelper h) {
        var p = player(h);
        var supply = LegacyRefillFixture.pouch();
        github.nyeptos.packingcube.bag.refill.RefillSettings.set(supply, "timing", "use");
        github.nyeptos.packingcube.bag.refill.RefillSettings.set(supply, "target", "3");
        storage(supply).setItem(0, new ItemStack(Items.STONE, 8));
        var root = bag();
        modules(root).setItem(0, supply);
        back(p).setStackInSlot(0, root);
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, new ItemStack(Items.STONE));
        h.assertTrue(
                github.nyeptos.packingcube.bag.refill.RefillService.hotbar(
                                        p, 0, new ItemStack(Items.STONE, 3))
                                == 2
                        && storage(modules(back(p).getStackInSlot(0)).getItem(0))
                                        .getItem(0)
                                        .getCount()
                                == 6,
                "Refill saves through equipped backpack parent");
        h.assertTrue(
                !CuriosApi.isStackValid(new SlotContext("back", p, 0, false, true), supply),
                "Refill pouch is a module, not a wearable backpack");
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void refillTargetPreviewsUseRegisteredIconsAndActiveFunctionalSlots(
            GameTestHelper h) {
        var player = player(h);
        var equipped = bag();
        back(player).setStackInSlot(0, equipped);
        var targets = BagEquipment.refillTargets(player);
        var target =
                targets.stream()
                        .filter(t -> t.id().equals("curios:back:0"))
                        .findFirst()
                        .orElseThrow();
        h.assertTrue(
                target.icon()
                        .equals(CuriosApi.getSlot("back", player.level()).orElseThrow().getIcon()),
                "Registered type icon is used");
        h.assertTrue(
                target.stack() != equipped && ItemStack.matches(target.stack(), equipped),
                "UI receives defensive copies, not mutable equipment");
        inventory(player).setSlotActive("back", 0, false);
        h.assertTrue(
                BagEquipment.refillTargets(player).stream()
                        .noneMatch(t -> t.id().equals("curios:back:0")),
                "Inactive destinations disappear without selecting another slot");
        inventory(player).setSlotActive("back", 0, true);
        h.assertTrue(
                BagEquipment.refillTargets(player).stream()
                        .anyMatch(t -> t.id().equals("curios:back:0")),
                "Reactivation retains the same stable identity");
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void mappedCuriosSupplyHonorsPublicValidationAndInactiveSlots(GameTestHelper h) {
        var player = player(h);
        var supply = ModItems.REFILL_POUCH.get().getDefaultInstance();
        new BagInventory(supply, false, () -> {}).setItem(0, new ItemStack(Items.STONE, 12));
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(
                supply, 0, "destination", "curios:back:0");
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(supply, 0, "target", "3");
        var root = bag();
        modules(root).setItem(0, supply);
        player.getInventory().setItem(8, root);
        h.assertTrue(
                github.nyeptos.packingcube.bag.refill.RefillDispatcher.tick(
                                player, java.util.Set.of())
                        == 0,
                "Ordinary stone cannot equip in back slot");
        Consumer<top.theillusivec4.curios.api.event.CurioCanEquipEvent> allow =
                event -> {
                    if (event.getEntity() == player && event.getStack().is(Items.STONE))
                        event.setEquipResult(net.neoforged.neoforge.common.util.TriState.TRUE);
                };
        NeoForge.EVENT_BUS.addListener(allow);
        try {
            inventory(player).setSlotActive("back", 0, false);
            h.assertTrue(
                    github.nyeptos.packingcube.bag.refill.RefillDispatcher.tick(
                                    player, java.util.Set.of())
                            == 0,
                    "Inactive slot cannot receive items");
            inventory(player).setSlotActive("back", 0, true);
            h.assertTrue(
                    github.nyeptos.packingcube.bag.refill.RefillDispatcher.tick(
                                            player, java.util.Set.of())
                                    == 3
                            && back(player).getStackInSlot(0).getCount() == 3,
                    "Public validator permission allows valid custom equipment into active slot");
        } finally {
            NeoForge.EVENT_BUS.unregister(allow);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void mappedCuriosReplacementHonorsUnequipDenial(GameTestHelper h) {
        var player = player(h);
        var supply = ModItems.REFILL_POUCH.get().getDefaultInstance();
        new BagInventory(supply, false, () -> {}).setItem(0, new ItemStack(Items.STONE, 12));
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(
                supply, 0, "destination", "curios:back:0");
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(supply, 0, "replace", "true");
        var root = bag();
        modules(root).setItem(0, supply);
        player.getInventory().setItem(8, root);
        back(player).setStackInSlot(0, new ItemStack(Items.DIRT));
        Consumer<top.theillusivec4.curios.api.event.CurioCanEquipEvent> allow =
                event -> {
                    if (event.getEntity() == player)
                        event.setEquipResult(net.neoforged.neoforge.common.util.TriState.TRUE);
                };
        Consumer<top.theillusivec4.curios.api.event.CurioCanUnequipEvent> deny =
                event -> {
                    if (event.getEntity() == player)
                        event.setUnequipResult(net.neoforged.neoforge.common.util.TriState.FALSE);
                };
        NeoForge.EVENT_BUS.addListener(allow);
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            var before = root.copy();
            h.assertTrue(
                    github.nyeptos.packingcube.bag.refill.RefillDispatcher.tick(
                                            player, java.util.Set.of())
                                    == 0
                            && ItemStack.matches(root, before)
                            && back(player).getStackInSlot(0).is(Items.DIRT),
                    "Curios removal denial prevents all replacement mutation");
        } finally {
            NeoForge.EVENT_BUS.unregister(allow);
            NeoForge.EVENT_BUS.unregister(deny);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void bagMenuCuriosSlotsEquipUnequipAndTrackOpenBackpack(GameTestHelper h) {
        var player = player(h);
        var root = bag();
        player.getInventory().setItem(0, root);
        var menu = new BagMenu(81, player.getInventory(), modules(root), root, 0, true);
        player.containerMenu = menu;
        var slot = menu.sideSlot("curios:back:0");
        h.assertTrue(
                slot != null && !slot.mayPlace(new ItemStack(Items.STONE)),
                "Curios registered validity is used");
        // Pick up the currently opened root and equip it without closing or duplicating it.
        menu.clicked(36, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(menu.ownsRoot(menu.getCarried()), "Open root can move onto cursor");
        menu.clicked(slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(
                menu.getCarried().isEmpty()
                        && menu.ownsRoot(back(player).getStackInSlot(0))
                        && menu.stillValid(player),
                "Root follows equip to Curios");
        menu.clicked(slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(
                back(player).getStackInSlot(0).isEmpty()
                        && menu.ownsRoot(menu.getCarried())
                        && menu.stillValid(player),
                "Root follows unequip onto cursor");
        menu.clicked(36, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(
                menu.getCarried().isEmpty() && menu.ownsRoot(player.getInventory().getItem(0)),
                "Root returns to ordinary inventory without loss");
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void storagePouchCuriosSlotsHonorUnequipVeto(GameTestHelper h) {
        var player = player(h);
        var pouch = stoneModule();
        player.getInventory().setItem(0, pouch);
        back(player).setStackInSlot(0, bag());
        var menu = new BagMenu(82, player.getInventory(), storage(pouch), pouch, 0, false);
        player.containerMenu = menu;
        var slot = menu.sideSlot("curios:back:0");
        java.util.function.Consumer<top.theillusivec4.curios.api.event.CurioCanUnequipEvent> veto =
                event -> {
                    if (event.getSlotContext().entity() == player)
                        event.setUnequipResult(net.neoforged.neoforge.common.util.TriState.FALSE);
                };
        NeoForge.EVENT_BUS.addListener(veto);
        try {
            menu.clicked(slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            h.assertTrue(
                    menu.getCarried().isEmpty() && slot.hasItem(),
                    "Curios event veto blocks pickup from storage pouch screen");
            menu.clicked(slot.index, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
            h.assertTrue(slot.hasItem(), "Veto also blocks shift unequip");
        } finally {
            NeoForge.EVENT_BUS.unregister(veto);
        }
        menu.clicked(slot.index, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        h.assertTrue(
                slot.getItem().isEmpty(),
                "Storage pouch screen supports shift unequip after veto removed");
        h.succeed();
    }

    @GameTest(templateNamespace = "packingcube", template = "empty")
    public static void wornBackpackRealOpenerSupportsAllSourcesAndReassignment(GameTestHelper h) {
        var p = new RefillOpeningGameTests.OpeningPlayer(h);
        inventory(p).reset();
        var root = bag();
        new BagInventory(root, true, () -> {}).setItem(0, RefillOpeningGameTests.filledPouch());
        back(p).setStackInSlot(0, root);
        RefillOpeningGameTests.assertAllRulesAndReassignment(h, p, root);
        h.assertTrue(
                back(p).getStackInSlot(0).is(ModItems.LARGE_BAG.get()),
                "Worn owning backpack remains equipped");
        h.succeed();
    }
}
