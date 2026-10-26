package github.nyeptos.packingcube.gui.skin;

/** One axis of a nine-slice. Source corners are preserved, or compressed only for tiny targets. */
public record NineSliceAxis(int source, int target, int border) {
    public NineSliceAxis {
        if (source < 1 || target < 1 || border < 0 || border * 2 >= source)
            throw new IllegalArgumentException("Invalid nine-slice axis");
    }

    public int targetBorder() {
        return Math.min(border, target / 2);
    }

    public int sourceStart(int part) {
        return switch (part) {
            case 0 -> 0;
            case 1 -> border;
            case 2 -> source - border;
            default -> throw new IndexOutOfBoundsException(part);
        };
    }

    public int sourceLength(int part) {
        return switch (part) {
            case 0, 2 -> border;
            case 1 -> source - border * 2;
            default -> throw new IndexOutOfBoundsException(part);
        };
    }

    public int targetStart(int part) {
        return switch (part) {
            case 0 -> 0;
            case 1 -> targetBorder();
            case 2 -> target - targetBorder();
            default -> throw new IndexOutOfBoundsException(part);
        };
    }

    public int targetLength(int part) {
        return switch (part) {
            case 0, 2 -> targetBorder();
            case 1 -> target - targetBorder() * 2;
            default -> throw new IndexOutOfBoundsException(part);
        };
    }
}
