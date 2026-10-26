package github.nyeptos.packingcube.gametest.refill;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.refill.RefillDispatcher;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.menu.*;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class RefillOpeningGameTests {
    public static final class OpeningPlayer extends Player {
        public BagMenu mirror;
        private int nextId = 90;

        public OpeningPlayer(GameTestHelper h) {
            super(
                    h.getLevel(),
                    BlockPos.ZERO,
                    0,
                    new GameProfile(UUID.randomUUID(), "refill-open-test"));
        }

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
            int id = ++nextId;
            var buffer =
                    new RegistryFriendlyByteBuf(
                            io.netty.buffer.Unpooled.buffer(), level().registryAccess());
            try {
                extra.accept(buffer);
                mirror = BagMenu.fromNetwork(id, getInventory(), buffer);
                if (buffer.readableBytes() != 0)
                    throw new IllegalStateException("Unread opening data");
            } finally {
                buffer.release();
            }
            containerMenu = provider.createMenu(id, getInventory(), this);
            return OptionalInt.of(id);
        }
    }

    public static ItemStack filledPouch() {
        var pouch = ModItems.REFILL_POUCH.get().getDefaultInstance();
        var inv = new BagInventory(pouch, false, () -> {});
        for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(Items.TORCH, 20));
        return pouch;
    }

    public static void assertAllRulesAndReassignment(
            GameTestHelper h, OpeningPlayer p, ItemStack root) {
        var radial = new BagRadialMenu(70, p.getInventory(), root);
        p.containerMenu = radial;
        h.assertTrue(radial.select(p, 0), "Real radial opener opens nested pouch");
        var menu = (BagMenu) p.containerMenu;
        h.assertTrue(
                menu.isRefill() && p.mirror.isRefill(),
                "Server and opening-data client both identify nested refill pouch");
        for (int i = 0; i < 9; i++) {
            h.assertTrue(
                    menu.editFilter(
                            p,
                            FilterActions.SET_REFILL_SOURCE,
                            i,
                            "destination",
                            "inventory:" + (9 + i)),
                    "Initial assignment accepted for source " + (i + 1));
            h.assertTrue(
                    menu.editFilter(
                            p,
                            FilterActions.SET_REFILL_SOURCE,
                            i,
                            "destination",
                            "inventory:" + (9 + i)),
                    "Re-selecting saved target accepted");
            h.assertTrue(
                    menu.editFilter(
                            p,
                            FilterActions.SET_REFILL_SOURCE,
                            i,
                            "destination",
                            "inventory:" + (18 + i)),
                    "Changing saved target accepted for source " + (i + 1));
            h.assertTrue(
                    menu.editFilter(
                            p,
                            FilterActions.SET_REFILL_SOURCE,
                            i,
                            "target",
                            Integer.toString(i + 1)),
                    "Count setting accepted through real opener");
            h.assertTrue(
                    RefillSlotSettings.get(menu.filterSnapshot(), i)
                            .destination()
                            .equals("inventory:" + (18 + i)),
                    "Snapshot contains replaced destination");
        }
        p.containerMenu = p.inventoryMenu;
        // Reopening follows the persisted nested stack, not the old editing view.
        radial = new BagRadialMenu(71, p.getInventory(), root);
        p.containerMenu = radial;
        h.assertTrue(radial.select(p, 0), "Nested pouch reopens");
        menu = (BagMenu) p.containerMenu;
        for (int i = 0; i < 9; i++)
            h.assertTrue(
                    RefillSlotSettings.get(menu.filterSnapshot(), i)
                            .destination()
                            .equals("inventory:" + (18 + i)),
                    "Reassigned target persists after reopening");
        p.containerMenu = p.inventoryMenu;
        h.assertTrue(
                RefillDispatcher.tick(p, Set.of()) == 45,
                "All nine nested sources actually refill");
        for (int i = 0; i < 9; i++)
            h.assertTrue(
                    p.getInventory().getItem(18 + i).getCount() == i + 1
                            && p.getInventory().getItem(9 + i).isEmpty(),
                    "Only new target receives items");
    }

    @GameTest(template = "empty")
    public static void carriedBackpackRealOpenerSupportsAllSourcesAndReassignment(
            GameTestHelper h) {
        var p = new OpeningPlayer(h);
        var root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, filledPouch());
        p.getInventory().setItem(0, root);
        assertAllRulesAndReassignment(h, p, root);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void standalonePouchOpeningKindsMatchOnBothSides(GameTestHelper h) {
        for (boolean refill : new boolean[] {false, true}) {
            var p = new OpeningPlayer(h);
            var pouch =
                    (refill ? ModItems.REFILL_POUCH : ModItems.FILTER_BAG)
                            .get()
                            .getDefaultInstance();
            p.getInventory().setItem(0, pouch);
            h.assertTrue(
                    BagMenuOpener.open(
                            p,
                            new BagInventory(pouch, false, () -> {}),
                            pouch,
                            0,
                            false,
                            pouch.getHoverName()),
                    "Standalone pouch opens");
            h.assertTrue(
                    ((BagMenu) p.containerMenu).isRefill() == refill
                            && p.mirror.isRefill() == refill,
                    "Standalone kind matches on both sides");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void slotFeedbackAnimatesWithoutChangingPixelGeometry(GameTestHelper h) {
        var animation = new github.nyeptos.packingcube.gui.animation.SlotButtonAnimation();
        animation.sample(0, false, false);
        var hover = animation.sample(45, true, false);
        h.assertTrue(
                hover.hover() > .6F && hover.hover() < .7F && hover.press() == 0,
                "Hover opacity eases in");
        animation.press(50);
        h.assertTrue(
                animation.sample(50, true, true).press() == 1,
                "Held press feedback is immediately visible");
        animation.press(100); // Release/commit refreshes the short visual tail.
        h.assertTrue(
                animation.sample(180, false, false).press() > 0,
                "Click animation survives return to settings");
        var end = animation.sample(300, false, false);
        h.assertTrue(
                end.press() == 0 && end.hover() < hover.hover(),
                "Feedback fades out without lingering selected marks");
        h.succeed();
    }
}
