package github.nyeptos.packingcube.gametest.compat;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class BagEquipmentGameTests {
    private static BagMenu open(Player player, boolean backpack) {
        ItemStack root =
                (backpack ? ModItems.LARGE_BAG : ModItems.FILTER_BAG).get().getDefaultInstance();
        player.getInventory().setItem(0, root);
        var menu =
                new BagMenu(
                        81,
                        player.getInventory(),
                        new BagInventory(root, backpack, () -> {}),
                        root,
                        0,
                        backpack);
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(template = "empty")
    public static void armorAndOffhandCanBeEquippedAndRemovedFromBothBags(GameTestHelper h) {
        for (boolean backpack : new boolean[] {false, true}) {
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            var menu = open(player, backpack);
            var head = menu.sideSlot("inventory:39");
            h.assertTrue(
                    head != null && !head.mayPlace(new ItemStack(Items.STONE)),
                    "Vanilla armor validity is reused");
            menu.setCarried(new ItemStack(Items.DIAMOND_HELMET));
            menu.clicked(head.index, 0, ClickType.PICKUP, player);
            h.assertTrue(
                    menu.getCarried().isEmpty()
                            && player.getInventory().getItem(39).is(Items.DIAMOND_HELMET),
                    "Helmet equipped through normal menu click");
            menu.clicked(head.index, 0, ClickType.PICKUP, player);
            h.assertTrue(
                    menu.getCarried().is(Items.DIAMOND_HELMET) && head.getItem().isEmpty(),
                    "Helmet removed onto cursor");
            var offhand = menu.sideSlot("inventory:40");
            menu.setCarried(new ItemStack(Items.TORCH, 20));
            menu.clicked(offhand.index, 1, ClickType.PICKUP, player);
            h.assertTrue(
                    offhand.getItem().getCount() == 1 && menu.getCarried().getCount() == 19,
                    "Right click equips one offhand item");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(offhand.index, 0, ClickType.QUICK_MOVE, player);
            h.assertTrue(
                    offhand.getItem().isEmpty()
                            && player.getInventory().contains(new ItemStack(Items.TORCH)),
                    "Shift unequip returns item to ordinary inventory, not pouch");
            h.assertTrue(
                    menu.visibleSlotCount() == 45 && !menu.getSlot(45).mayPickup(player),
                    "Original editor indices remain protected");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bindingCurseAndFullInventoryCannotBypassEquipmentRules(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = open(player, false);
        var head = menu.sideSlot("inventory:39");
        var helmet = new ItemStack(Items.IRON_HELMET);
        helmet.enchant(
                h.getLevel()
                        .registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolderOrThrow(
                                net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE),
                1);
        player.getInventory().setItem(39, helmet);
        menu.clicked(head.index, 0, ClickType.PICKUP, player);
        h.assertTrue(
                menu.getCarried().isEmpty() && head.getItem() == helmet,
                "Binding curse cannot be removed with ordinary click");
        menu.clicked(head.index, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(head.getItem() == helmet, "Binding curse cannot be shift-removed");
        player.getInventory().setItem(39, new ItemStack(Items.IRON_HELMET));
        for (int i = 1; i < 36; i++)
            player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        menu.clicked(head.index, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(
                head.getItem().is(Items.IRON_HELMET),
                "Full ordinary inventory leaves equipment in place");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void equipmentDoesNotPolluteFilterSamplesOrPouchShiftTargets(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = open(player, false);
        menu.setCarried(new ItemStack(Items.STONE));
        menu.clicked(menu.sideSlot("inventory:39").index, 0, ClickType.PICKUP, player);
        h.assertTrue(
                menu.getCarried().is(Items.STONE) && player.getInventory().getItem(39).isEmpty(),
                "Invalid armor is not consumed");
        h.assertTrue(menu.sample().isEmpty(), "Equipment never writes filter sample");
        menu.clicked(45, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().is(Items.STONE), "Snapshot remains non-interactive");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openingTransportPinsCuriosIdentityAndClientMirror(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var buffer =
                new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            buffer.writeVarInt(0);
            buffer.writeBoolean(false);
            buffer.writeBoolean(false);
            var descriptors =
                    java.util.List.of(
                            new github.nyeptos.packingcube.compat.BagEquipment.Target(
                                    "curios:back:0",
                                    net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                                            "item/empty_armor_slot_chestplate"),
                                    ItemStack.EMPTY),
                            new github.nyeptos.packingcube.compat.BagEquipment.Target(
                                    "curios:custom_type:2",
                                    net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                                            "item/empty_armor_slot_helmet"),
                                    ItemStack.EMPTY));
            BagMenu.writeCuriosTargets(buffer, descriptors);
            var mirror = BagMenu.fromNetwork(81, player.getInventory(), buffer);
            h.assertTrue(
                    buffer.readableBytes() == 0 && mirror.curiosTargets().size() == 2,
                    "Client receives server Curios layout and icons");
            h.assertTrue(
                    mirror.sideSlot("curios:custom_type:2").index == 53,
                    "Stable type/index matches exact server menu slot order");
            mirror.sideSlot("curios:custom_type:2").set(new ItemStack(Items.APPLE, 3));
            h.assertTrue(
                    mirror.sideSlot("curios:custom_type:2").getItem().getCount() == 3,
                    "Menu synchronization updates mirror even before external Curios sync");
        } finally {
            buffer.release();
        }
        h.succeed();
    }
}
