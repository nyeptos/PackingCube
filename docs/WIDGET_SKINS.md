# 受領ウィジェット画像の適用（PC-35）

## 原本と保存先

20枚のユーザーPNGを`src/main/resources/assets/packingcube/textures/gui/widgets/`へ無加工でコピー。`assets/packingcube/widget_assets.json`にファイル名・寸法・SHA-256を記録し、GameTestで梱包内容と原本ハッシュの一致を検証する。色変換・拡縮・透過補正・描き足しは行わない。

| 種類 | 受領した画像 | 原画寸法 |
|---|---|---|
| 共通ボタン | button_normal / button_selected / button_selected_hovered | 20×20 |
| 自動収納 | toggle_on_normal / hovered / disabled、toggle_off_normal / hovered / disabled | 40×12 |
| 閉じる× | close_normal / hovered / disabled | 14×14 |
| 検索欄 | search_normal / focused / disabled | 20×18 |
| 見本スロット | ghost_slot | 18×18 |
| スクロール | scrollbar_track / scrollbar_thumb_normal / hovered / dragging | 6×12 |

表中のすべての名前に`.png`が付く。省略表記のhovered等にも左の接頭辞を付ける。

## 分担

- `gui/WidgetSkin`: 受領済み画像だけを列挙し、状態と画像の対応を一元化。
- `gui/NineSliceAxis`: 固定縁と中央の伸縮計算。画像の分割は描画時だけ行う。
- `client/widget/WidgetTextures`: raw PNGを9分割して描画。GUIアトラスやバニラ素材を上書きしない。
- `SkinnedButton`: Buttonの入力・キー・クリック音・ツールチップ・ナレーションを継承。共通、×、ON/OFFの描画を差し替える。
- `SkinnedEditBox`: 検索欄の背景だけ差し替える。標準の文字入力、4pxの内側余白、カーソル、選択、横スクロールは維持する。
- SettingsScrollPanelの溝・つまみ、FilterEditorPanelの見本枠、ラジアルの構成ボタンにも適用。

## 伸縮と状態

ボタン・スイッチ・検索欄は四辺3px固定。×14×14と見本18×18は原寸。バーは横幅6px固定、上下2pxだけ固定し中央を縦に伸ばす。

通常ボタンのbutton_hovered/button_pressed/button_disabledは未受領。PC-40から有効な通常ボタンのホバーにもbutton_selected_hoveredを共用し、通常時と無効時はbutton_normalを使用する。文字は白のまま、無効時の入力は標準どおり無効。これら3枚が追加されたらWidgetSkinへ状態を追加する。未受領ファイルを毎フレーム探したり、欠損テクスチャを表示しない。

選択中タブはbutton_selected、ホバー／フォーカス中はbutton_selected_hovered。選択中をdisabledとして扱わず、再クリックでページをリセットしない。ON/OFF・検索・×・スクロールつまみは受領した状態差分を使用。つまみはドラッグ中をホバーより優先する。

×はPNGに含まれる記号だけを描き、文字の×を二重に重ねない。PC-36のユーザー指定により、通常・選択中・ON/OFF・無効を含め文字色はすべて白に統一する。

## 既に合意した見た目

設定ウィンドウはタイトルバーの帯と文字を描かず、上端20pxの移動領域だけ維持する。ラジアルの輪のコード描画を削除。ラジアル背景と共通枠3枚はPC-36/37で受領・適用済み。空き枠専用画像は不要。

PC-36でインベントリ背景・設定パネル背景・ラジアル背景も受領して適用。ポーチ画像とラジアル枠はまだ変更しない。ユーザーのバックパックモデル・テクスチャ・装備位置は一切含めない。

## 注意

画像状態と伸縮の数値、梱包されたPNGのハッシュは自動テストで確認する。実クライアントの色の見え方・描画の重なり・操作は別途確認する。PC-34のカーソル／説明前面レイヤーは維持する。バニラのGUIや全画面共通リソースは変更しない。

PC-36補足: raw PNG描画の直前に共有ヘルパーでflushを行い、先行するfill/textの遅延バッチが後から画像へ被る経路を遮断。前回20枚すべてを累積ZIPへ再同梱し、SkinnedEditBox/SkinnedButton/見本/スクロールの接続クラスもまとめて上書きできる形にする。実クライアントの表示確認は別途必要。


## PC-40 ホバーの見切れ・状態判定・画像割り当てと音
SettingsScrollPanelはスクロール先頭/末尾だけ描画クリップを上下2pxまで拡張し、上端の自動収納トグルが1px浮いても切れないようにする。スクロール中間のクリップ、論理viewport/入力判定/スクロール量/つまみは変えず、外側FloatingWindowのクリップも維持。描画余白上だけにカーソルがある場合は子ウィジェットへ仮座標を渡してクリック不能な場所のホバーを防ぐ。

SkinnedButtonでisHoveredOrFocusedを無条件に使うのをやめ、実マウスホバーかキーボード入力中のフォーカスだけを共通ButtonHoverStateで強調する。クリックで残ったマウスフォーカスが浮き上がり状態を固定し、再ホバー時に動かなく見える問題を防ぐ。スクロールで見えなくなったボタンはホバー状態をリセット。

通常の有効ボタン（優先度/モード等）もbutton_selected_hovered.pngを共有ホバー画像として使用。通常未選択はbutton_normal、選択中タブの通常はbutton_selected。button_hovered.pngは未受領のため新画像は生成しない。自動収納ON/OFFは従来の各toggle_*_hoveredを維持。優先度MIN=0/MAX=9の無効な−/＋は押せず、ホバー画像/演出/音も無効のまま。

有効ボタンへマウスが入った時だけUI_BUTTON_CLICKをpitch1.6・volume0.15で1回再生。ホバー滞在中/キーボードフォーカスだけ/無効状態では鳴らさない。既存クリック音とラジアルのホバー音は維持し二重追加しない。背景透過・PNG原本・装備モデル・クリック処理・PC-39の押下縮小演出は変更しない。
