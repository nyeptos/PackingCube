package github.nyeptos.packingcube.data;

import github.nyeptos.packingcube.Packingcube;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Paired translations keep both supported locales on the same set of keys. */
public final class ModLanguageProvider extends LanguageProvider {
    public enum Locale {
        EN_US("en_us"),
        JA_JP("ja_jp");

        private final String id;

        Locale(String id) {
            this.id = id;
        }
    }

    private final Locale locale;

    public ModLanguageProvider(PackOutput output, Locale locale) {
        super(output, Packingcube.MODID, locale.id);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        translated("itemGroup.packingcube", "PackingCube", "PackingCube");
        translated("item.packingcube.large_bag", "Backpack", "バックパック");
        translated("item.packingcube.filter_bag", "Small Storage Pouch", "小さな収納ポーチ");
        translated(
                "message.packingcube.hold_filter",
                "Hold a Small Storage Pouch in your main hand.",
                "小さな収納ポーチをメインハンドに持ってください。");
        translated(
                "message.packingcube.close_menu",
                "Close the bag screen before editing filters.",
                "バッグ画面を閉じてからフィルターを編集してください。");
        translated("message.packingcube.unknown_item", "Unknown item: %s", "存在しないアイテムです: %s");
        translated(
                "message.packingcube.filter_added",
                "Filter added: %s = %s",
                "フィルターを追加しました: %s = %s");
        translated(
                "message.packingcube.filter_cleared",
                "Filter reset to include mode with no rules. Stored items were not changed.",
                "フィルターを初期化しました（含む・未登録）。収納済みの中身は変更されません。");
        translated(
                "message.packingcube.legacy_overflow",
                "Recovered %s items from the old bag slots. %s items are still retained; free"
                        + " inventory space and reopen the bag.",
                "旧仕様の超過スロットから%s個を回収しました。保留分は%s個です。保留分がある場合はインベントリを空けてバッグを開き直してください。");
        translated("gui.packingcube.settings", "Settings", "設定");
        translated("gui.packingcube.back", "Back", "戻る");
        translated("gui.packingcube.allow", "Include", "含む");
        translated("gui.packingcube.exclude", "Exclude", "含まない");
        translated("gui.packingcube.add_item", "Add this item", "このアイテムを登録");
        translated("gui.packingcube.tags", "Item tags", "タグ候補");
        translated("gui.packingcube.registered", "Rules", "登録済み");
        translated("gui.packingcube.search", "Search IDs", "IDで絞り込み");
        translated("gui.packingcube.sample", "Sample (not consumed)", "見本（消費しません）");
        translated("gui.packingcube.no_sample", "No sample selected", "見本未選択");
        translated(
                "gui.packingcube.sample_help",
                "Hold an item and click the sample slot. Nothing is consumed. Right-click to"
                        + " clear.",
                "アイテムを持って見本枠をクリック。元の個数は減りません。右クリックで解除。");
        translated(
                "gui.packingcube.rules_help",
                "Shows the current mode. Click to switch Include/Exclude without clearing rules."
                        + " Manual insertion is filtered too.",
                "現在のモードを表示しています。クリックで含む／含まないを切り替えます。登録条件は保持し、手動収納にも適用します。");
        translated("gui.packingcube.empty_tag", "No matching items", "対象アイテムなし");
        translated("gui.packingcube.tag_count", "Matches %s items", "対象: %s種類");
        translated(
                "gui.packingcube.no_rules",
                "No include rules: nothing can be inserted.",
                "含む条件が未登録のため、収納しません。");
        translated(
                "gui.packingcube.no_tags", "No tag candidates. Select a sample.", "見本のタグ候補はありません。");
        translated(
                "message.packingcube.filter_limit",
                "A filter supports up to %s rules in total.",
                "フィルターは合計%s件まで登録できます。");
        translated(
                "gui.packingcube.no_exclude_rules",
                "No exclude rules: all otherwise eligible items can be inserted.",
                "含まない条件が未登録のため、収納可能な全アイテムを受け入れます。");
        translated(
                "message.packingcube.mode_changed",
                "Filter mode changed to %s.",
                "フィルターモードを%sに変更しました。");
        translated(
                "gui.packingcube.ghost_help",
                "Sample slot: left-click to copy the cursor item, right-click to clear. No physical"
                        + " items are stored.",
                "見本スロット：左クリックでカーソルのアイテムをコピー。右クリックで解除。実物は収納しません。");
        translated(
                "gui.packingcube.sample_short_help", "Left: copy / Right: clear", "左: コピー / 右: 解除");
        translated("gui.packingcube.pickup_on", "Auto pickup: ON", "自動収納: ON");
        translated("gui.packingcube.pickup_off", "Auto pickup: OFF", "自動収納: OFF");
        translated("gui.packingcube.pickup_priority", "Priority: %s", "優先度: %s");
        translated(
                "gui.packingcube.pickup_priority_help",
                "0-9: higher first across all backpacks. Ties: equipped backpack, inventory order,"
                        + " then left to right. Full pouches pass the remainder on.",
                "0〜9。大きい値を優先。同値なら装備中→インベントリ順→左から右。満杯なら次のポーチへ送ります。");
        translated("gui.packingcube.filter_mode", "Mode: %s", "モード: %s");
        translated("gui.packingcube.pickup_pouch_on", "Pouch pickup: ON", "このポーチの自動収納: ON");
        translated("gui.packingcube.pickup_pouch_off", "Pouch pickup: OFF", "このポーチの自動収納: OFF");
        translated(
                "gui.packingcube.pickup_pouch_help",
                "When ON, this installed pouch automatically stores matching ground items. OFF"
                    + " disables only this pouch. Manual insertion still follows the filter. Loose"
                    + " pouches do not auto-pick up. Open backpacks are temporarily skipped.",
                "ONなら装着中のこのポーチへ、フィルターに合う拾得物を自動収納します。OFFはこのポーチだけに適用。手動収納は引き続きフィルターに従います。単体のポーチは自動収納せず、開いているバックパックは一時停止します。");
        translated(
                "gui.packingcube.settings_space_help",
                "Not enough room beside the inventory. Reduce GUI Scale or widen the window to open"
                        + " settings.",
                "横に設定を表示するスペースが足りません。GUIスケールを小さくするか、ウィンドウを広げてください。");
        translated("key.categories.packingcube", "PackingCube", "PackingCube");
        translated("gui.packingcube.radial_title", "Select a Bag", "バッグを選択");
        translated("gui.packingcube.radial_cancel", "Choose a bag", "バッグを選択");
        translated("gui.packingcube.radial_empty", "No modules", "装着バッグなし");
        translated("gui.packingcube.radial_empty_slot", "Empty slot", "空きスロット");
        translated("gui.packingcube.radial_click", "Left-click to select", "左クリックで決定");
        translated("gui.packingcube.radial_manage", "Manage Backpack", "バックパックの構成");
        translated("key.packingcube.open_radial", "Open bag radial menu", "バッグのラジアルメニューを開く");
        translated("key.packingcube.open_big_bag", "Open Backpack directly", "バックパックを直接開く");
        translated(
                "message.packingcube.empty_cursor",
                "Put down the cursor stack before switching screens.",
                "カーソルのアイテムを戻してから操作してください。");
        translated("gui.packingcube.radial_toggle_hint", "Press to close", "再押下で閉じる");
        translated("tooltip.packingcube.filter_item", "  %s [%s]", "  %s [%s]");
        translated(
                "tooltip.packingcube.filter_more",
                "%s more (view all rules in the bag filter screen)",
                "ほか%s件（全条件はバッグのフィルター画面で確認）");
        translated(
                "tooltip.packingcube.filter_include",
                "Filter: Include (store matching items)",
                "フィルター: 含む（一致するものを収納）");
        translated(
                "tooltip.packingcube.filter_exclude",
                "Filter: Exclude (reject matching items)",
                "フィルター: 含まない（一致するものを除外）");
    }

    private void translated(String key, String english, String japanese) {
        add(
                key,
                switch (locale) {
                    case EN_US -> english;
                    case JA_JP -> japanese;
                });
    }
}
