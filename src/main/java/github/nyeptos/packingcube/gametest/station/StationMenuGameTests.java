package github.nyeptos.packingcube.gametest.station;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.bag.station.StationBlocks;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.menu.CraftingStationMenu;
import github.nyeptos.packingcube.bag.station.StationItems;
import github.nyeptos.packingcube.bag.station.StationTicker;
import github.nyeptos.packingcube.menu.station.AnvilStationMenu;
import github.nyeptos.packingcube.menu.station.BlastFurnaceStationMenu;
import github.nyeptos.packingcube.menu.station.BrewingStandStationMenu;
import github.nyeptos.packingcube.menu.station.CarriedStationMenu;
import github.nyeptos.packingcube.menu.station.FurnaceStationMenu;
import github.nyeptos.packingcube.menu.station.CartographyStationMenu;
import github.nyeptos.packingcube.menu.station.GrindstoneStationMenu;
import github.nyeptos.packingcube.menu.station.LoomStationMenu;
import github.nyeptos.packingcube.menu.station.SmokerStationMenu;
import github.nyeptos.packingcube.menu.station.SmithingStationMenu;
import github.nyeptos.packingcube.menu.station.StonecutterStationMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(PackingCube.MODID)
@PrefixGameTestTemplate(false)
public final class StationMenuGameTests {
    public static final class StationPlayer extends Player {
        public UUID openedRoot;
        public int openedModuleSlot = -1;
        private int nextId = 140;

        public StationPlayer(GameTestHelper h) {
            super(
                    h.getLevel(),
                    BlockPos.ZERO,
                    0,
                    new GameProfile(UUID.randomUUID(), "station-menu-test"));
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
                openedRoot = buffer.readUUID();
                openedModuleSlot = buffer.readVarInt();
                if (buffer.readableBytes() != 0)
                    throw new IllegalStateException("Unread opening data");
            } finally {
                buffer.release();
            }
            containerMenu = provider.createMenu(id, getInventory(), this);
            return OptionalInt.of(id);
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider) {
            int id = ++nextId;
            containerMenu = provider.createMenu(id, getInventory(), this);
            return OptionalInt.of(id);
        }
    }

    @GameTest(template = "empty")
    public static void theRadialKeyWorksWhileAStationIsOpen(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWith(StationBlocks.Kind.FURNACE);
        p.getInventory().setItem(0, root);
        openStation(h, p, root);
        h.assertTrue(
                p.containerMenu instanceof CarriedStationMenu, "A carried station panel is open");
        h.assertTrue(EquippedBagAccess.open(p), "The radial opens over a station panel");
        h.assertTrue(
                p.containerMenu instanceof BagRadialMenu, "The wheel replaced the station panel");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aBorrowedBlockPanelStillRefusesTheRadial(GameTestHelper h) {
        var p = new StationPlayer(h);
        p.getInventory().setItem(0, bagWith(StationBlocks.Kind.FURNACE));
        p.containerMenu =
                new ChestMenu(
                        MenuType.GENERIC_9x1, 90, p.getInventory(), new SimpleContainer(9), 1);
        h.assertTrue(
                !EquippedBagAccess.open(p), "A placed container still refuses the radial key");
        h.succeed();
    }

    private static ItemStack bagWith(StationBlocks.Kind kind) {
        ItemStack root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {}).setItem(0, new ItemStack(kind.item()));
        return root;
    }

    private static AbstractContainerMenu openStation(
            GameTestHelper h, StationPlayer p, ItemStack root) {
        var radial = new BagRadialMenu(70, p.getInventory(), root);
        p.containerMenu = radial;
        h.assertTrue(radial.select(p, 0), "The station opens from the radial");
        return p.containerMenu;
    }

    private static Class<? extends AbstractContainerMenu> expected(StationBlocks.Kind kind) {
        return switch (kind) {
            case CRAFTING_TABLE -> CraftingStationMenu.class;
            case STONECUTTER -> StonecutterStationMenu.class;
            case GRINDSTONE -> GrindstoneStationMenu.class;
            case CARTOGRAPHY_TABLE -> CartographyStationMenu.class;
            case LOOM -> LoomStationMenu.class;
            case SMITHING_TABLE -> SmithingStationMenu.class;
            case ANVIL -> AnvilStationMenu.class;
            case FURNACE -> FurnaceStationMenu.class;
            case SMOKER -> SmokerStationMenu.class;
            case BLAST_FURNACE -> BlastFurnaceStationMenu.class;
            case BREWING_STAND -> BrewingStandStationMenu.class;
        };
    }

    @GameTest(template = "empty")
    public static void everyStationFitsOneModuleSlot(GameTestHelper h) {
        for (var kind : StationBlocks.Kind.values()) {
            h.assertTrue(
                    BagInventory.accepts(true, new ItemStack(kind.item())),
                    kind + " fits a module slot");
            h.assertFalse(
                    BagInventory.accepts(true, new ItemStack(kind.item(), 2)),
                    kind + " stack does not fit");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyStationOpensItsOwnMenu(GameTestHelper h) {
        for (var kind : StationBlocks.Kind.values()) {
            var p = new StationPlayer(h);
            var root = bagWith(kind);
            p.getInventory().setItem(0, root);
            var menu = openStation(h, p, root);
            h.assertTrue(expected(kind).isInstance(menu), kind + " opens its own GUI");
            h.assertTrue(
                    p.openedRoot != null && p.openedModuleSlot == 0,
                    kind + " sends the module it was opened from");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aClosedFurnaceKeepsSmelting(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWith(StationBlocks.Kind.FURNACE);
        p.getInventory().setItem(0, root);
        var menu = openStation(h, p, root);
        menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 2));
        menu.getSlot(1).set(new ItemStack(Items.COAL, 1));
        menu.removed(p);
        p.containerMenu = p.inventoryMenu;
        for (int i = 0; i < 420; i++) StationTicker.tick(p);
        var stored = StationItems.stored(new BagInventory(root, true, () -> {}).getItem(0), 3);
        h.assertTrue(stored.get(0).isEmpty(), "The closed furnace used up the ore");
        h.assertTrue(
                stored.get(2).is(Items.IRON_INGOT) && stored.get(2).getCount() == 2,
                "Two ingots were waiting in the block");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aCookingStationStillOpensFromTheWheel(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWith(StationBlocks.Kind.FURNACE);
        p.getInventory().setItem(0, root);
        var menu = openStation(h, p, root);
        menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 2));
        menu.getSlot(1).set(new ItemStack(Items.COAL, 1));
        menu.removed(p);
        p.containerMenu = p.inventoryMenu;

        var radial = new BagRadialMenu(71, p.getInventory(), root);
        p.containerMenu = radial;
        for (int i = 0; i < 20; i++) StationTicker.tick(p);
        h.assertTrue(radial.select(p, 0), "A furnace that is cooking still opens from the wheel");
        h.assertTrue(
                p.containerMenu instanceof FurnaceStationMenu, "The cooking furnace opens its GUI");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aClosedBrewingStandBrewsItsPotion(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWith(StationBlocks.Kind.BREWING_STAND);
        p.getInventory().setItem(0, root);
        var menu = openStation(h, p, root);
        menu.getSlot(0).set(PotionContents.createItemStack(Items.POTION, Potions.WATER));
        menu.getSlot(3).set(new ItemStack(Items.NETHER_WART));
        menu.getSlot(4).set(new ItemStack(Items.BLAZE_POWDER));
        menu.removed(p);
        p.containerMenu = p.inventoryMenu;
        for (int i = 0; i < 430; i++) StationTicker.tick(p);
        var stored = StationItems.stored(new BagInventory(root, true, () -> {}).getItem(0), 5);
        var contents = stored.get(0).get(DataComponents.POTION_CONTENTS);
        h.assertTrue(
                contents != null && contents.is(Potions.AWKWARD),
                "The closed stand brewed the water into an awkward potion");
        h.assertTrue(stored.get(3).isEmpty(), "The wart was used up");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stationInputsSurviveReopening(GameTestHelper h) {
        for (var kind : StationBlocks.Kind.values()) {
            if (kind == StationBlocks.Kind.CRAFTING_TABLE) continue; // Covered by its own test.
            var p = new StationPlayer(h);
            var root = bagWith(kind);
            p.getInventory().setItem(0, root);
            var menu = openStation(h, p, root);
            for (int i = 0; i < kind.inputs(); i++)
                menu.getSlot(kind.firstInput() + i).set(new ItemStack(Items.STONE, 3 + i));
            h.assertTrue(
                    menu.getSlot(0).getItem().getCount() == 3, kind + " holds the item before close");
            menu.removed(p);
            var kept =
                    github.nyeptos.packingcube.bag.station.StationItems.stored(
                            new BagInventory(root, true, () -> {}).getItem(0), kind.inputs());
            h.assertTrue(
                    kept.get(0).getCount() == 3,
                    kind + " stored slot 0, saw " + kept.get(0).getCount());
            p.containerMenu = p.inventoryMenu;
            var reopened = openStation(h, p, root);
            reopened.broadcastChanges();
            for (int i = 0; i < kind.inputs(); i++)
                h.assertTrue(
                        reopened.getSlot(kind.firstInput() + i).getItem().getCount() == 3 + i,
                        kind + " kept slot " + i + ", saw "
                                + reopened.getSlot(kind.firstInput() + i).getItem().getCount());
        }
        h.succeed();
    }
}
