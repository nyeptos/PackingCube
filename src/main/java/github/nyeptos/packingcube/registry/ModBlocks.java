package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.block.PouchStationBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Placed blocks and the items that carry them. */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PackingCube.MODID);
    private static final DeferredRegister.Items BLOCK_ITEMS =
            DeferredRegister.createItems(PackingCube.MODID);

    public static final DeferredBlock<PouchStationBlock> POUCH_STATION =
            BLOCKS.register(
                    "pouch_station",
                    () ->
                            new PouchStationBlock(
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.WOOD)
                                            .strength(2.5F)
                                            .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> POUCH_STATION_ITEM =
            BLOCK_ITEMS.registerSimpleBlockItem("pouch_station", POUCH_STATION);

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ITEMS.register(bus);
    }
}
