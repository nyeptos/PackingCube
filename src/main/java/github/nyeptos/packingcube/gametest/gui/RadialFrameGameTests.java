package github.nyeptos.packingcube.gametest.gui;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.filter.BagFilter;
import github.nyeptos.packingcube.gui.radial.RadialGeometry;
import github.nyeptos.packingcube.gui.radial.RadialSelectionFeedback;
import github.nyeptos.packingcube.gui.layout.RadialLayout;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static github.nyeptos.packingcube.gametest.support.RadialTestSupport.*;

/** Frames, layout, previews, ghost slots, and hover feedback of the radial menu. */
@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class RadialFrameGameTests {
    @GameTest(template = "empty")
    public static void radialGeometrySelectsOnlyTheDrawnFrames(GameTestHelper h) {
        h.assertTrue(
                RadialGeometry.hit(0, 0, 0, 0, 72) == -1
                        && RadialGeometry.hit(0, 29, 0, 0, 72) == -1,
                "Center cancels");
        for (int i = 0; i < 9; i++)
            h.assertTrue(
                    RadialGeometry.hit(
                                    RadialGeometry.x(0, 72, i),
                                    RadialGeometry.y(0, 72, i),
                                    0,
                                    0,
                                    72)
                            == i,
                    "Every displayed frame selects its own slot");
        h.assertTrue(RadialGeometry.hit(0, -80, 0, 0, 72) == 0, "Slot one sits at twelve o'clock");
        h.assertTrue(
                RadialGeometry.hit(-17, -89, 0, 0, 72) == 0,
                "The frame corner still selects its slot");
        h.assertTrue(
                RadialGeometry.hit(-19, -89, 0, 0, 72) == -1,
                "One pixel outside the frame selects nothing");
        h.assertTrue(
                RadialGeometry.hit(Double.NaN, 0, 0, 0, 72) == -1,
                "Invalid coordinates cannot select a slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void gapsAndOuterSpaceAreNotSelectable(GameTestHelper h) {
        int radius = 72;
        for (int i = 0; i < 9; i++) {
            int next = (i + 1) % 9;
            double gapX =
                    (RadialGeometry.x(0, radius, i) + RadialGeometry.x(0, radius, next)) / 2.0;
            double gapY =
                    (RadialGeometry.y(0, radius, i) + RadialGeometry.y(0, radius, next)) / 2.0;
            h.assertTrue(
                    RadialGeometry.hit(gapX, gapY, 0, 0, radius) == -1,
                    "Gap " + (i + 1) + "-" + (next + 1) + " selects nothing");
            h.assertTrue(
                    RadialGeometry.hit(
                                    RadialGeometry.x(0, radius, i) * 1.6,
                                    RadialGeometry.y(0, radius, i) * 1.6,
                                    0,
                                    0,
                                    radius)
                            == -1,
                    "The area outside frame " + (i + 1) + " selects nothing");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void framesFollowTheLayoutAtEveryWindowSize(GameTestHelper h) {
        int[][] sizes = {
            {320, 180}, {320, 240}, {640, 360}, {854, 480}, {960, 540}, {1920, 1080}
        };
        for (int[] size : sizes) {
            RadialLayout layout = RadialLayout.fit(size[0], size[1]);
            int cx = layout.centerX(), cy = layout.centerY(), radius = layout.radius();
            for (int i = 0; i < 9; i++) {
                int x = RadialGeometry.x(cx, radius, i), y = RadialGeometry.y(cy, radius, i);
                h.assertTrue(
                        RadialGeometry.hit(x, y, cx, cy, radius) == i,
                        "Drawn frame center selects its own slot at " + size[0] + "x" + size[1]);
                h.assertTrue(
                        RadialGeometry.frameLeft(cx, radius, i)
                                        == x - RadialGeometry.FRAME_SIZE / 2
                                && RadialGeometry.frameTop(cy, radius, i)
                                        == y - RadialGeometry.FRAME_SIZE / 2,
                        "The drawn frame starts where the selectable frame starts");
            }
            h.assertTrue(
                    RadialGeometry.hit(cx, cy, cx, cy, radius) == -1,
                    "Center cancels at every window size");
            h.assertTrue(
                    layout.manageX() >= 0
                            && layout.manageX() + RadialLayout.MANAGE_WIDTH <= size[0]
                            && layout.manageY() + RadialLayout.MANAGE_HEIGHT <= size[1],
                    "The manage button stays on screen at " + size[0] + "x" + size[1]);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lowerDiagonalFramesHaveMoreSpaceAndKeepTheirTargets(GameTestHelper h) {
        for (int radius : new int[] {63, 67, 72}) {
            int rightGap =
                    RadialGeometry.x(0, radius, 3)
                            - RadialGeometry.x(0, radius, 4)
                            - RadialGeometry.FRAME_SIZE;
            int leftGap =
                    RadialGeometry.x(0, radius, 5)
                            - RadialGeometry.x(0, radius, 6)
                            - RadialGeometry.FRAME_SIZE;
            int oldGap =
                    (int) Math.round(Math.cos(RadialGeometry.angle(3)) * radius)
                            - RadialGeometry.x(0, radius, 4)
                            - RadialGeometry.FRAME_SIZE;
            h.assertTrue(
                    rightGap == leftGap && rightGap > oldGap && rightGap >= 3,
                    "Lower pairs have symmetric, wider gaps between their 36px frames");
            for (int i = 0; i < 9; i++) {
                h.assertTrue(
                        RadialGeometry.hit(
                                        RadialGeometry.x(0, radius, i),
                                        RadialGeometry.y(0, radius, i),
                                        0,
                                        0,
                                        radius)
                                == i,
                        "Shifted frame center still selects its own numbered slot");
                h.assertTrue(
                        Math.abs(RadialGeometry.x(0, radius, i))
                                        + RadialGeometry.FRAME_SIZE / 2
                                < 100,
                        "All frames remain inside the smallest standard backdrop");
                if (i != 3 && i != 6)
                    h.assertTrue(
                            RadialGeometry.x(0, radius, i)
                                    == (int) Math.round(Math.cos(RadialGeometry.angle(i)) * radius),
                            "Unrelated slot positions stay unchanged");
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialPreviewsKeepNamesAndRulesButNotContents(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag(), filter = module();
        filter.set(DataComponents.CUSTOM_NAME, Component.literal("Mining"));
        BagFilter.add(filter, "items", ResourceLocation.withDefaultNamespace("diamond"));
        storage(filter).setItem(0, new ItemStack(Items.DIAMOND, 15));
        modules(root).setItem(5, filter);
        BagRadialMenu menu = open(player, root);
        h.assertTrue(
                menu.icon(0).isEmpty() && !menu.icon(5).isEmpty(),
                "Empty positions are not compacted");
        h.assertTrue(
                menu.icon(5).getHoverName().getString().equals("Mining"),
                "Custom name is preserved");
        h.assertTrue(
                menu.icon(5).get(DataComponents.CONTAINER) == null,
                "No nested contents are sent as icons");
        h.assertTrue(
                BagFilter.entries(menu.icon(5), "items")
                        .equals(java.util.List.of("minecraft:diamond")),
                "Only filter settings accompany the preview name");
        h.assertTrue(
                storage(modules(root).getItem(5)).getItem(0).getCount() == 15,
                "Preview does not alter real contents");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialGhostSlotsRefuseEveryContainerAction(GameTestHelper h) {
        Player player = player(h);
        ItemStack root = bag();
        modules(root).setItem(0, module());
        BagRadialMenu menu = open(player, root);
        for (ClickType type : ClickType.values()) {
            menu.clicked(0, 0, type, player);
            menu.clicked(0, 1, type, player);
            menu.clicked(-999, 0, type, player);
        }
        h.assertTrue(
                menu.getCarried().isEmpty() && !menu.icon(0).isEmpty(),
                "Icons cannot be taken or thrown");
        h.assertTrue(
                menu.quickMoveStack(player, 0).isEmpty(), "Shift transfer cannot retrieve an icon");
        h.assertFalse(menu.getSlot(0).mayPickup(player), "Pickup denied");
        h.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Insertion denied");
        h.assertTrue(
                modules(root).getItem(0).is(ModItems.STORAGE_POUCH.get()),
                "Real module stays installed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void radialHoverFeedbackOnlyFiresWhenValidTargetChanges(GameTestHelper h) {
        var feedback = new RadialSelectionFeedback();
        h.assertFalse(feedback.moveTo(-1), "Opening at center is silent");
        h.assertTrue(feedback.moveTo(0), "Entering a bag clicks");
        h.assertFalse(feedback.moveTo(0), "Repeated frames and move events do not repeat sound");
        h.assertTrue(feedback.moveTo(4), "Changing bag clicks");
        h.assertFalse(feedback.moveTo(-1), "Entering center or an empty slot is silent");
        h.assertTrue(feedback.moveTo(4), "Returning to a bag clicks again");
        h.assertTrue(feedback.moveTo(9), "Management button is also a valid target");
        h.assertFalse(feedback.moveTo(9), "Stationary management target stays silent");
        h.assertFalse(feedback.moveTo(99), "Invalid target is silent");
        h.succeed();
    }
}
