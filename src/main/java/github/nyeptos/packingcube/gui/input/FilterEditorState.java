package github.nyeptos.packingcube.gui.input;

import github.nyeptos.packingcube.bag.filter.BagFilter;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only projection and local view state; no menu, registry item type or network dependency. */
public final class FilterEditorState {
    public record Rule(String field, String value) {}

    private boolean registered;
    private boolean excluding;
    private int page;
    private int pageSize = 1;
    private String query = "";
    private List<Rule> rows = List.of();

    public boolean registered() {
        return registered;
    }

    public boolean excluding() {
        return excluding;
    }

    public int page() {
        return page;
    }

    public String query() {
        return query;
    }

    public int pageCount() {
        return Math.max(1, (rows.size() + pageSize - 1) / pageSize);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < pageCount();
    }

    public boolean empty() {
        return rows.isEmpty();
    }

    public List<Rule> visibleRules() {
        int first = page * pageSize;
        return rows.subList(first, Math.min(rows.size(), first + pageSize));
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
        page = 0;
    }

    public void setQuery(String query) {
        this.query = query;
        page = 0;
    }

    public void setPageSize(int pageSize) {
        if (pageSize < 1) throw new IllegalArgumentException("A page needs at least one row");
        this.pageSize = pageSize;
        clampPage();
    }

    public void previous() {
        page--;
        clampPage();
    }

    public void next() {
        page++;
        clampPage();
    }

    private void clampPage() {
        page = Math.max(0, Math.min(page, pageCount() - 1));
    }

    public void refresh(ItemStack settings, ItemStack sample) {
        excluding = BagFilter.isExcluding(settings);
        List<Rule> result = new ArrayList<>();
        if (registered) {
            for (String field : List.of("items", "tags")) {
                for (String value : BagFilter.entries(settings, field))
                    result.add(new Rule(field, value));
            }
        } else if (!sample.isEmpty()) {
            sample.getTags()
                    .map(tag -> tag.location().toString())
                    .sorted()
                    .forEach(value -> result.add(new Rule("tags", value)));
        }
        String needle = query.toLowerCase(Locale.ROOT);
        rows =
                result.stream()
                        .filter(rule -> rule.value().toLowerCase(Locale.ROOT).contains(needle))
                        .toList();
        clampPage();
    }
}
