package github.nyeptos.packingcube.data;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Packingcube.MODID, existingFiles);
    }

    @Override
    protected void registerModels() {
        // Share the artist's geometry/UV with the equipped model, without modifying its poses.
        // Keep display transforms in the Blockbench-authored parent, not in this wrapper.
        withExistingParent(
                BuiltInRegistries.ITEM.getKey(ModItems.LARGE_BAG.get()).getPath(),
                modLoc("item/backpack_equipped"));
        // Use the artist-supplied pouch icon in inventory and radial menus.
        flatItem(ModItems.FILTER_BAG.get(), modLoc("item/filter_bag"));
        // For a new mod texture at textures/item/<id>.png: basicItem(ModItems.YOUR_ITEM.get());
    }

    private void flatItem(Item item, ResourceLocation texture) {
        withExistingParent(BuiltInRegistries.ITEM.getKey(item).getPath(), mcLoc("item/generated"))
                .texture("layer0", texture);
    }
}
