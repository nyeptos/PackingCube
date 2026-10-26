# 共通フローティングGUIの組み込み

移動・リサイズ・画面端補正・前面描画を、バッグに依存しない部品として使う。バニラの画面へ自動適用したり、バニラのScreenを置換する仕組みではない。

## 部品

- `gui/FloatingWindowState`: 位置・サイズ・四辺と四隅のヒット判定・ドラッグ捕捉。Minecraftのクライアント型に依存しない。
- `client/widget/FloatingWindow`: 状態を所有し、ヘッダー・枠・クリッピング・前面レイヤーの描画を提供する。内容はホストから渡す。
- `client/widget/SettingsScrollPanel`: ウィジェットの所有・スクロール・可視範囲内の入力。汎用コンストラクタではバッグのレイアウトを使わず、ローカル矩形・内容高を渡す。
- `gui/SettingsScrollState`: スクロール量と補正の状態。

状態はScreenインスタンス単位で保持する。staticで共有しない。永続保存は提供しない。

## 初期化と描画

```java
private final FloatingWindow floating = new FloatingWindow();

// init時。初回の推奨位置・寸法、最小寸法、GUI座標系の画面サイズを渡す。
floating.state().configure(defaultX, defaultY, defaultWidth, defaultHeight,
        minimumWidth, minimumHeight, width, height);

// 通常Screenなら背面の描画の後に呼ぶ。引数はスクリーン座標。
floating.render(graphics, font, title, mouseX, mouseY, () -> {
    // 現在の floating.state().x/y/width/height に従って内容を描く。
});
```

- コンテナ画面ではBagScreenを参考に、`renderLabels`でスロットの後・カーソルアイテムの前に呼ぶ。コンテナのleftPos/topPos変換を一旦打ち消してスクリーン座標に戻す。
- `FloatingWindow.render`は前後のbatch flushとZ=400レイヤーを作る。アイテム・テキスト等の遅延描画を境界で確定し、背面の文字だけが前へ出ることを防ぐ。内容のクリップも所有する。
- 自動renderable登録と手動描画を二重に行わない。内容中のスクロールビューポートは独自の内側scissorを使える。
- 実インベントリの寸法・座標をウィンドウ寸法へ追従させない。

## 入力とリフロー

1. ホストの通常ウィジェットより先に `floating.mouseClicked(x, y, button, allowTransform)` を呼ぶ。上端20pxが移動ヘッダー、内側4pxの端がリサイズ用。四隅は二軸リサイズ。
2. アイテムを持っている時などはallowTransform=false。枠クリックは消費するが移動・リサイズしない。
3. `floating.mouseDragged(x, y, button)` がtrueならホストへ入力を流さない。位置変更だけなら子ウィジェットの原点を移す。寸法変更なら内容を再構築する。
4. 再構築時、同じ画面寸法のconfigureは捕捉中の操作とサイズを保持する。検索等の内容状態もScreen側で持つ。画面自体がリサイズされた場合は操作を終了し、画面内に再補正する。
5. `floating.mouseReleased(button)` をポインターが枠外にあっても呼ぶ。捕捉を終えてからホストのスロット処理へ戻す。
6. `floating.contains(x, y)` で背面のクリック・ドラッグ・ホイール・hover・tooltipを遮断する。これはホスト側の責任。BagScreenではisHovering/hasClickedOutsideも接続している。

スクロール内容へ渡す座標とスクリーン座標を混同しない。BagScreenの設定ではSettingsScrollPanel.moveToとFilterEditorPanel.moveOriginを同時更新する。

## 小さい画面

推奨サイズが画面より大きければ縮めて画面内に配置する。最小寸法も画面寸法が優先される。BagScreenでは通常最小124×80、内容は縦スクロールでアクセスする。隣に置けない場合はインベントリへ重ねる。「隣に空きがない」ことを開けない理由にしない。

BagSettingsLayout.floatingはMinecraftで使用するGUIサイズを対象とし、設定パネル幅64以上・高さ48以上のレイアウトを受け付ける。通常のMinecraftの最小GUIより小さい特殊なホストでは、自前の内容レイアウトまたは縮尺を使う。FloatingWindowState自体は1px以上の画面内補正に対応する。

## 確認項目

- 背面アイテムと×文字より枠・内容が前に出るか。
- 背面のスロットを誤操作しないか。カーソルの実アイテムが正しく表示されるか。
- 四辺・四隅で逆の端を固定してリサイズできるか。
- 縮小後もスクロールで末尾まで操作できるか。検索・ルール・優先度がリセットされないか。
- 画面の拡縮・操作途中の解放・別画面への遷移で捕捉が残らないか。

自動GameTestは幾何・状態遷移の検証。描画と実ポインター操作は実クライアントで別途確認する。

## 説明ポップアップの前面レイヤー（PC-34）

ウィンドウ本体Z=400と、バニラのツールチップZ=400は同じ深度のため、通常の最終描画に任せるだけでは説明背景よりウィンドウ内の文字・アイテムが前へ出る場合がある。

- ホストの`setTooltipForNextRenderPass(List<FormattedCharSequence>, ClientTooltipPositioner, boolean)`をオーバーライドし、ウィンドウ表示中だけ`floating.queueTooltip`へ渡す。表示していない時はsuperへ渡す。Component/Tooltipのオーバーロードはこの経路へ集約される。
- コンテナ等の本体描画の終了後、`floating.renderTooltips(graphics, font, mouseX, mouseY, directTooltips)`を一度呼ぶ。`directTooltips`へ見本枠・アイテムなどの直接描画する説明を渡す。遅延ボタン説明も同じ呼び出しで描画される。
- 共通部品がscissor外で前後flushと追加Z=400を適用する。標準の説明背景・本文は合計Z=800となり、設定本体Z=400や内容アイテムより前になる。位置決定アルゴリズムと画面端補正は標準のまま。
- `clearTooltipForNextRenderPass`のホストオーバーライドではsuperと`floating.clearTooltips`を両方呼ぶ。ウィジェット再構築時・画面終了時にもclearし、古い説明を残さない。
- `TooltipQueue`は最初の要求を保持し、override=trueで差し替える標準Screenの規則を維持し、一度描画した要求は消費する。
- Screen.renderWithTooltipやバニラ本体には変更しない。組み込んだホストのみ適用する。BagScreenが実装例。

### カーソル上の実アイテムと個数

設定を前面にしたまま標準のカーソルアイテムがZ=382、個数がZ=432で描かれると、本体だけがウィンドウに隠れて個数だけが見える。ホストの`renderLabels`末尾、すべてのウィンドウ描画後に`FloatingWindow.prepareContainerCursorLayer(graphics)`を一度だけ呼ぶ。

この呼び出しはコンテナが所有するposeの残りをZ=250だけ上げる。続く標準の持ち上げアイテム／個数／snapback描画はそのまま利用し、コンテナ自身のpopで復元される。独自のItemStack再描画・複製・メニュー書き換えは行わない。普通のScreenやコンテナrenderの外では使わない。

共通の`GuiRenderLayers`へ値を集約。MC1.21.1の標準オフセットを含めた順序は、設定本体400（内容アイテム550／個数600まで）＜カーソル本体632／個数682＜説明背景・本文800。型ごとの深度差があるので、枠だけの順序確認では不十分。新しいホストでも持ち上げ描画の接続と説明キューの接続を両方行う。バニラ本体を変更する機能ではない。
