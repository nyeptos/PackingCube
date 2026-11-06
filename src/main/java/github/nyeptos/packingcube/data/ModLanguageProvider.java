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
        translated("item.packingcube.storage_pouch", "Storage Pouch", "収納ポーチ");
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
        translated(
                "gui.packingcube.refill_threshold",
                "Start refill at: %s remaining",
                "補充開始: 残り%s個以下");
        translated("gui.packingcube.refill_target", "Hand count after refill: %s", "補充後の手元: %s");
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
                "Start refill when the hand has at most",
                "補充を始める残り個数");
        translated("gui.packingcube.refill_threshold_count", "%s", "%s個");
        translated(
                "gui.packingcube.refill_target_label", "Hand count after refill", "補充後の手元の個数");
        translated("gui.packingcube.refill_item_count", "%s items", "%s個");
        translated(
                "gui.packingcube.refill_target_help",
                "1-%s items, following the item's real stack size.",
                "1〜%s個。アイテムの最大スタックに自動で合わせます。");
        translated(
                "gui.packingcube.refill_threshold_help",
                "Refills up to the count above once the hand reaches this. 0 waits until empty.",
                "残りがこの個数以下になったら上の個数まで補充します。0個は空になったときだけです。");
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
                "gui.packingcube.refill_pause_start",
                "Pause supply on slots",
                "補充を止める枠を選ぶ");
        translated(
                "gui.packingcube.refill_pause_banner",
                "Click a pouch slot to pause or resume supply",
                "枠をクリックすると補充の停止・再開を切り替えます");
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
        translated("gui.packingcube.refill_hotbar_index", "Hotbar %s", "ホットバー %s");
        translated("gui.packingcube.refill_inventory_index", "Inventory %s", "インベントリ %s");
        translated("gui.packingcube.refill_slot_feet", "Feet", "足");
        translated("gui.packingcube.refill_slot_legs", "Legs", "脚");
        translated("gui.packingcube.refill_slot_chest", "Chest", "胴");
        translated("gui.packingcube.refill_slot_head", "Head", "頭");
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
                "gui.packingcube.refill_collect_on",
                "Collect swapped-out item: ON",
                "外したアイテムを回収：ON");
        translated(
                "gui.packingcube.refill_collect_off",
                "Collect swapped-out item: OFF",
                "外したアイテムを回収：OFF");
        translated(
                "gui.packingcube.refill_lock_on",
                "Auto-lock collected slot: ON",
                "回収した枠を自動ロック：ON");
        translated(
                "gui.packingcube.refill_lock_off",
                "Auto-lock collected slot: OFF",
                "回収した枠を自動ロック：OFF");
        translated("gui.packingcube.refill_durability_off", "Durability: OFF", "耐久：OFF");
        translated(
                "gui.packingcube.refill_durability_percent",
                "Durability: swap at %s%% left",
                "耐久：残り%s％以下で入れ替え");
        translated(
                "gui.packingcube.refill_durability_count",
                "Durability: swap at %s left",
                "耐久：残り%s以下で入れ替え");
        translated("gui.packingcube.refill_durability_label", "Durability limit", "耐久しきい値");
        translated("gui.packingcube.help", "Help", "説明");
        translated(
                "gui.packingcube.refill_help_hint",
                "Hover a button to read what it does.",
                "説明を見たいボタンにカーソルを合わせてください。");
        translated("gui.packingcube.refill_help_name_source", "Pouch slot", "ポーチ枠");
        translated(
                "gui.packingcube.refill_help_body_source",
                "This slot holds the item to supply and where it goes.",
                "この番号の枠が、出すアイテムと入れる先を持ちます。");
        translated("gui.packingcube.refill_help_name_pages", "Pages", "ページ");
        translated(
                "gui.packingcube.refill_help_body_pages",
                "Switch between destination, method and filter. Switching never changes settings.",
                "補充先・補充方法・補充フィルターを切り替えます。切り替えても設定は変わりません。");
        translated("gui.packingcube.refill_help_name_enabled", "Supply on/off", "補充ON/OFF");
        translated(
                "gui.packingcube.refill_help_body_enabled",
                "Whether this slot supplies at all. Storage still works when off.",
                "この枠が補充に使われるかどうか。OFFでも収納はできます。");
        translated(
                "gui.packingcube.refill_help_name_select", "Select destination", "補充先を選択");
        translated(
                "gui.packingcube.refill_help_body_select",
                "Press it, then click an inventory or equipment slot to assign it.",
                "押してから、インベントリか装備の枠をクリックして指定します。");
        translated(
                "gui.packingcube.refill_help_name_clear", "Clear destination", "補充先を解除");
        translated(
                "gui.packingcube.refill_help_body_clear",
                "Sets this slot back to no destination.",
                "この枠の補充先を未指定に戻します。");
        translated("gui.packingcube.refill_help_name_replace", "Replace", "置換");
        translated(
                "gui.packingcube.refill_help_body_replace",
                "Swaps out a different item. Off only fills empty destinations.",
                "補充先に別のアイテムがあるとき入れ替えます。OFFなら空の枠にだけ入ります。");
        translated("gui.packingcube.refill_help_name_collect", "Collect", "回収");
        translated(
                "gui.packingcube.refill_help_body_collect",
                "The swapped-out item goes back into this pouch. Off sends it to your inventory.",
                "入れ替えで外したアイテムを、このポーチへ戻します。OFFならインベントリへ戻します。");
        translated("gui.packingcube.refill_help_name_lock", "Slot auto-lock", "枠の自動ロック");
        translated(
                "gui.packingcube.refill_help_body_lock",
                "A slot holding a collected item stops supplying until you empty it."
                        + " Pause supply on slots resumes it without emptying.",
                "回収したアイテムが入った枠は、空にするまで補充に使いません。"
                        + "「補充を止める枠を選ぶ」なら、中身を残したまま再開できます。");
        translated("gui.packingcube.refill_help_name_pause", "Supply pause", "補充の停止");
        translated(
                "gui.packingcube.refill_help_body_pause",
                "Pick the pouch slots that stop supplying. Items stay where they are, and"
                        + " picking the same slot again resumes it.",
                "補充を止める枠を選びます。中身はそのままで、同じ枠をもう一度選ぶと再開します。");
        translated(
                "gui.packingcube.refill_slot_locked",
                "Collected slot: supply paused",
                "回収済みの枠：補充停止中");
        translated(
                "gui.packingcube.refill_slot_unlock_hint",
                "Use \"Pause supply on slots\" in the settings to resume",
                "設定の「補充を止める枠を選ぶ」で再開できます");
        translated(
                "gui.packingcube.refill_help_name_durability_mode",
                "Durability limit",
                "耐久しきい値");
        translated(
                "gui.packingcube.refill_help_body_durability_mode",
                "Off swaps as soon as the data differs. Percent and count wait until the item in"
                        + " place is nearly spent. Other items are never delayed.",
                "OFFならデータが違えばすぐ入れ替えます。％と実数は、補充先の残り耐久が指定値以下に"
                        + "なるまで待ちます。別アイテムの置換は待ちません。");
        translated(
                "gui.packingcube.refill_help_name_durability_value",
                "Durability value",
                "耐久の値");
        translated(
                "gui.packingcube.refill_help_body_durability_value",
                "Percent uses the share left; count uses the remaining points. 1 is risky because"
                        + " the item can break before the next tick.",
                "％は残りの割合、実数は残りの点数で比べます。1は次のtickまでに壊れて消えることが"
                        + "あるので注意してください。");
        translated("gui.packingcube.refill_help_name_priority", "Priority", "優先度");
        translated(
                "gui.packingcube.refill_help_body_priority",
                "Higher numbers supply first. Ties: pouch order, then slots 1-9.",
                "数字が大きい枠から補充します。同じならポーチ順、次に1〜9番の順です。");
        translated("gui.packingcube.refill_help_name_target", "Final count", "補充後の個数");
        translated(
                "gui.packingcube.refill_help_body_target",
                "What you hold after refilling, up to that item's stack limit.",
                "補充が終わったときに手元に持つ個数。上限はそのアイテムの最大スタックです。");
        translated("gui.packingcube.refill_help_name_threshold", "Remaining", "残り個数");
        translated(
                "gui.packingcube.refill_help_body_threshold",
                "Refill when the count drops to this or below. 0 waits until empty.",
                "手元がこの数以下になったら補充します。0なら空になったときだけです。");
        translated(
                "gui.packingcube.refill_help_name_filter", "Refill filter", "補充フィルター");
        translated(
                "gui.packingcube.refill_help_body_filter",
                "Limits what leaves the pouch by the rules you register. Storage is never limited.",
                "ポーチから出すアイテムを、登録した条件で制限します。収納は制限しません。");
        translated("gui.packingcube.refill_help_name_trade", "Trading", "取引");
        translated(
                "gui.packingcube.refill_help_body_trade",
                "Trade materials follow the same rules. Off offers all stock.",
                "取引で出す材料も同じ条件で制限します。OFFならすべての在庫が候補です。");
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
