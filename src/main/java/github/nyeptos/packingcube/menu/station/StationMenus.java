package github.nyeptos.packingcube.menu.station;

import github.nyeptos.packingcube.bag.BagSounds;
import github.nyeptos.packingcube.bag.station.StationBlocks;
import github.nyeptos.packingcube.menu.CraftingStationMenu;

import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Opens the GUI of a station carried in a module slot, without placing any block. */
public final class StationMenus {
    private StationMenus() {}

    public static boolean open(Player player, UUID rootId, int moduleSlot, ItemStack block) {
        if (player.level().isClientSide) return false;
        StationBlocks.Kind kind = StationBlocks.kind(block);
        if (kind == null || block.getCount() != 1) return false;
        if (kind == StationBlocks.Kind.CRAFTING_TABLE)
            return CraftingStationMenu.open(player, rootId, moduleSlot, block);
        boolean opened =
                player.openMenu(
                                new SimpleMenuProvider(
                                        (id, inventory, ignored) ->
                                                create(
                                                        kind,
                                                        id,
                                                        inventory,
                                                        rootId,
                                                        moduleSlot,
                                                        block),
                                        block.getHoverName()),
                                data -> {
                                    data.writeUUID(rootId);
                                    data.writeVarInt(moduleSlot);
                                })
                        .isPresent();
        if (opened) BagSounds.opened(player);
        return opened;
    }

    private static AbstractContainerMenu create(
            StationBlocks.Kind kind,
            int id,
            Inventory inventory,
            UUID rootId,
            int moduleSlot,
            ItemStack block) {
        return switch (kind) {
            case STONECUTTER -> new StonecutterStationMenu(id, inventory, rootId, moduleSlot, block);
            case GRINDSTONE -> new GrindstoneStationMenu(id, inventory, rootId, moduleSlot, block);
            case CARTOGRAPHY_TABLE ->
                    new CartographyStationMenu(id, inventory, rootId, moduleSlot, block);
            case LOOM -> new LoomStationMenu(id, inventory, rootId, moduleSlot, block);
            case SMITHING_TABLE ->
                    new SmithingStationMenu(id, inventory, rootId, moduleSlot, block);
            case ANVIL -> new AnvilStationMenu(id, inventory, rootId, moduleSlot, block);
            case FURNACE -> new FurnaceStationMenu(id, inventory, rootId, moduleSlot, block);
            case SMOKER -> new SmokerStationMenu(id, inventory, rootId, moduleSlot, block);
            case BLAST_FURNACE ->
                    new BlastFurnaceStationMenu(id, inventory, rootId, moduleSlot, block);
            case BREWING_STAND ->
                    new BrewingStandStationMenu(id, inventory, rootId, moduleSlot, block);
            case CRAFTING_TABLE -> throw new IllegalStateException("Crafting table has its own menu");
        };
    }
}
