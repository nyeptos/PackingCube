package github.nyeptos.packingcube.gametest.support;

import com.mojang.authlib.GameProfile;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.menu.BagRadialMenu;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

public final class RadialTestSupport {
    private RadialTestSupport() {}
    public static ItemStack bag() {
        return ModItems.LARGE_BAG.get().getDefaultInstance();
    }

    public static ItemStack module() {
        return ModItems.STORAGE_POUCH.get().getDefaultInstance();
    }

    public static BagInventory modules(ItemStack root) {
        return new BagInventory(root, true, () -> {});
    }

    public static BagInventory storage(ItemStack stack) {
        return new BagInventory(stack, false, () -> {});
    }

    /** Mock transport only; selector validation, menu creation, and saved data are real. */
    public static Player player(GameTestHelper helper) {
        return new Player(
                helper.getLevel(),
                BlockPos.ZERO,
                0,
                new GameProfile(UUID.randomUUID(), "radial-test")) {
            private int nextMenuId = 30;

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
                containerMenu = provider.createMenu(++nextMenuId, getInventory(), this);
                return OptionalInt.of(containerMenu.containerId);
            }

            @Override
            public OptionalInt openMenu(
                    MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
                var buf =
                        new RegistryFriendlyByteBuf(
                                io.netty.buffer.Unpooled.buffer(), level().registryAccess());
                try {
                    extra.accept(buf);
                } finally {
                    buf.release();
                }
                containerMenu = provider.createMenu(++nextMenuId, getInventory(), this);
                return OptionalInt.of(containerMenu.containerId);
            }
        };
    }

    public static BagRadialMenu open(Player player, ItemStack root) {
        player.getInventory().setItem(0, root);
        BagRadialMenu menu = new BagRadialMenu(20, player.getInventory(), root);
        player.containerMenu = menu;
        return menu;
    }

    public static String translationKey(Component component) {
        return component.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents value
                ? value.getKey()
                : "";
    }
}
