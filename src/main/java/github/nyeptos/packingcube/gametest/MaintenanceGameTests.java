package github.nyeptos.packingcube.gametest;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.EquippedBagAccess;
import github.nyeptos.packingcube.menu.BagMenu;
import github.nyeptos.packingcube.menu.BagMenuOpener;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.network.FilterActions;
import github.nyeptos.packingcube.registry.ModCreativeTabs;
import github.nyeptos.packingcube.registry.ModItems;
import github.nyeptos.packingcube.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(Packingcube.MODID)
@PrefixGameTestTemplate(false)
public final class MaintenanceGameTests {
    /**
     * Only menu packet transport and notify-sound delivery are intercepted; menu/state logic is
     * real.
     */
    private static final class RecordingPlayer extends Player {
        private int nextId = 100;
        private int soundCount;
        private SoundEvent lastSound;
        private SoundSource lastSource;
        private float lastVolume, lastPitch;
        private boolean rejectMenus;

        RecordingPlayer(GameTestHelper helper) {
            super(
                    helper.getLevel(),
                    BlockPos.ZERO,
                    0,
                    new GameProfile(UUID.randomUUID(), "maintenance-test"));
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
        public OptionalInt openMenu(MenuProvider provider) {
            if (rejectMenus) return OptionalInt.empty();
            containerMenu = provider.createMenu(++nextId, getInventory(), this);
            return OptionalInt.of(containerMenu.containerId);
        }

        @Override
        public OptionalInt openMenu(
                MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            if (rejectMenus) return OptionalInt.empty();
            var buffer =
                    new RegistryFriendlyByteBuf(
                            io.netty.buffer.Unpooled.buffer(), level().registryAccess());
            try {
                extra.accept(buffer);
            } finally {
                buffer.release();
            }
            return openMenu(provider);
        }

        @Override
        public void playNotifySound(
                SoundEvent sound, SoundSource source, float volume, float pitch) {
            soundCount++;
            lastSound = sound;
            lastSource = source;
            lastVolume = volume;
            lastPitch = pitch;
        }
    }

    @GameTest(template = "empty")
    public static void registrySplitPreservesAllExistingIds(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(ModItems.LARGE_BAG.get()).equals(id("large_bag")),
                "Big Bag ID unchanged");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(ModItems.FILTER_BAG.get()).equals(id("filter_bag")),
                "Filter Bag ID unchanged");
        helper.assertTrue(
                BuiltInRegistries.MENU.getKey(ModMenus.BAG_MENU.get()).equals(id("bag")),
                "Storage menu ID unchanged");
        helper.assertTrue(
                BuiltInRegistries.MENU
                        .getKey(ModMenus.BAG_RADIAL_MENU.get())
                        .equals(id("bag_radial")),
                "Radial menu ID unchanged");
        helper.assertTrue(
                BuiltInRegistries.CREATIVE_MODE_TAB
                        .getKey(ModCreativeTabs.BAGS.get())
                        .equals(id("bags")),
                "Creative tab ID unchanged");
        helper.assertTrue(
                FilterActions.SAMPLE_SLOT == 0
                        && FilterActions.ADD_RULE == 1
                        && FilterActions.REMOVE_RULE == 2
                        && FilterActions.SET_MODE == 3
                        && FilterActions.SAMPLE_CURSOR == 4,
                "Existing filter wire IDs unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allBigBagOpenRoutesPlayLeatherExactlyOnce(GameTestHelper helper) {
        var player = new RecordingPlayer(helper);
        ItemStack root = ModItems.LARGE_BAG.get().getDefaultInstance();
        var contents = new BagInventory(root, true, () -> {});
        contents.setItem(0, ModItems.FILTER_BAG.get().getDefaultInstance());
        player.getInventory().setItem(0, root);
        helper.assertTrue(EquippedBagAccess.openDirect(player), "B opens Big Bag");
        helper.assertTrue(player.soundCount == 1, "Direct open has one sound");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, false),
                "B closes Big Bag");
        helper.assertTrue(player.soundCount == 1, "Closing does not play open sound");
        player.getInventory().selected = 0;
        root.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                player.containerMenu instanceof BagMenu && player.soundCount == 2,
                "Right-click open has one sound");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, true),
                "Open radial");
        helper.assertTrue(player.soundCount == 2, "Radial itself has no leather open sound");
        helper.assertTrue(
                ((BagRadialMenu) player.containerMenu).select(player, 0), "Select Filter Bag");
        helper.assertTrue(player.soundCount == 2, "Filter Bag does not inherit Big Bag open sound");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, false),
                "Filter B returns to Big Bag");
        helper.assertTrue(player.soundCount == 3, "Return to Big Bag has one sound");
        helper.assertTrue(
                EquippedBagAccess.navigate(player, player.containerMenu.containerId, true),
                "Open radial again");
        helper.assertTrue(
                ((BagRadialMenu) player.containerMenu).select(player, -1),
                "Management button opens Big Bag");
        helper.assertTrue(player.soundCount == 4, "Management button has one sound");
        helper.assertTrue(
                player.lastSound == SoundEvents.ARMOR_EQUIP_LEATHER.value()
                        && player.lastSource == SoundSource.PLAYERS
                        && player.lastVolume == 1.0F
                        && player.lastPitch == 1.0F,
                "Uses normal vanilla leather equip sound");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectedAndFilterOpensHaveNoBigBagSound(GameTestHelper helper) {
        var player = new RecordingPlayer(helper);
        ItemStack root = ModItems.LARGE_BAG.get().getDefaultInstance();
        player.getInventory().setItem(0, root);
        player.rejectMenus = true;
        helper.assertFalse(EquippedBagAccess.openDirect(player), "Failed menu open rejected");
        helper.assertTrue(player.soundCount == 0, "Failed open does not play sound");
        player.rejectMenus = false;
        player.containerMenu.setCarried(new ItemStack(Items.STONE));
        helper.assertFalse(EquippedBagAccess.openDirect(player), "Carried item rejects shortcut");
        helper.assertTrue(player.soundCount == 0, "Rejected request silent");
        player.containerMenu.setCarried(ItemStack.EMPTY);
        ItemStack filter = ModItems.FILTER_BAG.get().getDefaultInstance();
        player.getInventory().setItem(1, filter);
        var storage = new BagInventory(filter, false, () -> {});
        helper.assertTrue(
                BagMenuOpener.open(player, storage, filter, 1, false, filter.getHoverName()),
                "Filter menu still opens");
        helper.assertTrue(player.soundCount == 0, "Only Big Bag receives leather open sound");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Packingcube.MODID, path);
    }
}
