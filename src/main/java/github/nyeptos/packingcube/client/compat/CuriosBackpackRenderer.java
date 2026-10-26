package github.nyeptos.packingcube.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Draws the artist's baked item model relative to the animated player torso. */
final class CuriosBackpackRenderer implements ICurioRenderer {
    // Model coordinates: x=3..13, y=0..11, z=5..12.1 (sixteenths of a block).
    // ItemRenderer centers these around (8,8,8). Entity-model Y points down.
    // Result relative to torso: top=2px, bottom=13px, inner face=3px behind torso.
    private static final double OFFSET_Y = 2.0 / 16.0;
    private static final double OFFSET_Z = 5.0 / 16.0;
    private static final float SCALE = 1.0F;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext context,
            PoseStack pose,
            RenderLayerParent<T, M> parent,
            MultiBufferSource buffers,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (!context.identifier().equals("back")
                || context.cosmetic()
                || !context.visible()
                || !(context.entity() instanceof Player wearer)
                || wearer.isSpectator()
                || wearer.isInvisible()
                || !(parent.getModel() instanceof HumanoidModel<?> humanoid)) return;
        var inventory = CuriosApi.getCuriosInventory(wearer).orElse(null);
        if (inventory == null || !inventory.isSlotActive("back", context.index())) return;

        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(CuriosClientCompat.EQUIPPED_MODEL);
        pose.pushPose();
        try {
            // Use the already animated torso; do not also apply a second crouching transform.
            humanoid.body.translateAndRotate(pose);
            pose.translate(0, OFFSET_Y, OFFSET_Z);
            pose.scale(-SCALE, -SCALE, SCALE);
            // NONE intentionally ignores the exported third-person hand display transforms.
            minecraft
                    .getItemRenderer()
                    .render(
                            stack,
                            ItemDisplayContext.NONE,
                            false,
                            pose,
                            buffers,
                            light,
                            OverlayTexture.NO_OVERLAY,
                            model);
        } finally {
            pose.popPose();
        }
    }
}
