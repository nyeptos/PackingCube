package github.nyeptos.packingcube.registry;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.block.PouchStationBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PackingCube.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PouchStationBlockEntity>>
            POUCH_STATION =
                    TYPES.register(
                            "pouch_station",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    PouchStationBlockEntity::new,
                                                    ModBlocks.POUCH_STATION.get())
                                            .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
