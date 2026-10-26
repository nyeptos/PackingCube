package github.nyeptos.packingcube.gui.input;

import github.nyeptos.packingcube.bag.refill.RefillSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotSettings;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;

import net.minecraft.world.item.ItemStack;

/** Screen-local progressive disclosure. Never changes item configuration when navigating. */
public final class RefillSettingsView {
    public static final int DESTINATIONS = 0, METHOD = 1, FILTER = 2;
    private int page;
    private int source;

    public int source() {
        return source;
    }

    public void source(int value) {
        if (value < 0 || value >= 9) throw new IllegalArgumentException("Invalid source slot");
        source = value;
    }

    public RefillSlotSettings.Rule rule(ItemStack settings) {
        return RefillSlotSettings.get(settings, source);
    }

    private boolean priorityExpanded;

    public int page() {
        return page;
    }

    public void page(int value) {
        if (value < DESTINATIONS || value > FILTER)
            throw new IllegalArgumentException("Invalid refill page");
        page = value;
    }

    public boolean priorityExpanded() {
        return priorityExpanded;
    }

    public void togglePriority() {
        priorityExpanded = !priorityExpanded;
    }

    public int rows() {
        return page == FILTER ? 6 : 11;
    }

    public boolean editorVisible(ItemStack settings) {
        return page == FILTER && RefillSettings.filterEnabled(settings);
    }

    public boolean thresholdVisible(ItemStack settings) {
        return page == METHOD && rule(settings).timing().equals("threshold");
    }

    public boolean targetVisible(ItemStack settings) {
        return page == METHOD && rule(settings).target() != 0;
    }

    public int contentHeight(BagSettingsLayout layout, ItemStack settings) {
        return editorVisible(settings) ? layout.content().height() : page == FILTER ? 148 : 250;
    }

    public String shape(ItemStack settings) {
        return source
                + ":"
                + page
                + ":"
                + priorityExpanded
                + ":"
                + editorVisible(settings)
                + ":"
                + thresholdVisible(settings)
                + ":"
                + targetVisible(settings);
    }
}
