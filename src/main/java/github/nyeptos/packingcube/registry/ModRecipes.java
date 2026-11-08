package github.nyeptos.packingcube.registry;

import com.mojang.serialization.MapCodec;

import github.nyeptos.packingcube.PackingCube;
import github.nyeptos.packingcube.craft.PouchRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The station's own recipe kind. Vanilla syncs every recipe to the client, so the panel and the
 * menu read the same list from the recipe manager.
 */
public final class ModRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, PackingCube.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PackingCube.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<PouchRecipe>> POUCH_TYPE =
            TYPES.register(
                    "pouch",
                    () ->
                            RecipeType.simple(
                                    ResourceLocation.fromNamespaceAndPath(
                                            PackingCube.MODID, "pouch")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PouchRecipe>>
            POUCH_SERIALIZER =
                    SERIALIZERS.register(
                            "pouch",
                            () ->
                                    new RecipeSerializer<PouchRecipe>() {
                                        @Override
                                        public MapCodec<PouchRecipe> codec() {
                                            return PouchRecipe.CODEC;
                                        }

                                        @Override
                                        public StreamCodec<RegistryFriendlyByteBuf, PouchRecipe>
                                                streamCodec() {
                                            return PouchRecipe.STREAM_CODEC;
                                        }
                                    });

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
