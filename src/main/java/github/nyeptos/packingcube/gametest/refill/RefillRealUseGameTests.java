package github.nyeptos.packingcube.gametest.refill;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.gametest.support.LegacyRefillFixture;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Exercise server game-mode calls and Player.tick hooks, not a posted event plus manual shrink. */
@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillRealUseGameTests {
    private static final class Actor extends FakePlayer {
        Actor(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "RefillCheck"));
            setGameMode(GameType.SURVIVAL);
        }

        void finish() {
            completeUsingItem();
        }
    }

    private static ItemStack install(Actor p, ItemStack contents) {
        var supply = LegacyRefillFixture.pouch();
        new BagInventory(supply, false, () -> {}).setItem(0, contents);
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, supply);
        p.getInventory().setItem(8, root);
        p.getInventory().selected = 0;
        return root;
    }

    private static BlockHitResult target(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(0, 1, 0));
        h.getLevel().setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(base.above(), Blocks.AIR.defaultBlockState());
        return new BlockHitResult(Vec3.atCenterOf(base).add(0, 0.5, 0), Direction.UP, base, false);
    }

    @GameTest(template = "empty")
    public static void realServerPlacementLastMainHandItem(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        var root = install(p, new ItemStack(Items.STONE, 12));
        var hit = target(h);
        p.setPos(Vec3.atCenterOf(hit.getBlockPos()).add(3, 1, 0));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        var result =
                p.gameMode.useItemOn(
                        p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        h.assertTrue(
                result.consumesAction() && p.getMainHandItem().isEmpty(),
                "Real survival placement consumes final item");
        p.doTick();
        h.assertTrue(
                p.getMainHandItem().getCount() == 12,
                "Actual Player.Post tick refills consumed main hand");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void realServerPlacementLastOffhandItem(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        install(p, new ItemStack(Items.STONE, 12));
        var hit = target(h);
        p.setPos(Vec3.atCenterOf(hit.getBlockPos()).add(3, 1, 0));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
        var result =
                p.gameMode.useItemOn(
                        p, h.getLevel(), p.getOffhandItem(), InteractionHand.OFF_HAND, hit);
        h.assertTrue(
                result.consumesAction() && p.getOffhandItem().isEmpty(),
                "Real survival placement consumes offhand");
        p.doTick();
        h.assertTrue(
                p.getOffhandItem().getCount() == 12,
                "Actual Player.Post tick refills consumed offhand");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void realFoodFinishUsesPreConsumptionCopy(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        install(p, new ItemStack(Items.BREAD, 8));
        p.getFoodData().setFoodLevel(10);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        p.gameMode.useItem(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
        h.assertTrue(p.isUsingItem(), "Real game-mode use starts eating");
        p.finish();
        p.doTick();
        h.assertTrue(
                p.getMainHandItem().getCount() == 8,
                "Finish event survives actual final-food consumption");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directStackUseOnAlsoRefills(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        install(p, new ItemStack(Items.STONE, 7));
        var hit = target(h);
        p.setPos(Vec3.atCenterOf(hit.getBlockPos()).add(3, 1, 0));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        var result =
                p.getMainHandItem()
                        .useOn(
                                new net.minecraft.world.item.context.UseOnContext(
                                        p, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(
                result.consumesAction() && p.getMainHandItem().isEmpty(),
                "Direct useOn consumed final item without game-mode RightClickBlock");
        p.doTick();
        h.assertTrue(
                p.getMainHandItem().getCount() == 7,
                "Direct UseItemOnBlock/consumed event triggers refill");
        h.succeed();
    }

    /**
     * A client-side slot mirror with predicted consumption; receives only synchronizer messages.
     */
    private static final class ClientMirror
            implements net.minecraft.world.inventory.ContainerSynchronizer {
        final java.util.Map<Integer, ItemStack> slots = new java.util.HashMap<>();

        public void sendInitialData(
                net.minecraft.world.inventory.AbstractContainerMenu menu,
                net.minecraft.core.NonNullList<ItemStack> items,
                ItemStack carried,
                int[] data) {
            for (int i = 0; i < items.size(); i++) slots.put(i, items.get(i).copy());
        }

        public void sendSlotChange(
                net.minecraft.world.inventory.AbstractContainerMenu menu,
                int slot,
                ItemStack stack) {
            slots.put(slot, stack.copy());
        }

        public void sendCarriedChange(
                net.minecraft.world.inventory.AbstractContainerMenu menu, ItemStack stack) {}

        public void sendDataChange(
                net.minecraft.world.inventory.AbstractContainerMenu menu, int index, int value) {}
    }

    @GameTest(template = "empty")
    public static void sameCountRefillCorrectsPredictedMainHandConsumption(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        var root = install(p, new ItemStack(Items.STONE, 8));
        var modules = new BagInventory(root, true, () -> {});
        RefillSettings.set(modules.getItem(0), "timing", "use");
        RefillSettings.set(modules.getItem(0), "target", "16");
        modules.setChanged();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 16));
        var mirror = new ClientMirror();
        p.inventoryMenu.setSynchronizer(mirror);
        var hit = target(h);
        p.setPos(Vec3.atCenterOf(hit.getBlockPos()).add(3, 1, 0));
        var result =
                p.gameMode.useItemOn(
                        p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        h.assertTrue(
                result.consumesAction() && p.getMainHandItem().getCount() == 15,
                "Actual placement consumed one");
        mirror.slots.put(36, new ItemStack(Items.STONE, 15));
        p.doTick();
        h.assertTrue(
                p.getMainHandItem().getCount() == 16, "Server already refilled to original count");
        h.assertTrue(
                mirror.slots.get(36).getCount() == 16,
                "Client prediction must receive a correction even when final count equals last sent"
                        + " count");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lastOffhandItemRefillCorrectsPredictedEmptySlot(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        install(p, new ItemStack(Items.STONE));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
        var mirror = new ClientMirror();
        p.inventoryMenu.setSynchronizer(mirror);
        var hit = target(h);
        p.setPos(Vec3.atCenterOf(hit.getBlockPos()).add(3, 1, 0));
        p.gameMode.useItemOn(p, h.getLevel(), p.getOffhandItem(), InteractionHand.OFF_HAND, hit);
        h.assertTrue(p.getOffhandItem().isEmpty(), "Last offhand item consumed");
        mirror.slots.put(45, ItemStack.EMPTY);
        p.doTick();
        h.assertTrue(p.getOffhandItem().getCount() == 1, "Server restored one item from the pouch");
        h.assertTrue(
                mirror.slots.get(45).getCount() == 1,
                "Client must not stay visually empty after one-to-one refill");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mappedSourceRefillsRealPlacementAndCorrectsPrediction(GameTestHelper h) {
        var p = new Actor(h.getLevel());
        var root = install(p, new ItemStack(Items.STONE, 12));
        var parent = new BagInventory(root, true, () -> {});
        var pouch = parent.getItem(0);
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(
                pouch, 0, "destination", "inventory:0");
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(pouch, 0, "timing", "use");
        github.nyeptos.packingcube.bag.refill.RefillSlotSettings.set(pouch, 0, "target", "16");
        parent.setChanged();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 16));
        var mirror = new ClientMirror();
        p.inventoryMenu.setSynchronizer(mirror);
        var hit = target(h);
        p.setPos(Vec3.atCenterOf(hit.getBlockPos()).add(3, 1, 0));
        p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        mirror.slots.put(36, new ItemStack(Items.STONE, 15));
        p.doTick();
        h.assertTrue(
                p.getMainHandItem().getCount() == 16 && mirror.slots.get(36).getCount() == 16,
                "Mapped use dispatch runs from actual Post tick and corrects predicted count");
        h.succeed();
    }
}
