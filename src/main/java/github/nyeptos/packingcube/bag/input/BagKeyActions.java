package github.nyeptos.packingcube.bag.input;

/** One request per input tick. Kept common so selection policy can be tested without a client. */
public final class BagKeyActions {
    public enum Action {
        NONE,
        RADIAL,
        DIRECT
    }

    private BagKeyActions() {}

    public static Action choose(
            boolean radialPressed, boolean directPressed, boolean sameBoundKey) {
        if (!radialPressed && !directPressed) return Action.NONE;
        // Duplicate physical bindings may deliver a click to only one KeyMapping. Still choose
        // radial.
        if (sameBoundKey || radialPressed) return Action.RADIAL;
        return Action.DIRECT;
    }
}
