# PC-54：パッケージの責務別分割

既存クラスを移動し、packageとimportを更新する。アイテムID・保存形式・通信形式・実行処理は変更しない。

## bag

- 直下：BagInventory / BagIdentity / EquippedBagAccess / BagSounds（共通の収納・識別・アクセス・音）
- filter：フィルター判定とツールチップ
- pickup：拾得時の収納設定と振り分け
- refill：補充設定・搬出・補充先・診断
- input：キーとカーソルの状態
- radial：ラジアルの配置・選択状態

## gui（クライアント非依存の計算・状態・仕様）

- layout：設定画面とラジアルの配置
- window：ウィンドウの移動・サイズ・位置
- input：フィルター編集・補充設定表示・補充先選択・スクロールの状態
- animation：ボタンやスクロールバーのフィードバック状態
- skin：テクスチャ状態・9-slice・描画領域/レイヤー・ツールチップ待ち行列の共通仕様

Minecraftの実描画やウィジェットはclientに残す。クライアント専用型への新たな依存は追加しない。

## gametest

- bag：収納と手動格納
- pickup：自動収納
- refill：補充と補充先指定・実使用
- gui：配置・入力・アニメーション・GUI素材
- compat：Curios・装備・装備モデル
- support：補充テストとCuriosテストが共用するLegacyRefillFixture
- 直下：複数の責務を横断するMaintenanceGameTests

共有フィクスチャだけクラスとpouch()をpublicにする。それ以外の公開範囲は変えない。
クラス名・テストメソッド名・テンプレート名・注釈は維持する。

## 適用

PC-52適用済みでPC-53未適用ならstandalone版、PC-53も適用済みならafter-PC53版を使う。どちらか1つだけ適用する。standalone版にもID/既定値の回帰検査を含め、最終テストコードを揃える。レジストリ起動配線は変更しない。
IDEAでClientを停止し、プロジェクトルートを基準にApply Patchする。新しい場所への追加と旧ファイルの削除を両方適用する。旧クラスを残すと重複や誤importの原因になるため、追加ファイルだけコピーしない。
このパッチにPNG・モデル・言語ファイルは含まれない。

## 検証

Curiosあり253/253、なし234/234のGameTestおよび各build成功。既存と同数のテストが検出されることを確認。
全Javaについてpackage/importと共有フィクスチャのpublic化を除く本体の機械比較を実施。実クライアントのGUI・音は今回未検証。

## 移動一覧

- `bag/BagFilter.java` → `bag/filter/BagFilter.java`
- `bag/RadialGeometry.java` → `bag/radial/RadialGeometry.java`
- `bag/BagKeyActions.java` → `bag/input/BagKeyActions.java`
- `bag/BagKeyLatch.java` → `bag/input/BagKeyLatch.java`
- `bag/RadialKeyState.java` → `bag/radial/RadialKeyState.java`
- `bag/BagFilterTooltip.java` → `bag/filter/BagFilterTooltip.java`
- `bag/RadialSelectionFeedback.java` → `bag/radial/RadialSelectionFeedback.java`
- `bag/BagCursorPosition.java` → `bag/input/BagCursorPosition.java`
- `bag/BagCursorRelease.java` → `bag/input/BagCursorRelease.java`
- `bag/BagPickupSettings.java` → `bag/pickup/BagPickupSettings.java`
- `bag/BagRouter.java` → `bag/pickup/BagRouter.java`
- `bag/RefillSettings.java` → `bag/refill/RefillSettings.java`
- `bag/RefillSupply.java` → `bag/refill/RefillSupply.java`
- `bag/RefillService.java` → `bag/refill/RefillService.java`
- `bag/RefillDiagnostics.java` → `bag/refill/RefillDiagnostics.java`
- `bag/RefillTargets.java` → `bag/refill/RefillTargets.java`
- `bag/RefillSlotSettings.java` → `bag/refill/RefillSlotSettings.java`
- `bag/RefillDestination.java` → `bag/refill/RefillDestination.java`
- `bag/RefillDispatcher.java` → `bag/refill/RefillDispatcher.java`
- `gametest/BagGameTests.java` → `gametest/bag/BagGameTests.java`
- `gametest/RadialBagGameTests.java` → `gametest/gui/RadialBagGameTests.java`
- `gametest/PickupSettingsGameTests.java` → `gametest/pickup/PickupSettingsGameTests.java`
- `gametest/SettingsLayoutGameTests.java` → `gametest/gui/SettingsLayoutGameTests.java`
- `gametest/SettingsScrollGameTests.java` → `gametest/gui/SettingsScrollGameTests.java`
- `gametest/BackpackAppearanceGameTests.java` → `gametest/compat/BackpackAppearanceGameTests.java`
- `gametest/SettingsPanelPositionGameTests.java` → `gametest/gui/SettingsPanelPositionGameTests.java`
- `gametest/FloatingWindowGameTests.java` → `gametest/gui/FloatingWindowGameTests.java`
- `gametest/TooltipQueueGameTests.java` → `gametest/gui/TooltipQueueGameTests.java`
- `gametest/GuiRenderLayersGameTests.java` → `gametest/gui/GuiRenderLayersGameTests.java`
- `gametest/WidgetSkinGameTests.java` → `gametest/gui/WidgetSkinGameTests.java`
- `gametest/GuiSurfaceGameTests.java` → `gametest/gui/GuiSurfaceGameTests.java`
- `gametest/RadialPouchArtGameTests.java` → `gametest/gui/RadialPouchArtGameTests.java`
- `gametest/ButtonAnimationGameTests.java` → `gametest/gui/ButtonAnimationGameTests.java`
- `gametest/ButtonHoverGameTests.java` → `gametest/gui/ButtonHoverGameTests.java`
- `gametest/ScrollbarSoundGameTests.java` → `gametest/gui/ScrollbarSoundGameTests.java`
- `gametest/CuriosBagGameTests.java` → `gametest/compat/CuriosBagGameTests.java`
- `gametest/ManualStowGameTests.java` → `gametest/bag/ManualStowGameTests.java`
- `gametest/RefillPouchGameTests.java` → `gametest/refill/RefillPouchGameTests.java`
- `gametest/RefillControlsGameTests.java` → `gametest/refill/RefillControlsGameTests.java`
- `gametest/RefillRealUseGameTests.java` → `gametest/refill/RefillRealUseGameTests.java`
- `gametest/RefillClarityGameTests.java` → `gametest/refill/RefillClarityGameTests.java`
- `gametest/RefillTargetGameTests.java` → `gametest/refill/RefillTargetGameTests.java`
- `gametest/RefillSourceGameTests.java` → `gametest/refill/RefillSourceGameTests.java`
- `gametest/LegacyRefillFixture.java` → `gametest/support/LegacyRefillFixture.java`
- `gametest/BagEquipmentGameTests.java` → `gametest/compat/BagEquipmentGameTests.java`
- `gametest/RefillOpeningGameTests.java` → `gametest/refill/RefillOpeningGameTests.java`
- `gui/BagSettingsLayout.java` → `gui/layout/BagSettingsLayout.java`
- `gui/FilterEditorState.java` → `gui/input/FilterEditorState.java`
- `gui/SettingsScrollState.java` → `gui/input/SettingsScrollState.java`
- `gui/SettingsPanelPosition.java` → `gui/window/SettingsPanelPosition.java`
- `gui/FloatingWindowState.java` → `gui/window/FloatingWindowState.java`
- `gui/TooltipQueue.java` → `gui/skin/TooltipQueue.java`
- `gui/GuiRenderLayers.java` → `gui/skin/GuiRenderLayers.java`
- `gui/WidgetSkin.java` → `gui/skin/WidgetSkin.java`
- `gui/NineSliceAxis.java` → `gui/skin/NineSliceAxis.java`
- `gui/GuiSurface.java` → `gui/skin/GuiSurface.java`
- `gui/RadialLayout.java` → `gui/layout/RadialLayout.java`
- `gui/RadialSlotSkin.java` → `gui/skin/RadialSlotSkin.java`
- `gui/ButtonAnimation.java` → `gui/animation/ButtonAnimation.java`
- `gui/ButtonHoverState.java` → `gui/animation/ButtonHoverState.java`
- `gui/ScrollbarSoundState.java` → `gui/animation/ScrollbarSoundState.java`
- `gui/RefillSettingsView.java` → `gui/input/RefillSettingsView.java`
- `gui/RefillTargetInteraction.java` → `gui/input/RefillTargetInteraction.java`
- `gui/SlotButtonAnimation.java` → `gui/animation/SlotButtonAnimation.java`
