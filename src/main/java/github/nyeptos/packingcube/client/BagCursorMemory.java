package github.nyeptos.packingcube.client;

import github.nyeptos.packingcube.bag.input.BagCursorPosition;
import github.nyeptos.packingcube.bag.input.BagCursorRelease;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.lang.ref.WeakReference;

/** Release a bag's cursor at its remembered position, never via a visible center-and-restore. */
final class BagCursorMemory {
    private static final BagCursorPosition POSITION = new BagCursorPosition();
    private static WeakReference<ClientLevel> rememberedLevel = new WeakReference<>(null);

    private static void checkLevel(Minecraft minecraft) {
        if (rememberedLevel.get() != minecraft.level) {
            POSITION.clear();
            rememberedLevel = new WeakReference<>(minecraft.level);
        }
    }

    void added() {
        Minecraft minecraft = Minecraft.getInstance();
        checkLevel(minecraft);
        // Screen.added runs BEFORE Minecraft calls MouseHandler.releaseMouse.
        // GUI-to-GUI openings keep the current visible position, with no native warp at all.
        remember();
        if (minecraft.level == null || !minecraft.isWindowActive()) return;
        var window = minecraft.getWindow();
        var mouse = minecraft.mouseHandler;
        BagCursorRelease.restore(
                POSITION,
                mouse.isMouseGrabbed(),
                window.getScreenWidth(),
                window.getScreenHeight(),
                new BagCursorRelease.Backend() {
                    @Override
                    public void hide() {
                        // Exit disabled mode while still hidden: GLFW may restore its old center
                        // here.
                        GLFW.glfwSetInputMode(
                                window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
                    }

                    @Override
                    public void synchronize(BagCursorPosition.Point point) {
                        // Access is limited by META-INF/accesstransformer.cfg to these five fields.
                        // Publish logical coordinates now rather than waiting for an asynchronous
                        // GLFW callback.
                        mouse.mouseGrabbed = false;
                        mouse.xpos = point.x();
                        mouse.ypos = point.y();
                        mouse.accumulatedDX = 0;
                        mouse.accumulatedDY = 0;
                        mouse.setIgnoreFirstMove();
                    }

                    @Override
                    public void position(BagCursorPosition.Point point) {
                        GLFW.glfwSetCursorPos(window.getWindow(), point.x(), point.y());
                    }

                    @Override
                    public void show() {
                        // Hidden -> normal does not restore the disabled-mode center.
                        GLFW.glfwSetInputMode(
                                window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                        // The subsequent vanilla releaseMouse sees mouseGrabbed=false and is a
                        // no-op.
                    }
                });
    }

    void remember() {
        Minecraft minecraft = Minecraft.getInstance();
        checkLevel(minecraft);
        if (minecraft.level == null
                || !minecraft.isWindowActive()
                || minecraft.mouseHandler.isMouseGrabbed()) return;
        var window = minecraft.getWindow();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window.getWindow(), x, y);
            POSITION.remember(
                    x.get(0), y.get(0), window.getScreenWidth(), window.getScreenHeight());
        }
    }
}
