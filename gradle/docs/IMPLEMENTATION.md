# 最初の実装範囲

この版は提案仕様すべての実装ではなく、収納・持ち運び・自動振り分けを確かめる縦切りの試作。

| ファイル | 役割 |
|---|---|
| `Packingcube` | アイテムとクリエイティブタブの登録、共通イベント登録 |
| `BagItem` | バッグの右クリック使用、単体収納禁止の宣言 |
| `BagInventory` | コンポーネントからの復元、変更ごとの保存、部分収納 |
| `BagFilter` | タグ・アイテムID・除外条件の保存と判定 |
| `BagRouter` | 大型バッグ→モジュールの順に残数を振り分け |
| `BagMenu` | 両側共通のスロット制限、本体の追跡と移動対応。Big Bag画面は装着・取り外し専用 |
| `BagScreen` / `ClientEvents` | クライアント専用の画面描画・登録 |
| `BagCommands` | 自分のメインハンドのバッグに対する暫定フィルター編集 |
| `BagPickupHandler` | 拾得前イベントとの接続、遅延・所有者・残数の扱い |

## 保存

- 中身：バニラの `DataComponents.CONTAINER` / `ItemContainerContents`。
- フィルター：`DataComponents.CUSTOM_DATA` 内の `packingcube_filter`。
- フィルターのキー：`tags`, `items`, `exclude_tags`, `exclude_items`。
- カスタム名：バニラのアイテム名変更を使用できる。識別色の専用設定は未実装。
- 内容変更はサーバー側で確定。描画専用のクライアントクラスは共通コードから参照しない。

## スナップショットの扱い

`BagInventory` は短命なビューであり、同じバッグを複数の書き込み元から同時に開かない。
装着中モジュールの保存時は親の保存処理を呼ぶ。
バッグGUIが開いている間、そのルートバッグを自動収納対象から除外して競合を防止する。
将来GUI中も自動収納する場合は、既存のライブビューへ振り分ける仕組みを追加する。

## 次の段階

まずゲーム内で取り出し・交換・フィルターGUIと保存の感触を確認。その後、優先順の操作を作る。
廃棄は収納経路が安定してから別の機能として追加する。

## 9枠化に伴う保存データの保護

旧容量からはみ出すスロットは内部の退避リストへ保持し、保存時にもコンポーネントへ残す。
GUIには9枠のみ表示。開くときにプレイヤーの通常インベントリ36枠へ移せる分だけ回収する。
クリエイティブの `Inventory.add` による余剰消去を避け、専用の数量保存型挿入処理を使う。

## フィルターGUIと通信

- `FilterActionPayload` は操作種別、開いているメニューID、スロット番号、フィールド名、IDだけを送る。
- クライアントが任意のItemStackを送る仕組みにはしない。見本はサーバーの実スロットからコピーする。
- アクティブな自分のBagMenuであること、ルートバッグの所在、所在と操作の妥当性を検証。カーソル保持中も見本登録・設定編集は可能。
- 追加操作では、個別アイテムID／タグが見本と一致することをサーバーで確認。
- 登録・削除で収納アイテムを移動しない。内部BagInventoryの保存を呼び、装着時は親へ伝播する。
- 登録ルールは4フィールド合計256件まで。通信文字列長も制限。
- メニュー末尾に、フィルター表示用スナップショットと見本コピーの非表示同期スロットを2つ追加。
- 非表示スロットは描画・通常クリック・Shift移動・数字キー交換・取得対象から除外。実物として取り出せない。
- フィルター表示用スナップショットには収納コンポーネントを含めない。
- GUIでの見本選択やルール変更の表示は、通常のメニュースロット同期でサーバーから更新。
- 全クライアントとサーバー双方へ同じ更新版を導入する。


## 排他的フィルターモード

`packingcube_filter.mode` に `include` / `exclude` を保存。欠落時は `include`。
共通の2フィールド（タグ／アイテム）を両モードで使い、excludeでは一致結果のみを反転する。

- サーバーの `BagInventory.canPlaceItem` と自動振り分けで同じ `BagFilter.matches` を使う。
- クライアント／サーバーの `BagMenu` のスロットも、同期済みフィルター状態で同じ条件を使う。
- バニラの `moveItemStackTo` は既存スタックへの合流時に `mayPlace` を呼ばないため、Shift挿入前にも判定する。
- モード切り替えは操作種別3。サーバーで値を検証し、保存と同期を行う。
- 既存の対象外アイテムは移動・廃棄しない。取り出し操作を制限しない。


## 共通リストへの統合

- 正式な保存先は `tags` / `items`。モードは独立した `mode`。
- 旧 `exclude_tags` / `exclude_items` も読み込み、重複を除いた共通リストを表示する。
- ルール編集またはモード保存時に対応する旧フィールドを統合し、削除済み指定の復活を防ぐ。
- GUIの追加・削除・登録表示は常に共通フィールドを使う。
- 条件数上限は別名を二重カウントせず、共通のタグ＋アイテム件数で判定する。

## 開いている本体を追跡する方式

固定スロット位置での検証を廃止。

- `BagIdentity` がルートバッグの `CUSTOM_DATA.packingcube_open_id` に開くたび新しいUUIDを付ける。
- `BagMenu.stillValid` とクリック処理後に、プレイヤーの全インベントリ・オフハンド・カーソルを走査。
- 一意に一致するItemStackがあれば追跡し、実体参照が変わった場合はルートBagInventoryをrebindする。
- `BagInventory` は親ビューを保持でき、装着モジュールからルートビューへ到達する。
- 再バインド時にライブ内容を現在の本体へ保存。子の変更も親の保存処理を通じて新しい本体へ保存する。
- 本体がなくなった／同じIDが重複した場合、操作を止めてサーバー側でメニューを閉じる。
- BagRouterも参照一致ではなく追跡IDで開いている本体を判定し、二重の書き込みを防ぐ。
- 非表示の設定同期スロットは、従来どおり取り出し・操作不可。
- 初期スロット番号は既存のメニュー生成・通信引数に残るが、移動制限や追跡には使用しない。

## カーソルから非消費で登録する見本枠

- 画面右側の設定領域にクリック領域を持つゴースト枠を描画（実Containerスロットとしては操作しない）。
- 操作種別4で `copy` / `clear` を送る。クライアントからItemStackやアイテムIDは送らない。
- サーバーの実カーソルを読み、`copyWithCount(1)` で同期用見本コピーを作る。
- `split` / `shrink` / 挿入／返却／投棄は行わず、元カーソルを変更しない。
- 置き換え時に旧見本をアイテムとして返却しない。右クリックと空カーソルからの左クリックは見本だけ消す。
- 画面の実スロットのクリック横取りは廃止。通常の収納・取り出し操作と見本枠を分離する。
- 見本枠へドラッグを離した場合、バニラの保留中の分配処理を取り消してコピー処理だけを行う。
- 従来の操作種別0は既存テスト・互換用に残るが、画面からは送らない。
- 同期用の非表示スロットは引き続き取得不可。


## Curios 9.x 任意連携

- `BagEquipment`が依存境界。ModList確認後だけCurios型を含む`CuriosCompat`を呼ぶ。BagItem自体にはICurioItemを実装しない。
- RegisterCapabilitiesEventでBig BagにのみICurioを追加。タグ`curios:back`とプレイヤーへのback割り当てをデータで定義。実装備back以外のcanEquipはfalse。
- `BagRouter`は有効なback実装備→通常36枠の順。装備中のルートも既存の「開いているバッグを自動処理から除外」ルールを守る。
- `BagMenu.locateRoot`はインベントリ・カーソルに加え、back実装備も追跡。親本体が移動したときはrootViewを再バインドする。
- onSaveでは装備スロットへキャプチャ済みItemStackを書き戻さない。コンポーネントをライブスタックへ保存し、Curios自身の毎tick比較・同期・セーブを使う。装備解除後の古いスロットへの復活を防ぐ。
- Bキーはアイテムやスロットを持たない`OpenEquippedBagPayload`だけ送信。サーバーは本人の有効back装備、なければ通常36枠からBig Bagを解決し、生存・非観戦・通常インベントリメニュー・空カーソルを確認してラジアルを開く。
- Curios専用7項目はCurios導入時だけRegisterGameTestsEventで登録。未導入起動ではクラスをロードしない。共通テスト36項目はどちらでも実行する。
- compileOnly API / runtimeOnly 本体。開発用`-PwithoutCurios=true`でruntimeOnlyを除外して任意依存を検証できる。

参考：
- https://docs.illusivesoulworks.com/curios/getting-started
- https://docs.illusivesoulworks.com/curios/items/curio-creation
- https://docs.illusivesoulworks.com/curios/items/assign-slots
- https://docs.illusivesoulworks.com/curios/slots/entity-register


## ラジアルによるモジュールアクセス

- `BagRadialMenu`：サーバー管理の読み取り専用選択メニュー。9個の同期スロットにはFilter Bagの種類・カスタム名・正規化したフィルター設定だけを送る。収納コンポーネントや識別ID・無関係なカスタムデータは送らない。実データのコピーはサーバーのexpected配列でのみ保持。
- `BagRadialScreen`：通常スロット処理を使わず、9方向・中心待機・クリック・数字キー・構成ボタン・再押下で閉じる操作を実装。IN_GAME KeyMappingの状態はGUI中に落ちるため、設定済み実キー／マウスボタンを参照して押下状態を更新する。キーを離しても決定せず、左クリックまたは数字キーで明示的に選択する。
- `RadialGeometry`：方向判定・座標配置の共通計算。上から時計回りの9枠、30pxの中央待機領域。空き枠を詰めない。
- ネットワークは開く要求と選択要求を分離。選択は`RadialSelectPayload(containerId, moduleSlot)`のみ。範囲は0〜8、明示的な構成ボタンは-1。
- 選択時に本人の現在のRadialMenuとmenu IDを照合し、ルートIDが一意であることと実モジュールが表示時のサーバースナップショットに一致することを確認する。閉じたメニューや追加・交換後のモジュールは開かない。
- 選択成功後はサーバーが通常のBagMenuへ遷移する。クライアントから先にclose packetを送ると選択要求が無効化されるため、遷移通知を待つ。拒否された現メニューはサーバー側で閉じて待機し続けないようにする。
- `BagRootMenu`をBagMenu/RadialMenuで共用し、BagRouterの独立ライター除外に使用。`BagIdentity.findUnique`で両メニューのルート探索を統一。
- Big Bag画面の「空カーソルで右クリックしてFilter Bagへ遷移」の分岐は削除。通常の装着・取り外しクリックとして処理する。
- 画面はゲームをポーズしない。フォーカス喪失時はキャンセル。通信待ちで開く前にキーを離していてもクリック選択を待つ。


## 独立したキー割り当て

- `key.packingcube.open_radial`と`key.packingcube.open_big_bag`を標準キー設定へ登録。デフォルトはopen_big_bag=B、open_radial=C。Minecraft Optionsの保存済み割り当ては上書きしない。
- 旧open_back_bagは登録・翻訳から削除し、Bキーの古い割り当てを別用途へ流用しない。保存はMinecraft Optionsへ任せ、専用の設定ファイルは増やさない。
- 2つの入力キューを毎tick消費し、共通の`BagKeyActions.choose`で1つだけを選択。同一実キー＋同一修飾キーの場合、片側のKeyMappingにしかクリックが届かなくてもラジアルを優先する。
- キー再割り当て後の押下状態とRadialScreenのキーイベントはOPEN_RADIALだけを参照。実キー・マウスボタン・修飾キーを照合する。
- `OpenEquippedBagPayload`はラジアル要求として維持し、直接管理用の`OpenBigBagPayload`を追加。どちらも任意のアイテム・スロットをクライアントから受け取らない。
- `EquippedBagAccess`のcanOpenとprimaryBagを共通化。openはラジアル、openDirectはmodules=trueのBagMenuを開く。同じ生存・非観戦・inventoryMenu・空カーソル検証を使う。
- Curios側のキー設定や他MOD側の割り当ては変更しない。


## 画面内のB／Cナビゲーション

- 通常画面用のopen/openDirectは引き続きinventoryMenu限定。バッグ画面内では別のBagNavigationPayload(containerId, radial)を使う。
- navigateは本人の現在BagMenu・menu ID・生存・非観戦・空カーソル・stillValidを検証。古い画面の再送で遷移後の画面を閉じたり開いたりしない。
- Bではmodules=trueなら閉じ、Filter Bagなら親Big Bagへ。Cでは現在の親のRadialMenuへ。stillValidの再バインド後にrootBagを読むため、移動した本体も追跡する。親がBig Bagでない単体Filter Bagだけ通常のprimaryBag探索へフォールバック。
- BagScreenが設定済みキー／マウスを受け取り、サーバーの画面遷移を待つ。切替前のclose packet送信はしない。検索欄への入力は従来どおり優先。
- RadialKeyStateは初回キーの保持・離す・新規押下を区別する。releaseは押下状態を解除するだけで、選択アクションを持たない。次の新規押下で閉じる。初回のキーリピートを二度目と誤認しない。
- BagKeyLatchはワールドとバッグ画面で入力済み状態を共有し、閉じた直後に同じ長押しのリピートで再オープンするのを防ぐ。
- 表示の大きさやアイコン配置は変更せず、中心の文言を選択案内と再押下で閉じる案内へ変更。

## ラジアル選択音とフィルター表示

- BagItem.appendHoverTextはFilter BagだけBagFilterTooltipへ委譲。共通コードはクライアントクラスを参照しない。
- BagFilterTooltipは含む／含まないを明示し、登録条件のOR一致を基に収納／除外する旨を表示。タグ→アイテム順で最大8件、残数案内、未知アイテムはID表示。空リストの挙動も表示する。読み取りだけで保存値を書き換えない。
- BagFilter.copySettingsは既存形式の共通／旧除外リストを読んで、正規化したフィルター設定のみを表示アイコンへ転記。無効IDを除外し最大256条件に制限。収納、識別ID、無関係なcustom_dataは送らない。
- BagRadialScreenは標準のItemStackツールチップ描画を使い、実アイテムと同じ要約を表示。通常スロットのクリック処理には戻さない。
- RadialSelectionFeedbackは直前の有効ターゲットを追跡し、対象変化時のみ音を鳴らす。mouseMovedとrender両方から呼んでも二重再生しない。中央・空枠は無音で追跡を解除する。
- 音はクライアント専用SimpleSoundInstance.forUIでUI_BUTTON_CLICK、移動pitch=1.6、確定要求pitch=1.8、volume=0.25。選択送信済みフラグで再送・確定音重複を防止。ワールド音の配信や専用音声資産は追加しない。

## バッグ共通のカーソル位置保持

- BagRadialScreen.initのラジアル中心へのglfwSetCursorPosは削除済み。
- BagScreen／BagRadialScreenのadded（Minecraft.releaseMouseより前）とremoved（カメラ用grabMouseより前）で現在位置を扱う。GUIが既に表示中なら位置を記憶するだけで、ネイティブのカーソル状態を書き換えない。
- BagCursorMemoryはクライアント専用。捕捉中かつ記憶位置がある場合は、addedでBagCursorReleaseを呼び、非表示→内部状態同期→座標移動→表示の順に解放する。解放後はmouseGrabbed=falseなのでMinecraft標準のreleaseMouseは中央移動を行わない。
- BagCursorReleaseは座標解決とバックエンドの順序を共通ロジックとして検証する。GLFWがdisabled解除時に以前の中央位置を戻しても、その段階はHIDDENで表示しない。
- MouseHandlerのmouseGrabbed/xpos/ypos/accumulatedDX/accumulatedDYのみAccess Transformerで公開。内部のホバー座標とネイティブ座標を表示前に同期し、移動差分をゼロにする。setIgnoreFirstMoveで合成移動を実ドラッグとして扱わせない。
- BagCursorPositionはウィンドウ座標を正規化して保持し、GUIスケールに依存せず、リサイズ後の範囲内へ補正する。無効値・最小化サイズは無視する。
- initには復元処理がないため、編集パネルのrebuildWidgetsやリサイズで古い位置へ戻さない。WeakReferenceでワールドを比較し、切り替え時は記憶をクリアする。
- 記憶がない初回は標準解放に任せる。非アクティブ時は独自復元しない。ワールドへ閉じる際のバニラgrabMouseや、他MODのGUIは変更しない。
- AT追加を反映するには完全なクライアント再起動とGradle再読み込みが必要。メニューID・収納・フィルター・通信形式は変更しない。

## レジストリ・音・datagen・保守性の整理

- 登録責務をregistry/ModItems、ModMenus、ModCreativeTabsへ分離し、全参照を実際の所有クラスへ移動した。IDは一切変更していない。
- BagMenuOpenerで起動の通信パラメータと成功後のBig Bag音を共通化。BagSoundsは革防具装備音を返し、開く本人へのplayNotifySoundに使用する。
- CuriosはgetEquipSoundを差し替え、標準装備経路から革音を再生する。onEquipへ音を足さず、装備データ変更による連打と二重再生を避ける。
- ModDataGeneratorsでincludeClient/includeServerを尊重してItemModelProviderとRecipeProviderを登録。主ソースの手書きモデル2つは生成リソースへ移し、見た目を保ったまま二重管理を解消した。
- レシピは追加場所と実行経路を提供し、未決の材料・コストは実装しない。
- 既存の数値フィルター操作をFilterActionsへ集約し、Payload形式は維持。ネットワークregistrar共有、完全修飾ローカル型のimport化、Java全体のAOSP整形を実施。
- 設計の見取り図と追加手順はARCHITECTURE.md、DATAGEN.mdへ分離した。


## ラジアルの左クリック決定

- キーrelease／毎tickの実キー解放検出から選択送信へ到達する経路を削除した。RadialKeyStateからSELECTも除去。
- Cは開閉だけ、有効な方向の左クリックまたは既存の数字キーは選択だけを行う。中心・空き枠のクリック、右クリック、Escの既存キャンセル動作は維持。
- 押したままかどうかにかかわらず、有効バッグの案内は「左クリックで決定」。日英の不要なrelease案内キーは削除。
- 通信内容、サーバーの選択検証、カーソル保持、音、プレビューの安全性に変更なし。

## 日英翻訳のdatagen

`ModLanguageProvider` はNeoForgeの `LanguageProvider` を継承し、英語・日本語の文言をキーごとに一組で定義する。`ModDataGenerators` で `includeClient()` に従ってEN_USとJA_JPを登録する。既存44キーの文言と引数を維持したまま、翻訳JSONの所有元を手書きリソースから `src/generated/resources` へ移した。

新しい翻訳は `translated(key, english, japanese)` を追加してrunDataで再生成する。手書き側と生成側に同じ翻訳JSONを残さない。設定や収納、ネットワーク、キー操作は変更しない。

## 拾得時のON/OFFと優先度

`BagPickupSettings` が `CUSTOM_DATA.packingcube_pickup` の `enabled` と `priority` を所有する。欠損時は既存動作と同じON・0。優先度は0〜9で、設定操作は範囲外を拒否、読取時は範囲内に補正する。フィルターとは別キーなのでフィルター初期化で設定を失わない。

`BagRouter` は有効なback装備と通常36枠内の有効バックパックから、ONかつ対象フィルターに合う装着ポーチを収集する。各親のBagInventoryは一つだけ作り、参照を候補と一緒に保持する。優先度降順の安定ソートにより、同値なら装備番号→通常インベントリ位置→装着枠順を保つ。候補へ順次収納し残数だけ次へ渡す。入力ItemStack自体は変更せず、保存競合を避けるため開いている親は候補から除外する。

既存の `ItemEntityPickupEvent.Pre` による、拾得遅延・所有者・拒否状態・非観戦・生存の検証を維持。クライアント側処理も明示的に除外する。実収納数だけ拾得元を減らし、残りはバニラに引き渡す。OFFや満杯で物を削除しない。手動のインベントリ移動は監視しない。

設定操作は既存の `FilterActionPayload` に操作ID5（有効化）・6（優先度）を追加して使用し、ID0〜4とcodec順は変更しない。サーバーが現行containerId・実メニュー・生存／非観戦・所有本体の一意性、フィールド名、スロット値、厳密な値形式を検証する。有効化と優先度はポーチのみ。サーバーで更新後、BagInventory経由で親へ保存し、既存の読取専用スロットで設定を同期する。バックパックの同期コピーには内容・本体ID・旧全体設定を載せない。

バックパック管理画面の全体ON/OFFは廃止。ポーチの「設定」にON/OFFと優先度の±ボタンを置く。見本スロットも移動し、表示座標とクリック範囲を合わせている。日英の説明文をlang datagenへ追加。画像・レシピ・登録ID・キー割当は変更しない。

## フィルター設定の再利用

従来BagScreen内にあったフィルター編集ウィジェットと描画・見本入力をFilterEditorPanelへ分離。BagSettingsLayoutで収納領域高・固有設定行数に応じて座標を計算し、FilterEditorStateで検索／一覧／ページを保持する。現在のポーチは従来と同じ高さ132・2設定行で配置する。

パネルは設定／見本の読取と編集要求のAccessにだけ接続し、具体的なBagMenu・アイテム登録・自動収納・Payloadを参照しない。BagScreen内のアダプターが既存操作ID0〜6の通信へ接続する。サーバー側の収納・フィルター・優先度処理や保存形式は変更しない。別用途の空条件案内やモード説明はAccessで差し替え可能。

## 右側設定領域のスクロール

BagSettingsLayoutの固有設定行と見本を含む全設定を右へ移動。左の実スロット座標は維持し、中央の区切り線を削除。SettingsScrollStateが範囲・バー・表示判定、SettingsScrollPanelがクリッピングと入力捕捉を担当する。FilterEditorPanelの背景／ラベルは論理ローカル座標、見本判定はスクロールを逆変換した座標、ツールチップ表示は生の画面座標を使用する。描画と入力をビューポート内へ制限し、右側の操作を実スロットへ流さない。保存形式・通信・収納処理は変更しない。詳細はdocs/GUI.md。

旧バックパック全体のenabled=falseはBagRouterで無視し、ポーチのフラグだけを候補収集に使う。バックパックへの旧操作ID5はサーバーで拒否する。左インベントリは画面中央の元の位置から動かさず、設定領域の背景とウィジェットは独立した右原点で表示する。詳細はdocs/GUI.md。

### PC-32: 設定パネルの独立ドラッグ
設定ヘッダーからのみ移動。固定インベントリと別原点で入力・描画・スクロールを追従させる。位置は画面インスタンス内だけ保持。モデル・テクスチャ・Curios装備位置・収納プロトコルには変更なし。

### PC-33: 共通フローティングGUI・リサイズ・小画面対応
`FloatingWindowState`へ移動と四辺／四隅リサイズ・画面内補正を集約、`FloatingWindow`へ枠と前面batch境界を集約。BagScreenは内容・スロット遮蔽・リフローだけを接続。横幅不足で開かないガードを廃止。旧SettingsPanelPositionは共通状態の互換アダプタ。バニラGUI・Mixin・グローバルフック・モデル素材は変更なし。

### PC-34: 説明ポップアップの前面レイヤー修正
FloatingWindowに遅延説明キューとrenderTooltipsを追加。BagScreenが標準のtooltip予約メソッドをローカルに接続し、直接表示の説明も最終共通経路へ統合。標準説明のZ=400へ追加Z=400を与え、設定レイヤーより前へ。バニラ/全画面フック・モデル素材・収納仕様は変更しない。

PC-34の同時追加依頼: マウス保持アイテムの本体と個数の前後不一致も修正。GuiRenderLayersで深度を共通化、FloatingWindow.prepareContainerCursorLayerで既存コンテナの後続描画poseのみ持ち上げる。標準の分割スタック・ドラッグ分配・snapback処理はそのままで独自再描画しない。

### PC-35: ユーザー制作ウィジェット20枚の導入
PNGを無加工で受領。WidgetSkin/NineSliceAxis/WidgetTexturesで画像と伸縮を共通化し、SkinnedButton/SkinnedEditBox・スクロールバー・見本枠へ適用。未受領の通常ボタン状態差分はnormalを共用し、素材を捏造しない。標準の入力・クリック音・tooltip/narrationとPC34の前後関係を維持。既合意の設定タイトル帯／文字とラジアルの輪を非表示化。背景・ポーチ／ラジアル素材は未受領のため未適用。モデル・装備位置・サーバー通信は変更なし。

### PC-36: 背景3枚の導入・白文字・画像の描画順
GuiSurfaceと共通描画ヘルパーへ3枚の背景を追加。全所有GUI文字を白色化し、検索の編集不可色/ヒントも白へ。PNG前にflushし、遅延fill/textが画像を隠す経路を防止。ラジアルは背景に収まる半径へ調整し、管理ボタンを背景下へ配置。PC-35画像/関連コードを含む累積ZIPで上書き漏れに対応。モデル・装備位置・サーバー側の仕様・バニラは変更なし。

## PC-37 ポーチ・ラジアル画像の接続
filter_bagのdatagenモデルをminecraft:item/leatherからpackingcube:item/filter_bagへ変更。RadialSlotSkinで通常・ホバー・押下を共通化し、WidgetTexturesで3枚を描画。空き枠は常に通常状態。受領4枚は無加工、radial_pouch_assets.jsonに原本ハッシュと寸法を保存。バックパックのモデル・テクスチャ・Curiosレンダラーは変更なし。

## PC-38 ホバー説明の整理とラジアル間隔
自動収納トグル・含む/含まないモード切り替えからホバー説明だけを削除。動作・ボタン文字・優先度等の他の説明は維持。ラジアル4番を右、7番を左へ標準半径72で8px（半径に応じて縮小）移動し、4–5/6–7の横方向の枠間隔を約1pxから9pxへ拡大。他の番号の配置、背景、36px枠、角度ベースの選択は維持。

## PC-39 ボタンのホバー・押下アニメーション
ユーザー画像は無加工のまま、SkinnedButton（通常・タブ・トグル・閉じる）とラジアル枠・管理ボタンへ共通のButtonAnimation/ButtonAnimationRendererを接続。ホバー/キーボードフォーカスは1 GUI px上へ45ms時定数で滑らかに移動。押下は30msで93.5%へ縮小・2px沈み、180msで復帰する短いパルス。文字・アイコンも同じ変換で動かす。通常サイズ以上へ拡大しないため横端のスクロールクリップを避ける。選択済みタブは表示だけ反応し、ページをリセットしない。

実時間ベースでFPSに依存せず、無効化で動きをリセット。入力判定の矩形は固定し、クリック/キーのアクションやネットワーク送信を遅延させない。即座に画面が閉じるボタン・ラジアル選択は次画面を待たせず遷移するので、押下演出が最後まで見えない場合がある。既存PNG状態差分の切替は維持し、画像間クロスフェードは追加しない。バニラGUI・背景透過・ウィンドウ移動/サイズ・マウス位置には変更なし。描画はflush/push/try-finally/flush/popでローカル変換を閉じ、他GUIのレイヤーへ影響させない。
