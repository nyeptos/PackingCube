package github.nyeptos.packingcube.gui.animation;

import net.minecraft.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event-based hover/press feedback shared by every slot in the bag screens. One instance drives
 * the hover sound, the press pulse and the overlay opacity, so a slot reacts once per entry
 * instead of once per frame.
 */
public final class SlotFeedback {
    private final Map<String, SlotButtonAnimation> animations = new HashMap<>();
    private String hovered;

    /** Returns true only when the pointer enters a different slot. */
    public boolean enter(String id) {
        if (Objects.equals(hovered, id)) return false;
        hovered = id;
        return id != null;
    }

    public String hovered() {
        return hovered;
    }

    public void press(String id) {
        if (id == null) return;
        animations.computeIfAbsent(id, ignored -> new SlotButtonAnimation()).press(Util.getMillis());
    }

    /** Zero frames stay allocation-free; only the hovered or pressed slots keep an animation. */
    public SlotButtonAnimation.Frame frame(String id, long nowMillis, boolean held) {
        SlotButtonAnimation animation = animations.get(id);
        if (animation == null) {
            if (!id.equals(hovered)) return new SlotButtonAnimation.Frame(0, 0);
            animation = animations.computeIfAbsent(id, ignored -> new SlotButtonAnimation());
        }
        return animation.sample(nowMillis, id.equals(hovered), held);
    }

    public void clear() {
        animations.clear();
        hovered = null;
    }
}
