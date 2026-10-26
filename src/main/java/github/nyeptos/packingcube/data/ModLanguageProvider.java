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
        translated("item.packingcube.filter_bag", "Storage Pouch", "収納ポーチ");
        translated(
                "message.packingcube.hold_filter",
                "Hold a Storage Pouch in your main hand.",
                "収納ポーチをメインハンドに持ってください。");
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
        translated("gui.packingcube.stow_all", "Stow All", "一括収納");
        translated(
                "gui.packingcube.stow_all_help",
                "Move items from the 27 main inventory slots into this backpack's pouches. Uses"
                    + " filters and priority, even with auto pickup OFF. Hotbar and equipment stay"
                    + " put; leftovers remain in their original slots. Requires an empty cursor.",
                "通常インベントリ27枠から、このバックパックのポーチへフィルター・優先度順で収納。自動収納OFFでも利用できます。ホットバー・装備は動かさず、入り切らない分は元の枠に残します。カーソルは空にしてください。");
        translated(
                "message.packingcube.stow_result", "Stored %s items in pouches.", "ポーチへ%s個収納しました。");
        translated(
                "message.packingcube.stow_empty", "No items could be stored.", "収納できるアイテムはありません。");
        translated("item.packingcube.refill_pouch", "Refill Pouch", "補充ポーチ");
        translated(
                "tooltip.packingcube.refill_supply",
                "Supplies only from this pouch while installed in a backpack.",
                "バックパック装着中、このポーチの中身から補充します。");
        translated(
                "tooltip.packingcube.refill_trade_note",
                "Fills the selected trade's payments; trading remains manual.",
                "選択した取引の材料を補充します。取引の実行は手動です。");
        translated(
                "gui.packingcube.refill_priority_help",
                "0-9: higher first. Ties: equipped backpack, inventory order, then left to right."
                        + " Shortages continue to the next refill pouch.",
                "0〜9。大きい値から補充。同値なら装備中→インベントリ順→左から右。足りなければ次の補充ポーチを使います。");
        translated("gui.packingcube.refill_hotbar_on", "Hotbar refill: ON", "ホットバー補充: ON");
        translated("gui.packingcube.refill_hotbar_off", "Hotbar refill: OFF", "ホットバー補充: OFF");
        translated("gui.packingcube.refill_trade_on", "Trade refill: ON", "取引補充: ON");
        translated("gui.packingcube.refill_trade_off", "Trade refill: OFF", "取引補充: OFF");
        translated("gui.packingcube.refill_offhand_on", "Offhand refill: ON", "オフハンド補充: ON");
        translated("gui.packingcube.refill_offhand_off", "Offhand refill: OFF", "オフハンド補充: OFF");
        translated("gui.packingcube.refill_filter_on", "Refill filter: ON", "補充フィルター: ON");
        translated("gui.packingcube.refill_filter_off", "Refill filter: OFF", "補充フィルター: OFF");
        translated("gui.packingcube.refill_filter_heading", "Refill filter", "補充フィルター");
        translated("gui.packingcube.refill_timing", "Refill condition: %s", "補充する条件: %s");
        translated(
                "gui.packingcube.refill_timing_empty",
                "Refill when the hand is empty",
                "手元が0個になったら補充");
        translated(
                "gui.packingcube.refill_timing_threshold",
                "Refill at the chosen remaining count",
                "手元が指定数以下なら補充");
        translated("gui.packingcube.refill_timing_use", "After using a held item", "手持ちで使ったときに補充");
        translated(
                "gui.packingcube.refill_threshold",
                "Start refill at: %s remaining",
                "補充開始: 残り%s個以下");
        translated("gui.packingcube.refill_target", "Hand count after refill: %s", "補充後の手元: %s");
        translated("gui.packingcube.refill_full_stack", "1 stack", "1スタック");
        translated("gui.packingcube.refill_slot", "Refill hotbar slot %s", "ホットバー%s番を補充");
        translated("gui.packingcube.refill_page_destinations", "Slots", "補充先");
        translated("gui.packingcube.refill_status_main", "Main hand", "メインハンド");
        translated("gui.packingcube.refill_status_offhand", "Offhand", "オフハンド");
        translated("gui.packingcube.refill_status_line", "%s: %s", "%s: %s");
        translated(
                "gui.packingcube.refill_status_creative",
                "Hand refill is disabled in Creative. Test in Survival.",
                "クリエイティブでは手元へ補充しません。サバイバルで確認してください。");
        translated(
                "gui.packingcube.refill_status_inactive",
                "Requires a living, non-spectator player.",
                "生存中・非観戦のプレイヤーが対象です。");
        translated(
                "gui.packingcube.refill_status_menu",
                "Close the container screen before testing hand use.",
                "コンテナ画面を閉じてから使用してください。");
        translated(
                "gui.packingcube.refill_status_cursor",
                "Put down the item on the cursor.",
                "カーソルに持っているアイテムを置いてください。");
        translated(
                "gui.packingcube.refill_status_pouch",
                "No installed refill pouch enables this hand/slot. Check the backpack, switches and"
                        + " slot selection.",
                "この手・枠を有効にした装着中の補充ポーチがありません。装着・ON/OFF・対象枠を確認してください。");
        translated(
                "gui.packingcube.refill_status_empty",
                "Hold the item to check. Empty slots are only refilled immediately after"
                        + " consumption.",
                "調べるアイテムを手に持ってください。空枠は使用直後だけ補充します。");
        translated(
                "gui.packingcube.refill_status_stock",
                "No stock matches this item and all its components.",
                "同じアイテム・同じデータの在庫が補充ポーチにありません。");
        translated(
                "gui.packingcube.refill_status_filter",
                "Matching stock is blocked by the refill filter.",
                "在庫はありますが補充フィルターで除外されています。");
        translated(
                "gui.packingcube.refill_status_use",
                "Matching stock found. Refill occurs only after use meets the chosen condition, not"
                        + " by moving items.",
                "補充できる在庫があります。実際に使い、補充方法の条件を満たすと補充します。手動移動では動きません。");

        translated("gui.packingcube.refill_page_method", "Method", "補充方法");
        translated("gui.packingcube.refill_page_filter", "Shared", "共通");
        translated("gui.packingcube.refill_hotbar_slots", "Hotbar slots to refill", "補充するホットバー枠");
        translated(
                "gui.packingcube.refill_setup_help",
                "Stock and install in a backpack. Refills only the hand you use.",
                "このポーチに中身を入れ、バックパックに装着してください。使った枠だけを補充します。");
        translated(
                "gui.packingcube.refill_creative_help",
                "No hand refill in Creative. Test in Survival.",
                "クリエイティブでは補充しません。サバイバルで試してください。");
        translated(
                "gui.packingcube.refill_trade_help",
                "One payment at a time. Trading stays manual.",
                "取引には1回分の材料を補充します。売買は手動です。");
        translated(
                "gui.packingcube.refill_threshold_label",
                "Refill when hand count is at most:",
                "補充を始める手元の残り個数");
        translated("gui.packingcube.refill_threshold_count", "%s or fewer", "%s個以下");
        translated(
                "gui.packingcube.refill_target_label", "Hand count after refill:", "補充後に手元に持つ個数");
        translated("gui.packingcube.refill_item_count", "%s items", "%s個");
        translated(
                "gui.packingcube.refill_amount_full",
                "Fill hand to one full stack",
                "手元を1スタックまで補充");
        translated(
                "gui.packingcube.refill_amount_custom",
                "Fill hand to a chosen count",
                "手元を指定個数まで補充");
        translated(
                "gui.packingcube.refill_amount_help",
                "Final hand count, not extra items.",
                "追加数ではなく、補充後の手元の個数です。");
        translated(
                "gui.packingcube.refill_priority_show", "Advanced: pouch priority", "詳細: ポーチの優先度");
        translated("gui.packingcube.refill_priority_hide", "Hide pouch priority", "優先度の設定を閉じる");
        translated(
                "gui.packingcube.refill_filter_help",
                "Limits only which items may be supplied. Storage is unrestricted. OFF keeps your"
                        + " rules.",
                "補充してよい物だけを絞ります。収納は自由です。OFFでも登録は残ります。");

        translated(
                "gui.packingcube.refill_empty_include",
                "No rules: nothing may be supplied (when enabled).",
                "未登録: 有効時は何も補充しません。");
        translated(
                "gui.packingcube.refill_empty_exclude",
                "No rules: all stored items may be supplied.",
                "未登録: すべて補充対象です。");

        translated("gui.packingcube.refill_select_start", "Select destination slots", "補充先を選択");
        translated("gui.packingcube.refill_select_done", "Finish selecting", "選択を完了");
        translated(
                "gui.packingcube.refill_select_hint",
                "Select inventory, equipment or Curios slots directly.",
                "インベントリ・装備・Curiosの枠から直接選びます。");
        translated(
                "gui.packingcube.refill_empty_pending",
                "Slot selection is saved. Empty-slot item assignment and expanded refill are not"
                        + " enabled yet.",
                "指定先は保存されます。空き枠への品目指定と補充先拡張の実動作は次の実装で対応します。");
        translated("gui.packingcube.refill_selected", "Selected destination", "補充先に指定済み");
        translated("gui.packingcube.refill_unselected", "Not selected", "未指定");
        translated(
                "gui.packingcube.equipment_controls",
                "Click to equip / unequip; Shift-click to return to inventory",
                "クリックで着脱・Shiftクリックでインベントリへ戻す");
        translated("gui.packingcube.refill_hotbar_index", "Hotbar %s", "ホットバー %s");
        translated("gui.packingcube.refill_inventory_index", "Inventory %s", "インベントリ %s");
        translated("gui.packingcube.refill_slot_feet", "Feet", "防具：足");
        translated("gui.packingcube.refill_slot_legs", "Legs", "防具：脚");
        translated("gui.packingcube.refill_slot_chest", "Chest", "防具：胴");
        translated("gui.packingcube.refill_slot_head", "Head", "防具：頭");
        translated("gui.packingcube.refill_slot_offhand", "Offhand", "オフハンド");
        translated("gui.packingcube.refill_source_enabled_on", "Source %s: ON", "ポーチ%s番の補充：ON");
        translated("gui.packingcube.refill_source_enabled_off", "Source %s: OFF", "ポーチ%s番の補充：OFF");
        translated("gui.packingcube.refill_source_destination", "Source %s → %s", "ポーチ%s番 → %s");
        translated("gui.packingcube.refill_destination_none", "Not assigned", "補充先未指定");
        translated("gui.packingcube.refill_clear_destination", "Clear destination", "補充先を解除");
        translated("gui.packingcube.refill_replace_on", "Replace other items: ON", "別のアイテムと置換：ON");
        translated(
                "gui.packingcube.refill_replace_off", "Replace other items: OFF", "別のアイテムと置換：OFF");
        translated(
                "gui.packingcube.refill_source_priority_help",
                "Higher priority first. Ties: pouch order, then source 1–9. Other items return to"
                        + " free inventory space; otherwise replacement is skipped.",
                "優先度が高い枠を先に補充。同値はポーチ順→1〜9番。置換した元の品は指定先以外のインベントリへ戻し、空き不足なら中止します。");
        translated(
                "gui.packingcube.refill_source_amount_help",
                "This source only. Amount means final destination count, not quantity added. Empty"
                        + " destinations fill automatically, including before first use.",
                "この枠だけの設定です。個数は追加数ではなく補充後の個数。最初から空の補充先にも自動で入ります。");
        translated(
                "gui.packingcube.refill_shared_help",
                "Shared by the whole pouch: refill filter and trading. Per-source ON/OFF also"
                        + " applies to trade supply.",
                "補充フィルターと取引はポーチ共通です。各枠の補充OFFは取引への搬出も止めます。");
        translated("gui.packingcube.refill_select_cancel", "Cancel", "キャンセル");
        translated(
                "gui.packingcube.refill_selecting_source",
                "Selecting destination for pouch slot %s",
                "ポーチ%s番の補充先を選択中");
        translated(
                "gui.packingcube.refill_slot_rules_help",
                "Each source holds one destination. Assigning a source switches this pouch from"
                        + " legacy hand refill to source rules.",
                "各枠に補充先を1つ指定できます。枠別設定を保存すると、このポーチは従来の手持ち補充から枠別補充に切り替わります。");
        translated(
                "gui.packingcube.refill_status_mapped",
                "Source-slot mode: check the pouch's 1-9 pages for destination, stock, enabled"
                        + " state and amount.",
                "枠別補充方式です。ポーチの1〜9番で補充先・在庫・ON/OFF・個数を確認してください。");
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
