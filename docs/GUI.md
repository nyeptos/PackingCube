# バッグ設定画面の共通部品

## 目的と分担

今後のバッグにも同じフィルター編集と右側スクロール領域を組み込める構成。新しいバッグそのものは追加していない。

| クラス | 担当 |
|---|---|
| `gui/BagSettingsLayout` | 左収納領域・右ビューポート・設定内容の座標。クライアントGUI型に依存しない |
| `gui/SettingsScrollState` | スクロール量・範囲補正・スクロールバー・表示判定 |
| `gui/FilterEditorState` | 検索文字列、候補／登録一覧、ページ、同期スナップショットからの表示用投影 |
| `client/widget/SettingsScrollPanel` | 右側ウィジェットの所有、クリッピング、ホイール・バー・キーボード入力 |
| `client/widget/FilterEditorPanel` | フィルター編集部品、見本表示・ヒット判定、ラベル／ツールチップ |
| 各バッグのScreen | 実スロット、バッグ固有設定、キー遷移、通信への接続、背景、入力の振り分け |

`FilterEditorPanel` は `BagMenu`・アイテム登録・自動収納・Payload／通信処理に依存しない。読み取る設定／見本と更新要求を `Access` で受け取る。

## レイアウト

- 左側はポーチの実収納9枠とプレイヤーインベントリのみ。設定の開閉前後で画面上の位置もスロット座標も変えない。BagScreenのimageWidth=176/imageHeight=132を常時維持する。
- 右側にこのポーチの自動収納、優先度、単一モード切替、見本枠・説明、個別登録、候補／登録済み、検索、一覧、ページ移動をすべて配置する。
- 中央の区切り線は描画しない。
- `viewport()` は画面に見える領域、`content()` はスクロール前の論理領域。設定行は `optionRow(index)` で予約する。現ポーチは収納領域高132・固有設定2行。
- 左の収納＋プレイヤーインベントリは通常画面の中央座標と高さ132のまま、設定パネルは初期状態では右側に置き、ヘッダー移動・四辺／四隅リサイズに対応する（ポーチ初期高は最大373、手動では画面内の範囲で拡大可能）。入りきらない場合は右側だけスクロールする。ホイールと右端のバーを使用でき、全体が収まる場合はバーを表示しない。左インベントリ上のホイールで右側を動かさない。
- 右側にフォーカスがあるときはPageUp／PageDown／Home／Endでも移動できる。検索欄入力中はEditBox本来のキー操作を優先する。
- 一覧は1ページ6件。ページ切替と設定領域全体のスクロールは別の機能。
- リサイズでは検索・一覧種別・ページとスクロール量を保持し、範囲外なら補正する。小画面では設定ウィンドウを画面内に縮め、インベントリとの重なりを許可する。設定高の下限は通常80、画面がさらに低ければ画面高を優先する。実インベントリの寸法は変えない。
- 固有設定は0〜16行、収納領域高は0〜1024の範囲。無制限の汎用フォームではない。

## 新しい画面への組み込み

1. Screenごとに `FilterEditorState` と `SettingsScrollState` を保持する。static共有しない。
2. メニューの同期データを読む `FilterEditorPanel.Access` を実装する。編集要求はそのバッグのサーバー検証済み更新経路へ送る。
3. 実インベントリの寸法（現在176×132）で`super.init()`を済ませる。パネルのためにimageWidth/imageHeightを広げない。FloatingWindowの画面内寸法からBagSettingsLayout.floatingで設定レイアウトを作る。右側の空き不足で開くことを拒否しない。
4. 左のleftPos/topPosは維持し、右側のsettingsLeft/settingsTopを別に算出する。右側の背景・スクロール領域・ウィジェット・見本判定に同じ右原点を渡す。
5. 固有設定と共通パネルのウィジェットはScreenへ直接登録せず、スクロール領域へ登録する。

```java
var bounds = settingsWindow.state();
var layout = BagSettingsLayout.floating(bounds.width(), bounds.height(), customRows);
int settingsLeft = bounds.x() - 176;
int settingsTop = bounds.y();
var viewport = new SettingsScrollPanel(layout, settingsLeft, settingsTop, scrollState);
addWidget(viewport);
// renderLabelsで実スロットの後、カーソル上のアイテムの前に手動描画する。
var editor = new FilterEditorPanel(font, access, editorState);
editor.build(layout, settingsLeft, settingsTop, viewport::add);
viewport.setContentRenderer(graphics -> {
    editor.renderBackground(graphics);
    editor.renderLabels(graphics);
    // バッグ固有のラベルもここで論理ローカル座標に描く。
});
```

既存 `BagScreen` が実装例。`SettingsScrollPanel` が座標移動とscissorを行うため、contentRendererの中で同じ変換を重ねない。実インベントリはスクロール領域に登録しない。preferredHeightは初期サイズの候補だけに使用し、実際の設定寸法はFloatingWindowStateが画面とユーザー指定寸法に合わせる。既存ポーチは画面高240で設定高232、画面高540なら373。左の実スロット座標と背景高132は変えない。

## 描画と入力の接続

- tickで `editor.tick()` を呼ぶ。
- 見本の描画とヒット判定は同じ `sampleSlot()` を使用する。ヒット判定へ渡すYは `viewport.unscrolledY(mouseY)`、Xは画面座標のまま。
- 最初に生のマウス位置がビューポート内か確認する。領域外の見本やボタンを操作させない。ツールチップも領域内だけで判定する。
- ツールチップは `editor.renderTooltip(graphics, mouseX, mouseY, mouseX, viewport.unscrolledY(mouseY))`。判定座標と実際に表示する画面座標を混同しない。
- 見本の押下／解放を消費したら通常スロット処理へ流さず、バニラのドラッグ分配状態も解除する。領域外解放を誤って見本登録しない。`BagScreen` の処理を参照する。
- スクロールバーを領域外までドラッグした場合も解放を捕捉して終了する。右側で始まった入力を実インベントリのドラッグに漏らさない。
- 検索欄フォーカス中は文字入力をB/C・ホットバー・投棄より優先する。EditBoxのイベントはスクロール領域を経由する。

## モード・説明文・サーバー側

モードは「モード: 含む／含まない」の1個のボタンで現在値を表示する。押下時は最新同期値の反対側を要求し、登録条件・検索・一覧種別・ページはリセットしない。

自動収納はポーチのON/OFFだけで制御する。バックパック全体スイッチは廃止し、保存済みの旧enabled=falseも参照しない。優先度・フィルターは維持し、開いている親への一時停止も継続する。

`Access.modeHelp()` / `emptyRules(boolean)` は既存収納用の説明。別用途ではComponentを差し替えられる。新しい文言はlang datagenへ日英で追加する。

保存スキーマは既存 `BagFilter`。新しいバッグのサーバーメニューは所有・現在のメニューID・要求値・見本の正当性などを引き続き検証する。共通UIにしただけで、ポーチ専用サーバー処理が他バッグを無条件に受け入れるわけではない。

## バックパックの小型操作と設定の閉じ方

バックパック画面に自動収納ボタンは置かない。ポーチ側の自動収納・優先度は右設定に保持する。×は左領域ヘッダー(156,2)の14×14で、設定だけ閉じてポーチ通常画面へ戻す。左のインベントリは開閉時に動かない。

制作画像の全保存先・寸法・状態差分は`docs/TEXTURES.md`。PC-35で受領した20枚のウィジェットは切替済み。背景とラジアル等の未受領画像はまだ仮表示。

## 設定背景の範囲

設定用の背景はsettingsBackground()で計算する右領域のみ（X176から画面右端、Y0から可変の設定パネル高）。左の176×132インベントリやその外縁を包む追加背景は描画しない。左のバニラインベントリ背景は保持する。右のウィジェット表示領域・スクロール入力範囲・閉じる位置は変更しない。

## 横幅不足と右背景のクリック

`fitsBesideInventory`で左を動かさずに右の最小幅124と余白を確保できるか確認する（GUI論理幅440以上が目安）。足りない場合は設定を開かず、日英メッセージでGUIスケール／ウィンドウ幅の調整を案内する。設定中にリサイズで不足したら設定だけ閉じる。通常収納・取り出しは継続できる。

`hasClickedOutside`は右背景もGUI内として扱い、右の余白をクリックしただけでカーソル上の実アイテムを外へ投棄しない。見本・スクロール・ツールチップは右原点の座標を使用する。実GUIでの操作は別途確認する。

## 設定パネルだけの移動（PC-32）

- 上端20pxの「設定」ヘッダーを空のカーソルで左ドラッグ。実インベントリの座標は変更しない。
- `SettingsPanelPosition` がパネル左上・画面端クランプ・ドラッグ状態を管理。同じ画面内の設定開閉では位置を保持。別バッグを開いた場合や画面を閉じた場合の永続保存はしない。
- `settingsLeft/settingsTop` は設定用原点。`leftPos/topPos` は固定インベントリ原点。
- `SettingsScrollPanel.moveTo` で全ウィジェット・ビューポートを移し、`FilterEditorPanel.moveOrigin` で見本枠の入力原点を同時更新。スクロール・検索・ルールはリセットしない。
- ヘッダーはスクロール領域外。内容上端24px、最大表示高さ373px（ポーチ設定2行）。
- パネルはインベントリに重ねられる。`renderLabels` でスロットの後、持ち上げたアイテムの前に描画。パネル下のスロットのクリック・ホバー・ツールチップを遮断する。
- PC-33でこの移動専用実装を共通FloatingWindowへ移行。横幅不足の開く制限は廃止。現行の仕様は下記および`FLOATING_WINDOWS.md`を参照。
- 新しいテクスチャは追加しない。暫定ヘッダーは単色描画で、ユーザー制作素材への置換が可能。

## PC-33 共通フローティングウィンドウ

- `FloatingWindowState`（バッグ非依存）と`FloatingWindow`（クライアント枠）で移動・四辺四隅リサイズ・画面内補正・前面レイヤーを共通化。詳しい再利用手順は`FLOATING_WINDOWS.md`。
- 描画前後でbatchをflushし、Z=400の独立レイヤーへ描く。インベントリのアイテム・×文字が後から設定の上に出る問題を防ぐ。カーソル上の実アイテムは通常どおり後続のコンテナ処理で描画する。
- リサイズ時は内容の横幅・ビューポート高を再構築。検索文字・ページ・モード・スクロール状態を保持し、スクロールの範囲のみ補正する。位置とサイズは同じ画面内の設定開閉で維持。
- 設定パネルは通常最小124×80、画面の方が小さければ画面内へ縮める。Minecraftの小画面GUI（320px幅等）でも開く。設定内容が縦に収まらなければスクロール。
- `SettingsScrollPanel`にはBagSettingsLayoutを必要としない汎用コンストラクタも用意。既存のレイアウト付きコンストラクタは接続用アダプタ。
- `SettingsPanelPosition`は旧移動専用APIの互換アダプタ。新しい画面では使用しない。
- バニラのGUIクラスやグローバルイベントに変更なし。組み込んだMOD画面だけに適用する。

## PC-34 説明ポップアップの前面表示
設定GUIを前面へ上げた結果、標準Z=400の説明より設定の文字が前に出る問題に対し、共通FloatingWindowへ説明用レイヤーを追加。ボタンの遅延説明と見本枠・アイテムの直接説明を同じ最終経路でZ=800へ描画する。設定GUIを下げず、インベントリ＜設定＜説明の順を維持する。再利用時はFLOATING_WINDOWS.mdのqueueTooltip/renderTooltips接続を使う。

PC-34追加: 設定へアイテムをドラッグすると本体が隠れ個数だけ前に出る問題も同じ深度契約で修正。renderLabels末尾で共通prepareContainerCursorLayerを呼び、標準の持ち上げアイテム・個数・snapbackをまとめてZ=250持ち上げる。説明はそれらより上のZ=800。収納やサンプルコピーの通信・消費挙動は変更なし。

## PC-35 ウィジェット画像
受領20枚をWidgetSkin/WidgetTexturesとSkinnedButton/SkinnedEditBoxへ接続。見本枠・スクロールバーも適用。選択タブは無効ボタンではなくselected画像で表示し、選択済み再クリックでページをリセットしない。×は画像のみで文字を重ねない。設定のタイトル帯／文字とラジアルの輪は合意どおり非表示。上端20pxの移動判定は維持する。画像一覧はTEXTURES.md、詳細はWIDGET_SKINS.md。

## PC-36 背景と白文字

ユーザー制作のbag_inventory.pngを176×132の原寸で、settings_panel.pngを四辺4px固定で表示。灰色のコード背景／縁は描かず、金色の枠をそのまま使う。タイトル帯は復活させない。文字は状態にかかわらず白に統一。WidgetTextures.drawはPNG直前にflushするので、先行する遅延fill/textが後から画像へ被らない。

ラジアルの背景も受領PNGへ接続。RadialLayoutで背景サイズ・枠配置・タイトル・管理ボタンを一元化し、標準的な小画面でも枠内に収める。枠のPNGは未受領なので仮枠のまま、輪は描かない。PC-34の説明／保持アイテムの前後関係、固定インベントリ、移動・リサイズは維持する。

## PC-37 ラジアル枠画像
コード描画の仮枠をユーザー画像slot.png / slot_hovered.png / slot_pressed.pngへ置換。通常枠は空き枠にも共用。押下は選択送信からサーバーによる画面切り替えまで保持し、送信は遅延させない。アイコン・番号・当たり判定・白文字・背景は維持。36×36原寸、アイコン位置(10,7)、番号Y=25を維持。

## PC-38 ホバー説明の整理とラジアル間隔
自動収納トグル・含む/含まないモード切り替えからホバー説明だけを削除。動作・ボタン文字・優先度等の他の説明は維持。ラジアル4番を右、7番を左へ標準半径72で8px（半径に応じて縮小）移動し、4–5/6–7の横方向の枠間隔を約1pxから9pxへ拡大。他の番号の配置、背景、36px枠、角度ベースの選択は維持。

## PC-39 ボタンのホバー・押下アニメーション
ユーザー画像は無加工のまま、SkinnedButton（通常・タブ・トグル・閉じる）とラジアル枠・管理ボタンへ共通のButtonAnimation/ButtonAnimationRendererを接続。ホバー/キーボードフォーカスは1 GUI px上へ45ms時定数で滑らかに移動。押下は30msで93.5%へ縮小・2px沈み、180msで復帰する短いパルス。文字・アイコンも同じ変換で動かす。通常サイズ以上へ拡大しないため横端のスクロールクリップを避ける。選択済みタブは表示だけ反応し、ページをリセットしない。

実時間ベースでFPSに依存せず、無効化で動きをリセット。入力判定の矩形は固定し、クリック/キーのアクションやネットワーク送信を遅延させない。即座に画面が閉じるボタン・ラジアル選択は次画面を待たせず遷移するので、押下演出が最後まで見えない場合がある。既存PNG状態差分の切替は維持し、画像間クロスフェードは追加しない。バニラGUI・背景透過・ウィンドウ移動/サイズ・マウス位置には変更なし。描画はflush/push/try-finally/flush/popでローカル変換を閉じ、他GUIのレイヤーへ影響させない。

## PC-40 ホバーの見切れ・状態判定・画像割り当てと音
SettingsScrollPanelはスクロール先頭/末尾だけ描画クリップを上下2pxまで拡張し、上端の自動収納トグルが1px浮いても切れないようにする。スクロール中間のクリップ、論理viewport/入力判定/スクロール量/つまみは変えず、外側FloatingWindowのクリップも維持。描画余白上だけにカーソルがある場合は子ウィジェットへ仮座標を渡してクリック不能な場所のホバーを防ぐ。

SkinnedButtonでisHoveredOrFocusedを無条件に使うのをやめ、実マウスホバーかキーボード入力中のフォーカスだけを共通ButtonHoverStateで強調する。クリックで残ったマウスフォーカスが浮き上がり状態を固定し、再ホバー時に動かなく見える問題を防ぐ。スクロールで見えなくなったボタンはホバー状態をリセット。

通常の有効ボタン（優先度/モード等）もbutton_selected_hovered.pngを共有ホバー画像として使用。通常未選択はbutton_normal、選択中タブの通常はbutton_selected。button_hovered.pngは未受領のため新画像は生成しない。自動収納ON/OFFは従来の各toggle_*_hoveredを維持。優先度MIN=0/MAX=9の無効な−/＋は押せず、ホバー画像/演出/音も無効のまま。

有効ボタンへマウスが入った時だけUI_BUTTON_CLICKをpitch1.6・volume0.15で1回再生。ホバー滞在中/キーボードフォーカスだけ/無効状態では鳴らさない。既存クリック音とラジアルのホバー音は維持し二重追加しない。背景透過・PNG原本・装備モデル・クリック処理・PC-39の押下縮小演出は変更しない。

## PC-41 スクロールバーのホバー・つかむ音
SettingsScrollPanelのつまみへマウスが入った時にhover音を1回、左クリックでつまみ/トラックのドラッグを開始した時にgrab音を1回再生。hoverはUI_BUTTON_CLICK pitch1.6 volume0.15、grabはpitch1.8 volume0.2。ドラッグ中にカーソルがつまみ内外を行き来しても連続再生しない。トラッククリックでつまみがマウス下へ移動してもhoverを重ねない。マウスを離した時の音は追加しない。

ScrollbarSoundStateで進入/ジェスチャー単位の発音を管理。スクロールバーが不要な場合は無音・状態をリセット。UiInteractionSoundsにボタンとスクロールバーの共通音処理を分離し、既存ボタンの音量/高さは維持。画像・アニメーション・当たり判定・スクロール量・背景透過・バニラGUIは変更しない。

## PC-42 手動の一括収納
バックパック管理画面右上へ「一括収納」を追加。通常インベントリ27枠のみを開いているバックパックの装着ポーチへ、既存フィルター・優先度（同値は左から右）順で移す。自動収納OFFも手動収納可。ホットバー/防具/オフハンド/カーソルは動かさず、満杯/対象外は元枠に残す。他バックパック・単体ポーチへは振り分けない。廃棄なし。空カーソルでのみ操作可能。収納個数/0件をアクションバー表示。

BagStowPayloadはmenu IDだけを送り、BagMenu.stowAllで現menu/ID/所有/一意性/生存/非観戦等を検証。開いた親の既存rootStorageを再利用するBagRouter.routeManualを追加し、拾得経路とpriority安定ソート・残数挿入を共有。日英4キー生成、既存ボタン画像/ホバー音/演出を使用。詳細はdocs/MANUAL_STOW.md。

## PC-43 補充ポーチ
受領したrefill_pouch.png16×16を原本のまま使用し、独立アイテムrefill_pouchを追加。1列9枠の装着モジュール/単体利用/ラジアルに対応。ホットバー補充と取引補充を独立ON/OFF（新規は両ON）、優先度と共通フィルターを設定できる。初期収納条件は含まない・未登録、通常収納ポーチの初期条件は不変。補充元は装着した有効な補充ポーチだけ。通常ポーチ・単体からは補わず、拾得自動収納もしない。一括収納は明示操作として収納可。

ホットバーはメインハンドの実使用を起点に選択中枠の減少分だけ同一アイテム/全component一致で補充。取引は選択中の現在価格1回分の不足を入力へ供給し、2材料は両方足りる場合のみ。結果取得/自動売買はしない。限定ATで選択取引を読み、バニラGUI・終了時の返却処理は変更しない。通信生成データへ種別boolを追加し通信version2、マルチ全員更新が必要。レシピ未追加。詳細/制限/実機手順はdocs/REFILL_POUCH.md。


## PC-44 補充先・タイミング・補充フィルター
補充ポーチのフィルターを収納制限から搬出制限へ変更。通常収納ポーチは不変。補充ポーチへの手動/一括収納はフィルター非依存、非入れ子制限を維持。フィルター無効=全候補、ONではアイテム/タグの含む・含まないをホットバー/オフハンド/取引すべての取り出しに適用し、OFFやモード切替でも登録は保持する。

ホットバー9枠の指定・独立オフハンドON/OFF、使い切り/しきい値/毎回の使用起点タイミング、補充後個数を追加。初期は全枠/3機能ON、使い切り、1スタック、補充フィルターOFF。対象枠は使用した枠に限り、他の空き枠は埋めない。トーテム消費イベントも検知し、専用設定ではなく共通フィルターで制御。投棄/別物/手動移動のみは保護。取引は1回分のまま。

右設定は既存スクロール内の10行と共通エディター。RefillSettingsPanelを分離し、既存素材/音/アニメーション/白文字/透過/左インベントリ位置を維持。現menu/所有/種類/値のサーバー検証を通して保存し、ラジアル見本にも安全な設定のみ反映。通信版3、全参加者の更新が必要。詳細はdocs/REFILL_POUCH.md。実描画/操作/実NPC/実マルチは要確認。


## PC-45 補充設定の整理と検知・診断
名称は「補充フィルター」に統一。補充先/補充方法/補充フィルターの3ページへ分け、不要な残数入力・指定個数入力は非表示、複数ポーチ優先度は詳細を開いた時だけ表示。条件を「手元が0個になったら補充」等の文章にし、数値は「補充を始める手元の残り個数」「補充後に手元に持つ個数」と明記。追加個数ではないことも説明する。フィルターOFFで登録エディターを隠すが登録は保持。ページ切替は保存値に影響しない。短いページの余分なスクロール領域をなくし、非表示エディターへの入力をnullガードする。

UseItemOnBlockEvent/EntityInteractSpecific/非耐久PlayerDestroyItemEventを補充検知へ追加。従来の直接イベント+手動shrinkに加え、実ServerPlayerGameModeでのブロック設置両手、実食事完了、Player.tick、直接ItemStack.useOnをテストする。サーバー最終数だけでなくクライアント予測のスロットミラーを追加し、16→15→16/オフハンド1→0→1で補充後の差分同期が省略される不具合を修正前に再現。補充成功時に対象枠のremote baselineを消費直後の状態へ更新し、バニラ差分同期で必ず訂正を送る。実クライアントでの解消は要確認。読み取り専用/権限不要の`/packingcube refillstatus`で両手の適用条件を確認できる。適用後の実機確認待ち。ほかの原因が残る場合は再現条件・ゲームモード・装着状況・診断結果を確認する。画像・モデル・装備位置・収納ルール・初期値・通信形式は変更しない。

### Refill destination selection (PC-46)

The source pouch and player inventory remain anchored at the same coordinates. Read-only
armor/offhand previews sit on the right, active functional Curios previews on the left.
Selection mode suppresses item actions and commits only a matching left press/release pair.
Mark overlays use z210 before the floating settings window (z400); popup/cursor layers remain
managed by FloatingWindow. Opacity feedback does not scale pixel art or move hit boxes.
The client is a preview/selector, not an authority over equipment or refill transactions.

While choosing destinations, the floating settings panel temporarily collapses so it cannot
obscure right-hand equipment targets. A finish button stays in the pouch header; finishing
or Esc restores settings with its prior floating position/size. No inventory slots move.

### PC-47 source pages

1–9 are source selection pages; Destination/Method are per source, Shared is pouch-wide.
Destination confirmation is now single-shot (auto returns to normal item mode); the header
button cancels instead of finishing a multi-target edit. Esc cancels without any configuration
write. The selected source is marked using the existing artist PNG. No new textures needed.

## PC-49：バッグ共通の装備サイドパネル

補充ポーチ専用だったサイドパネルをバックパック・収納ポーチにも共通化した。通常モードのクリックはメニュー上の実装備スロットへ送信し、補充先指定モードのクリックは指定のみに使う。旧「表示専用」の制約は廃止。装備は右、Curiosは左、元の中央インベントリ位置は維持。操作・動的なCurios構成変更時の挙動は `BAG_EQUIPMENT.md` を参照。

## PC-50：補充先選択の入力分離

選択開始時に設定パネルのドラッグ・フォーカス・ウィンドウ移動状態を解除し、次のtickを待たず非表示の設定ウィジットを取り除く。選択モード中の押下・解放・ドラッグ・スクロールは、古い設定パネルや見本スロットの入力処理より先に処理する。非表示パネルの範囲や押下状態が選択確定の解放イベントを吸収しないようにする。

選択開始時の補充元番号を固定し、確定パケットには現在のページ番号を読み直さず固定した番号を使う。保存形式、装備操作、素材は変更しない。

## PC-51：選択枠のアニメーションと音

- ホバー：`refill_target_hovered.png` を滑らかにフェード。
- 押下：`refill_target_pressed.png` を重ね、ホールド中は維持、確定後も短く減衰。
- 枠は18×18固定。ピクセルやアイコン、クリック範囲の拡縮・変形はしない。
- 選択済みの青枠は引き続き設定中だけ表示する。
- 共通の設定ボタンは既存SkinnedButtonのアニメーション/音を維持。選択枠・サイド装備枠のホバーと押下にも高めの静かなUI音を付ける。

## PC-52：両ポーチの共通ボタン描画を統一

- 収納ポーチ/補充ポーチの設定ボタンは共通SkinnedButtonでホバーの浮き上がり、押下の縮み・押し込みを描画する。
- 設定パネルの再構築時に、位置・サイズ・種類が同じボタンのアニメーション/ホバー履歴を引き継ぐ。スナップショットやページ変更で押下直後の動きが消えないようにする。キャッシュは画面インスタンス内のみ、最大512項目。
- 補充元1〜9とページのタブは通常ボタンのテクスチャを使い、実際のON/OFF設定だけスイッチのテクスチャを使う。
- ホバーは毎フレームの実ヒット範囲から計算する。押下状態と180msのフィードバックを明示し、長押し中も押下の外観を維持する。
- 通常の設定ボタンの押下音も共通の高め・小さめのUI音へ揃える。無効なボタンは発音しない。
- 既存PNGを変更せず、通常ボタンの押下は既存の選択画像、スイッチと×はホバー画像を既定の代替画像にする。
- `textures/gui/widgets/` に任意の `button_hovered.png`(20×20)、`button_pressed.png`(20×20)、`toggle_on_pressed.png` / `toggle_off_pressed.png`(40×12)、`close_pressed.png`(14×14) が存在する場合は対応する専用画像を使う。新規画像は必須でなく、存在しなければ上記代替画像を使う。リソース再読込で有無を再評価する。
- PC51の補充先スロット枠は引き続き18×18の不透明度アニメーション。共通の設定ボタンとは別の表現である。
