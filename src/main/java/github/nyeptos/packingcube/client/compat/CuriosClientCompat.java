package github.nyeptos.packingcube.client.compat;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Client-only optional boundary. Called only after confirming that Curios is loaded. */
public final class CuriosClientCompat {
    static final ModelResourceLocation EQUIPPED_MODEL =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Packingcube.MODID, "item/backpack_equipped"));

    private CuriosClientCompat() {}

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(EQUIPPED_MODEL);
    }

    public static void registerRenderer() {
        CuriosRendererRegistry.register(ModItems.LARGE_BAG.get(), CuriosBackpackRenderer::new);
    }
}
