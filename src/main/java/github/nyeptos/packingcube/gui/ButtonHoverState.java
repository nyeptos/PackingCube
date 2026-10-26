package github.nyeptos.packingcube.gui;

/** Mouse focus left over from a click is not a continuing hover. */
public final class ButtonHoverState {
    private boolean pointerInside;

    public record Feedback(boolean highlighted, boolean playSound) {}

    public Feedback update(
            boolean enabled, boolean pointerOver, boolean focused, boolean keyboardInput) {
        boolean inside = enabled && pointerOver;
        boolean sound = inside && !pointerInside;
        pointerInside = inside;
        return new Feedback(enabled && (pointerOver || (focused && keyboardInput)), sound);
    }

    public void reset() {
        pointerInside = false;
    }
}
