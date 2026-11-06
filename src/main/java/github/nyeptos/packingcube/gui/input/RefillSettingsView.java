package github.nyeptos.packingcube.gui.input;

import github.nyeptos.packingcube.bag.refill.RefillPouchSettings;
import github.nyeptos.packingcube.bag.refill.RefillSlotDestinations;
import github.nyeptos.packingcube.gui.layout.BagSettingsLayout;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    public RefillSlotDestinations.Rule rule(ItemStack settings) {
        return RefillSlotDestinations.get(settings, source);
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
        return page == FILTER && RefillPouchSettings.filterEnabled(settings);
    }

    public int contentHeight(BagSettingsLayout layout, ItemStack settings) {
        if (editorVisible(settings)) return layout.content().height();
        return page == FILTER ? 148 : 24 + rows() * 20 + 6;
    }

    public record HelpRow(int row, String key) {}

    /** Control rows on the current page in row order; spacers and plain labels are absent. */
    public List<HelpRow> helpRows(ItemStack settings) {
        List<HelpRow> rows = new ArrayList<>();
        rows.add(new HelpRow(0, "source"));
        rows.add(new HelpRow(1, "pages"));
        switch (page) {
            case DESTINATIONS -> {
                rows.add(new HelpRow(2, "enabled"));
                rows.add(new HelpRow(3, "select"));
                rows.add(new HelpRow(6, "clear"));
                rows.add(new HelpRow(7, "replace"));
                if (rule(settings).replace()) {
                    rows.add(new HelpRow(8, "collect"));
                    rows.add(new HelpRow(9, "lock"));
                }
                rows.add(new HelpRow(priorityRow(settings), "priority"));
            }
            case METHOD -> {
                rows.add(new HelpRow(3, "target"));
                rows.add(new HelpRow(4, "durability_mode"));
                if (!rule(settings).durabilityOff())
                    rows.add(new HelpRow(5, "durability_value"));
                rows.add(new HelpRow(7, "threshold"));
            }
            case FILTER -> {
                rows.add(new HelpRow(2, "filter"));
                rows.add(new HelpRow(3, "trade"));
                rows.add(new HelpRow(4, "pause"));
            }
            default -> throw new IllegalStateException();
        }
        return rows;
    }

    /** Priority sits on the last row so the replacement toggles never leave a gap. */
    public int priorityRow(ItemStack settings) {
        return rule(settings).replace() ? 10 : 8;
    }

    public String shape(ItemStack settings) {
        RefillSlotDestinations.Rule rule = rule(settings);
        return source
                + ":"
                + page
                + ":"
                + priorityExpanded
                + ":"
                + editorVisible(settings)
                + ":"
                + rule.replace()
                + ":"
                + rule.durabilityMode();
    }
}
