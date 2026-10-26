# コード構成と変更時のルール

## 主な責務

| 場所 | 責務 |
|---|---|
| `Packingcube` | MOD IDと起動時の配線だけ |
| `registry/ModRegistries` | レジストリ登録の入口。MODライフサイクルバスへ接続 |
| `event/ModGameEvents` | ゲームバスへのイベント登録。既存の順序・優先度を維持 |
| `registry/ModItems` | バッグなどのアイテム登録 |
| `registry/ModMenus` | 管理画面・ラジアルのMenuType登録 |
| `registry/ModCreativeTabs` | クリエイティブタブ登録 |
| `data` | アイテムモデルとレシピのdatagen |
| `bag` | 収納、フィルター、バッグ識別、振り分け、入力・座標の共通判定 |
| `menu` | サーバーメニューと同期状態、検証済み画面遷移 |
| `client` | GUI、キー、ラジアル音、カーソル保持。共通コードから参照しない |
| `compat` | Curios未導入でも動く境界と、導入時のみ読み込むAPI実装 |
| `network` | 既存Payloadのcodecとサーバー側ハンドラー |
| `gametest` | 収納・フィルター・装備・メニュー・入力状態などの回帰テスト |

## 守る互換性

- 登録IDは `packingcube:large_bag`、`filter_bag`、`bag`、`bag_radial`、`bags` のまま。
- `DataComponents.CONTAINER` と `packingcube_filter` の保存形式は変えない。
- PayloadのID、プロトコルバージョン、フィールド順・数値操作IDは変えない。フィルター操作番号は `network/FilterActions` へ集約した。
- 権限・カーソル保持・menu ID・バッグ所在の検証はクライアントへ移さない。
- Curios型を `BagEquipment` の共通メソッドの引数・戻り値へ漏らさない。
- B/C、ラジアル選択、ghost sample、フィルター、カーソル保持は既存動作を維持する。

## バッグを開く処理

`BagMenuOpener.open` がBagMenuの通信形式と開く音を共通化する。

1. 呼び出し元で操作権限と対象を確定する。
2. 必要な旧容量の超過回収を行う。
3. 共通のopenerでサーバーの `openMenu` を実行する。
4. 実際に開けた場合だけ、Big Bagに革防具の装備音を本人へ一度通知する。

右クリック、B、Filter BagからBで戻る、ラジアルの管理ボタンが同じ経路を使う。閉じる・拒否された要求・Filter Bag単体・ラジアル自体ではBig Bagの開く音を鳴らさない。

## 音の分担

- `BagSounds`：バニラ革防具の音・標準volume/pitch。Big Bagの開く音とCurios装備音で共用する。
- `CuriosCompat#getEquipSound`：Curios標準の装備処理へ革音を返す。`onEquip` で直接再生しないので、収納やフィルターのデータ更新が装備音を連打しない。
- `BagRadialScreen`：既存の高めの選択・決定クリック音だけをクライアント再生する。

## アイテムを増やすとき

1. `ModItems` へ既存IDと衝突しない名前で登録する。
2. 必要なら `ModCreativeTabs` へ表示を追加する。
3. `ModItemModelProvider` と翻訳へ追加する。テクスチャは作者が作る。
4. 材料を決めてから `ModRecipeProvider` へレシピを書く。
5. `runData`、ビルド、GameTestを実行し、生成JSONもコミット対象へ含める。

Javaは4スペースのAOSP書式で整形済み。今回の大きな行差分には書式統一とimport整理が含まれる。機能変更は音・登録クラスの責務分離・datagen・操作番号定数化・開く処理の共通化に限定した。


## カーソル解放の注意

バッグの位置復元は画面init後ではなくadded内で行う。捕捉解除時の中央ジャンプを避けるため、HIDDENでdisabledモードを抜け、MouseHandlerの座標・捕捉フラグ・差分を同期してから保存位置を表示する。必要な5フィールドだけをAccess Transformerで公開する。一般の画面やワールドへ戻るgrabMouseを改変しない。ATの変更はホットリロードではなく、Gradle再読み込みとクライアント再起動で確認する。

## 自動収納の設定と優先順位

- 永続設定は `bag/BagPickupSettings`、適用順の一元管理は `bag/BagRouter` が所有する。
- ポーチ内のフィルターと自動拾得の有効フラグは独立。手動収納は自動フラグを参照せずフィルターのみを使う。
- 優先度はポーチ本体の0〜9。全有効バックパックを横断して降順、同値なら元の装備／スロット順。アイテム種別やタグの「専用性」による隠れた優先は足さない。
- 複数の候補が同じ親にある場合も、同一の短命BagInventory親ビューを共有して保存を上書きし合わない。画面で編集中の親は候補に入れない。
- FilterActionsのID0〜4を維持して5・6を追加。ネットワークではアイテム実体を受信せず、現在サーバーで所有が確認できる画面の実アイテムへだけ設定を適用する。

## フィルター設定UIの共通化

`gui/BagSettingsLayout` と `gui/FilterEditorState` はクライアントGUI型に依存しない座標／状態モデル。`client/widget/FilterEditorPanel` が再利用可能な編集部品を持ち、バッグ固有のメニューや通信方式へは `Access` で接続する。

`BagScreen` は実スロットとB/C遷移・カーソル・自動収納設定のホストに限定する。固有設定はレイアウトで行数を予約し、共通フィルター内にポーチ固有のON/OFFや優先度を埋め込まない。配置・入力接続・再利用の詳細は `docs/GUI.md`。

### 右側の共通スクロール領域

SettingsScrollStateはGUI非依存の範囲・表示モデル。SettingsScrollPanelは右側の全ウィジェットを所有してクリッピングとスクロール入力を担当する。左の実収納とプレイヤーインベントリはスクロールしない。FilterEditorPanelは論理座標で内容を描画し、Screenが見本入力とツールチップの座標変換を接続する。中央線は削除。

## 装備モデルのクライアント境界

ClientEventsのCurios導入判定後だけclient/compat/CuriosClientCompatを呼び、追加モデルとICurioRendererを登録する。背負い描画はCuriosBackpackRendererが担当。共通の装備判定／収納／同期処理には描画依存を追加しない。モデル・画像は通常resources配下に置き、アイテムのdatagen出力とは分離する。

## 可変の右設定高と共用アイテムモデル

左インベントリの高さ132と右設定高を分離。右だけ画面高-8を上限に内容末尾+8まで拡大する。右背景のX176以降のみの範囲は維持する。large_bagのdatagenモデルはbackpack_equippedをparentとして参照し、形状・テクスチャ・displayを複製／上書きしない。Blockbenchの表示設定と、NONEコンテキストで行う装備描画の位置補正を分離する。ユーザー調整済みrendererファイルは今回変更しない。

## ポーチ単位の自動収納と固定インベントリ横パネル

BagRouterはバックパック旧enabledを参照しない。BagMenuの有効化ID5はポーチのみ受理し、バックパック要求は拒否。バックパックスナップショットは退役した全体設定をコピーしない。BagPickupSettingsに旧値が残っていても破壊的移行はしない。

BagScreenは常時176×132で通常の中央座標を維持。右設定だけ独立したsettingsTopを使い、レイアウトは残りの横幅を基に生成する。背景、ウィジェット、見本と入力を同じ右原点へ揃え、outside判定も右背景を含める。横幅不足では左を動かさず説明を出す。

## 共通フローティングGUI（PC-33）
`gui/FloatingWindowState`はバッグ非依存の矩形・移動・端リサイズ・画面内補正。`client/widget/FloatingWindow`は枠・入力アダプタ・batch境界・前面レイヤー。内容や実スロット遮断・通信は各Screenが接続する。バニラへの変更・Mixin・全画面への自動適用はしない。再利用手順は`FLOATING_WINDOWS.md`。

## ウィジェットスキン（PC-35）
WidgetSkin/NineSliceAxisはクライアント非依存の状態選択と伸縮計算。WidgetTextures/SkinnedButton/SkinnedEditBoxはPackingCube画面だけに適用する描画アダプタ。受領PNGは専用namespaceに無加工で梱包し、バニラのatlas/spriteは上書きしない。ボタン入力・説明・音・ナレーションは標準クラスを継承。


## PC-53：登録入口とイベントバスの分離

`Packingcube` → `ModRegistries.register(modBus)` → アイテム・クリエイティブタブ・メニューの順で登録する。各DeferredRegisterの所有者と公開フィールドは変更しない。`packingcube:refill_pouch`を含む既存IDを維持する。

ゲーム中の12件のイベント購読は `ModGameEvents.register(NeoForge.EVENT_BUS)` に集約する。うち8件のLOWEST優先度と登録順は従来どおり。ネットワーク・datagen・任意Curios連携のライフサイクル登録は別責務として起動クラスに明示する。クライアント専用のClientEventsは変更しない。

新しい登録を追加するときは該当する所有クラスへ追加し、新種のレジストリだけModRegistriesへ入口を足す。ゲームイベントはModGameEventsへ追加する。自動探索や登録の二重化は行わない。
