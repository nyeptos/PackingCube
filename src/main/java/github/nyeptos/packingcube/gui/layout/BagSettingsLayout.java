package github.nyeptos.packingcube.gui.layout;

/**
 * Inventory/close bounds are local to the inventory origin. Settings bounds use a separate sidebar
 * origin independent of the inventory, so opening settings never moves real slots.
 */
public record BagSettingsLayout(int width, int height, int inventoryHeight, int optionRows) {
    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public int centerX() {
            return x + width / 2;
        }

        public boolean contains(double x, double y) {
            return x >= this.x && x < right() && y >= this.y && y < bottom();
        }
    }

    public BagSettingsLayout(int width, int height, int optionRows) {
        this(width, height, 132, optionRows);
    }

    public BagSettingsLayout {
        validate(inventoryHeight, optionRows);
        if (width < 240 || height < 48)
            throw new IllegalArgumentException("Settings viewport is too small");
    }

    private static void validate(int inventoryHeight, int optionRows) {
        if (inventoryHeight < 0 || inventoryHeight > 1024 || optionRows < 0 || optionRows > 16)
            throw new IllegalArgumentException("Unsupported inventory height or option rows");
    }

    /** The inventory always keeps the ordinary pouch screen's center and size. */
    public static Rect inventoryAnchor(int screenWidth, int screenHeight) {
        return new Rect((screenWidth - 176) / 2, (screenHeight - 132) / 2, 176, 132);
    }

    public static boolean fitsBesideInventory(int screenWidth, int inventoryLeft) {
        return screenWidth - inventoryLeft - 8 >= 300;
    }

    public static BagSettingsLayout besideInventory(
            int screenWidth, int screenHeight, int inventoryLeft, int optionRows) {
        if (!fitsBesideInventory(screenWidth, inventoryLeft))
            throw new IllegalArgumentException("Not enough space beside inventory");
        return new BagSettingsLayout(
                Math.min(420, screenWidth - inventoryLeft - 8),
                preferredHeight(screenHeight, optionRows),
                optionRows);
    }

    /** Floating content may be smaller than the inventory; overflow is handled by scrolling. */
    public static BagSettingsLayout floating(int panelWidth, int panelHeight, int optionRows) {
        return new BagSettingsLayout(176 + panelWidth, panelHeight, optionRows);
    }

    public int settingsTop(int screenHeight) {
        return (screenHeight - height) / 2;
    }

    public static int preferredWidth(int screenWidth) {
        return Math.min(420, Math.max(300, screenWidth - 8));
    }

    public static int preferredHeight(int screenHeight, int optionRows) {
        return preferredHeight(screenHeight, 132, optionRows);
    }

    public static int preferredHeight(int screenHeight, int inventoryHeight, int optionRows) {
        // The left inventory keeps its own height; only the right canvas expands.
        return Math.max(
                minimumHeight(inventoryHeight, optionRows),
                Math.min(contentBottom(optionRows) + 8, screenHeight - 8));
    }

    public static int minimumHeight(int optionRows) {
        return minimumHeight(132, optionRows);
    }

    public static int minimumHeight(int inventoryHeight, int optionRows) {
        validate(inventoryHeight, optionRows);
        return Math.max(112, inventoryHeight);
    }

    public Rect closeButton() {
        return new Rect(156, 2, 14, 14);
    }

    public Rect inventory() {
        return new Rect(0, 0, 176, inventoryHeight);
    }

    /** Only the right panel; never draw a settings backdrop behind the inventory. */
    public Rect settingsBackground() {
        return new Rect(inventory().right(), 0, width - inventory().right(), height);
    }

    /** Fixed drag handle, outside the scrolling content. */
    public Rect dragHandle() {
        return new Rect(176, 0, width - 176, 20);
    }

    /** Visible area, including the scrollbar gutter. */
    public Rect viewport() {
        return new Rect(180, 24, width - 188, height - 32);
    }

    /** Logical, unscrolled content area. Reserve ten pixels for the scrollbar. */
    public Rect content() {
        return new Rect(180, 24, width - 198, contentBottom() - 24);
    }

    public Rect optionRow(int index) {
        if (index < 0 || index >= optionRows) throw new IndexOutOfBoundsException(index);
        return new Rect(content().x(), 24 + index * 20, content().width(), 18);
    }

    public int modeY() {
        return 24 + optionRows * 20;
    }

    public int sampleLabelY() {
        return modeY() + 26;
    }

    public Rect sampleSlot() {
        return new Rect(content().x(), modeY() + 38, 18, 18);
    }

    public int sampleHelpY() {
        return sampleSlot().bottom() + 4;
    }

    public int addItemY() {
        return sampleHelpY() + 22;
    }

    public int tabsY() {
        return addItemY() + 26;
    }

    public int searchY() {
        return tabsY() + 26;
    }

    /** Rule pages remain bounded; the complete settings form has its own viewport scroll. */
    public int rulePageSize() {
        return 6;
    }

    public int ruleRowY(int index) {
        if (index < 0 || index >= rulePageSize()) throw new IndexOutOfBoundsException(index);
        return searchY() + 23 + index * 19;
    }

    public int pagerY() {
        return ruleRowY(0) + rulePageSize() * 19 + 4;
    }

    public int contentBottom() {
        return contentBottom(optionRows);
    }

    private static int contentBottom(int rows) {
        return 325 + rows * 20;
    }
}
