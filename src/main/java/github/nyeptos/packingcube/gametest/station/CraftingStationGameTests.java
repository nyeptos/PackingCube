package github.nyeptos.packingcube.gametest.station;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.menu.CraftingStationMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingStationGameTests {
    public static final class StationPlayer extends Player {
        public CraftingStationMenu mirror;
        private int nextId = 120;

        public StationPlayer(GameTestHelper h) {
            super(
                    h.getLevel(),
                    BlockPos.ZERO,
                    0,
                    new GameProfile(UUID.randomUUID(), "station-test"));
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
                mirror = CraftingStationMenu.fromNetwork(id, getInventory(), buffer);
                if (buffer.readableBytes() != 0)
                    throw new IllegalStateException("Unread opening data");
            } finally {
                buffer.release();
            }
            containerMenu = provider.createMenu(id, getInventory(), this);
            return OptionalInt.of(id);
        }
    }

    private static ItemStack bagWithTable(int moduleSlot) {
        ItemStack root = ModItems.LARGE_BAG.get().getDefaultInstance();
        new BagInventory(root, true, () -> {})
                .setItem(moduleSlot, new ItemStack(Items.CRAFTING_TABLE));
        return root;
    }

    private static CraftingStationMenu openTable(
            GameTestHelper h, StationPlayer p, ItemStack root) {
        var radial = new BagRadialMenu(70, p.getInventory(), root);
        p.containerMenu = radial;
        h.assertTrue(radial.select(p, 0), "Selecting the carried crafting table opens its GUI");
        return (CraftingStationMenu) p.containerMenu;
    }

    @GameTest(template = "empty")
    public static void onlyOneCraftingTableFitsAModuleSlot(GameTestHelper h) {
        h.assertTrue(
                BagInventory.accepts(true, new ItemStack(Items.CRAFTING_TABLE)),
                "A single crafting table fits a module slot");
        h.assertFalse(
                BagInventory.accepts(true, new ItemStack(Items.CRAFTING_TABLE, 2)),
                "A stack of tables does not");
        h.assertFalse(
                BagInventory.accepts(true, new ItemStack(Items.ENCHANTING_TABLE)),
                "Blocks outside the station list stay out of module slots");
        h.assertTrue(
                BagInventory.accepts(true, ModItems.STORAGE_POUCH.get().getDefaultInstance()),
                "Pouches still fit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theRadialOpensTheCarriedCraftingTable(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWithTable(0);
        p.getInventory().setItem(0, root);
        var radial = new BagRadialMenu(70, p.getInventory(), root);
        p.containerMenu = radial;
        h.assertTrue(radial.icon(0).is(Items.CRAFTING_TABLE), "The radial previews the block");
        h.assertTrue(radial.select(p, 0), "The station opens");
        h.assertTrue(
                p.containerMenu instanceof CraftingStationMenu,
                "The station menu replaces the radial");
        h.assertTrue(p.mirror != null, "Opening data rebuilds the menu on the client");
        h.assertTrue(
                p.mirror.slots.size() == p.containerMenu.slots.size(),
                "Both sides agree on the slot layout");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theGridStaysOnTheBlockStack(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWithTable(0);
        p.getInventory().setItem(0, root);
        var menu = openTable(h, p, root);
        menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS, 4));
        var stored =
                new BagInventory(root, true, () -> {})
                        .getItem(0)
                        .getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        .stream()
                        .toList();
        h.assertTrue(
                stored.get(0).getCount() == 4,
                "The block itself carries the grid it was left with");
        p.containerMenu = p.inventoryMenu;
        var reopened = openTable(h, p, root);
        h.assertTrue(
                reopened.getSlot(1).getItem().getCount() == 4,
                "Reopening the table restores the stored grid");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theStoredGridStillCrafts(GameTestHelper h) {
        var p = new StationPlayer(h);
        var root = bagWithTable(0);
        p.getInventory().setItem(0, root);
        var menu = openTable(h, p, root);
        for (int row = 0; row < 2; row++)
            for (int column = 0; column < 2; column++)
                menu.getSlot(1 + column + row * 3).set(new ItemStack(Items.OAK_PLANKS));
        CraftingInput input = menu.grid().asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> found =
                h.getLevel()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(found.isPresent(), "The stored grid matches a recipe");
        h.assertTrue(
                found.get().value().assemble(input, h.getLevel().registryAccess())
                        .is(Items.CRAFTING_TABLE),
                "Planks left in the grid still make a table");
        h.succeed();
    }
}
