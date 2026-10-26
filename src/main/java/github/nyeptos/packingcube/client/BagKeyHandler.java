package github.nyeptos.packingcube.client;

import com.mojang.blaze3d.platform.InputConstants;

import github.nyeptos.packingcube.Packingcube;
import github.nyeptos.packingcube.bag.input.BagKeyActions;
import github.nyeptos.packingcube.bag.input.BagKeyLatch;
import github.nyeptos.packingcube.network.OpenBigBagPayload;
import github.nyeptos.packingcube.network.OpenEquippedBagPayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Packingcube.MODID, value = Dist.CLIENT)
public final class BagKeyHandler {
    // New IDs intentionally do not reuse the old open_back_bag B assignment in options.txt.
    public static final KeyMapping OPEN_RADIAL =
            binding("key.packingcube.open_radial", GLFW.GLFW_KEY_C);
    public static final KeyMapping OPEN_BIG_BAG =
            binding("key.packingcube.open_big_bag", GLFW.GLFW_KEY_B);
    private static final BagKeyLatch INPUT_LATCH = new BagKeyLatch();

    private BagKeyHandler() {}

    public static void latch(BagKeyActions.Action action) {
        INPUT_LATCH.accept(action, sameBinding());
    }

    private static KeyMapping binding(String name, int defaultKey) {
        return new KeyMapping(
                name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                defaultKey,
                "key.categories.packingcube");
    }

    private static boolean consume(KeyMapping key) {
        boolean pressed = false;
        while (key.consumeClick()) pressed = true;
        return pressed;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        boolean radial = consume(OPEN_RADIAL);
        boolean direct = consume(OPEN_BIG_BAG);
        Minecraft minecraft = Minecraft.getInstance();
        INPUT_LATCH.refresh(bindingDown(), bindingDown(OPEN_BIG_BAG));
        if (minecraft.player == null || minecraft.screen != null) return;
        boolean same = sameBinding();
        var action = BagKeyActions.choose(radial, direct, same);
        if (!INPUT_LATCH.accept(action, same)) return;
        switch (action) {
            case RADIAL -> PacketDistributor.sendToServer(new OpenEquippedBagPayload());
            case DIRECT -> PacketDistributor.sendToServer(new OpenBigBagPayload());
            case NONE -> {}
        }
    }

    public static boolean sameBinding() {
        return !OPEN_RADIAL.isUnbound()
                && OPEN_RADIAL.getKey().equals(OPEN_BIG_BAG.getKey())
                && OPEN_RADIAL.getKeyModifier() == OPEN_BIG_BAG.getKeyModifier();
    }

    public static boolean modifierActive(KeyMapping key) {
        return key.getKeyModifier().isActive(KeyConflictContext.IN_GAME);
    }

    public static BagKeyActions.Action keyAction(int key, int scan) {
        return BagKeyActions.choose(
                OPEN_RADIAL.matches(key, scan) && modifierActive(OPEN_RADIAL),
                OPEN_BIG_BAG.matches(key, scan) && modifierActive(OPEN_BIG_BAG),
                sameBinding());
    }

    public static BagKeyActions.Action mouseAction(int button) {
        return BagKeyActions.choose(
                OPEN_RADIAL.matchesMouse(button) && modifierActive(OPEN_RADIAL),
                OPEN_BIG_BAG.matchesMouse(button) && modifierActive(OPEN_BIG_BAG),
                sameBinding());
    }

    /** KeyMapping.isDown becomes false in GUI contexts. Poll the actual remapped input instead. */
    public static boolean radialModifierActive() {
        return modifierActive(OPEN_RADIAL);
    }

    public static boolean bindingDown() {
        return bindingDown(OPEN_RADIAL);
    }

    public static boolean bindingDown(KeyMapping mapping) {
        var key = mapping.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getValue() < 0 || !modifierActive(mapping)) return false;
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM)
            return InputConstants.isKeyDown(window, key.getValue());
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            if (GLFW.glfwGetKeyScancode(code) == key.getValue())
                return InputConstants.isKeyDown(window, code);
        }
        return false;
    }
}
