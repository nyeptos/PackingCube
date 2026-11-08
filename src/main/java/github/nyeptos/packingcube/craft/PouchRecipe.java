package github.nyeptos.packingcube.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import github.nyeptos.packingcube.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One pouch and the flat material bill the station charges for it. The bill is data, so a new
 * pouch is a json file rather than a code change.
 */
public record PouchRecipe(Item result, List<Material> materials, int order)
        implements Recipe<SingleRecipeInput> {
    /** A single bill line: the accepted item and how many of it the pouch costs. */
    public record Material(Item item, int count) {
        public static final Codec<Material> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                BuiltInRegistries.ITEM
                                                        .byNameCodec()
                                                        .fieldOf("item")
                                                        .forGetter(Material::item),
                                                Codec.intRange(1, 4096)
                                                        .fieldOf("count")
                                                        .forGetter(Material::count))
                                        .apply(instance, Material::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.ITEM),
                        Material::item,
                        ByteBufCodecs.VAR_INT,
                        Material::count,
                        Material::new);

        public ItemStack stack() {
            return new ItemStack(item, count);
        }
    }

    public static final MapCodec<PouchRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("result")
                                                    .forGetter(PouchRecipe::result),
                                            Material.CODEC
                                                    .listOf()
                                                    .fieldOf("materials")
                                                    .forGetter(PouchRecipe::materials),
                                            Codec.INT
                                                    .optionalFieldOf("order", 0)
                                                    .forGetter(PouchRecipe::order))
                                    .apply(instance, PouchRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PouchRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(Registries.ITEM),
                    PouchRecipe::result,
                    Material.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    PouchRecipe::materials,
                    ByteBufCodecs.VAR_INT,
                    PouchRecipe::order,
                    PouchRecipe::new);

    public ItemStack resultStack() {
        return new ItemStack(result);
    }

    /** Never matched by a grid: the station reads the bill and pays it out of the inventory. */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return resultStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.POUCH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.POUCH_TYPE.get();
    }
}
