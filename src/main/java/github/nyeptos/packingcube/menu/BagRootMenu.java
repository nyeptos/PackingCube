package github.nyeptos.packingcube.menu;

import net.minecraft.world.item.ItemStack;

/** A menu that owns a root bag must be excluded from independent pickup writers. */
public interface BagRootMenu {
    boolean ownsRoot(ItemStack stack);
}
