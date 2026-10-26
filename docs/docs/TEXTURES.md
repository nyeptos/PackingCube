# PackingCube テクスチャ制作・受領状況

## 現在の状態

PC-35でユーザー制作のウィジェット20枚を無加工で受領・組み込み。詳しい状態切替・原本ハッシュ・コードの分担は`WIDGET_SKINS.md`。背負い／アイテム表示のbackpack.png（32×32）も従来どおり使用。

PC-36でbag_inventory/settings_panel/radial backgroundの3枚も受領・適用。ポーチアイコンとラジアル枠3状態は未受領のため仮表示を維持する。これから作る画像は、置くだけではなく読み込み処理も接続する必要がある。

共通ルール: RGBA PNG、GUIスケール適用前の1px単位。状態差分の大きさと絵の位置をそろえる。文字・個数・ページ・アイテム本体は原則コード描画（×記号だけclose系PNGに含む）。

## 受領済み20枚

保存先: `src/main/resources/assets/packingcube/textures/gui/widgets/`

| ファイル名 | 幅×高さ | 状態・特徴 |
|---|---|---|
| button_normal.png | 20×20 | 共通ボタン通常。四辺3px固定 |
| button_selected.png | 20×20 | 選択中タブ。四辺3px固定 |
| button_selected_hovered.png | 20×20 | 選択中タブのホバー／フォーカス |
| toggle_on_normal.png | 40×12 | 自動収納ON通常。四辺3px固定 |
| toggle_on_hovered.png | 40×12 | ONホバー／フォーカス |
| toggle_on_disabled.png | 40×12 | ON無効 |
| toggle_off_normal.png | 40×12 | 自動収納OFF通常。四辺3px固定 |
| toggle_off_hovered.png | 40×12 | OFFホバー／フォーカス |
| toggle_off_disabled.png | 40×12 | OFF無効 |
| close_normal.png | 14×14 | 設定を閉じる×通常、記号もPNGに含む |
| close_hovered.png | 14×14 | ×ホバー／フォーカス |
| close_disabled.png | 14×14 | ×無効 |
| search_normal.png | 20×18 | 検索欄通常。四辺3px固定 |
| search_focused.png | 20×18 | 検索欄フォーカス |
| search_disabled.png | 20×18 | 検索欄無効 |
| ghost_slot.png | 18×18 | 見本枠。内側16×16にアイテムを描画 |
| scrollbar_track.png | 6×12 | スクロール溝。上下2px固定、縦伸縮 |
| scrollbar_thumb_normal.png | 6×12 | つまみ通常。上下2px固定 |
| scrollbar_thumb_hovered.png | 6×12 | つまみホバー |
| scrollbar_thumb_dragging.png | 6×12 | つまみドラッグ中 |

## 背景・アイテムの受領状況

| 保存先（assets/packingcube/textures/以下） | 幅×高さ | 描く内容 |
|---|---|---|
| item/filter_bag.png | 16×16 | 小さな収納ポーチ。ラジアル等でも使う |
| gui/container/bag_inventory.png | 176×132 | 収納9枠＋プレイヤー27枠＋ホットバー9枠の背景、文字・ボタンなし |
| gui/container/settings_panel.png | 244×132 | 独立設定ウィンドウの背景。タイトルバーの帯・文字・見本枠・ボタンなし。四辺4px固定で伸縮 |
| gui/radial/background.png | 220×220 | ラジアル背景・装飾。輪・バッグ枠・文字・アイテムなし |
| gui/radial/slot.png | 36×36 | 空き／ポーチ入り共通の枠。アイテムと番号は別描画 |
| gui/radial/slot_hovered.png | 36×36 | 共通枠のホバー状態 |
| gui/radial/slot_pressed.png | 36×36 | 共通枠のクリック状態 |

settings_panel上端20pxは見えない移動領域として使うが、専用帯・文字を描き分ける必要はない。輪は不要（ring.pngなし）。空き枠は通常枠と同じためslot_empty.pngも不要。バックパックは既存モデルを流用し、平面large_bag.pngは不要。上表の全7枚は受領・適用済み。ポーチ16×16、枠3状態36×36を元画像のまま使用する。

通常ボタン差分button_hovered.png / button_pressed.png / button_disabled.png（各20×20）は追加可能だが、未受領の間は通常画像を共用する。

## bag_inventory.pngの座標

左上を(0,0)とする。各スロット外枠18×18、内側アイテム16×16。

- 共通のX座標: 7、25、43、61、79、97、115、133、151px。
- 収納9枠: 外枠Y=17。アイテム開始Y=18。
- プレイヤー27枠: 外枠Y=48、66、84。アイテム開始Y=49、67、85。
- ホットバー9枠: 外枠Y=106。アイテム開始Y=107。
- バッグ名は(8,6)、「インベントリ」は(8,38)。文字は描かない。
- 設定ボタンは(115,2)の55×14、×は(156,2)の14×14。背景へ描き込まない。

## ラジアルの配置とアニメーション

220×220の背景は、0始まりの中央4pxが(109,109)〜(110,110)。幾何学的中心は画像端から(110,110)。枠36×36の左上から(10,7)へ16×16アイテム、Y=25付近へ番号をコードで重ねる。

枠は9方向で共通画像を使用し、背景と分離する。ホバーはslot_hovered、選択送信後から画面切り替えまではslot_pressedを使用する。空き枠はslot.png共用。輪は表示しない。クリックの送信を遅らせる演出やアイテムの移動・拡大アニメーションは追加しない。応答が速い場合、押下状態の表示時間は短い。

## PC-36 背景・白文字・再適用
新規背景3枚を無加工で適用し、gui_surfaces.jsonのSHA-256で原本一致を検証。bag_inventoryは176×132原寸、settings_panelは244×132を四辺4px固定で可変サイズへ描く。radial backgroundは220×220を最大として画面高に合わせ、枠・タイトルと管理ボタンが背景の金縁に重ならないよう配置する。

全GUIのボタン・無効状態・検索入力/未選択ヒント・見出し・説明ラベル・ラジアル文字を白に統一。アイテムやバニラtooltipの固有の文字装飾までグローバルに書き換えるものではない。

WidgetTexturesはraw PNGを描く直前に先行fill/textバッチをflushし、後から古い背景が画像へ被ることを防ぐ。配布ZIPは前回20枚と関連クラスも同梱したPC-34基準の累積版。部分的な上書き漏れを防ぎ、モデル・backpackテクスチャは含まない。
