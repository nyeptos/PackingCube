# Curiosのバックパック背負い表示

## 採用したユーザー制作リソース

| リソース | パス |
|---|---|
| 装備用JSONモデル | `src/main/resources/assets/packingcube/models/item/backpack_equipped.json` |
| 装備用32×32テクスチャ | `src/main/resources/assets/packingcube/textures/item/backpack.png` |

受領したbackpack.jsonから変更したのはtextures.2／textures.particleの参照先のみ。`block/backpack`を`packingcube:item/backpack`へ変更する。7個の要素、寸法、UV、回転、グループ、表示変換などは保持。PNGは元データとバイト一致。モデルは装備用として独立させ、既存のアイテムID・インベントリ／手持ちアイコン・モデルdatagenは変更しない。ポーチは対象外。

## クライアント実装

- `ClientEvents`がCurios導入時のみ追加モデル登録とクライアントセットアップ処理を呼ぶ。
- `client/compat/CuriosClientCompat`がstandaloneモデル`packingcube:item/backpack_equipped`をModelEvent.RegisterAdditionalへ登録し、バックパックのICurioRendererをCuriosRendererRegistryへ登録する。
- `CuriosBackpackRenderer`は現在のHumanoidModel.bodyの移動・回転へ追従する。しゃがみ用の別回転を重ねて二重適用しない。
- バックパックの機能側backスロット、表示ON、有効なスロット、非観戦／非透明のPlayer、HumanoidModelの場合に描画する。Curiosの表示トグルOFF時は描画しない。Cosmeticスロットは対象外。
- 現在のフレームでベイク済みモデルをModelManagerから取得し、ItemRendererにItemDisplayContext.NONEで渡す。書き出し済みthirdperson_hand変換は背中表示へ適用しない。F3+T等の再読込後も古いBakedModelを保持しない。
- Curios依存型はclient/compat内へ閉じ込める。Curios非導入ではそのクラスを呼び出さず、専用サーバーからクライアントクラスを参照しない。
- 背負いモデルが描画されるのは第三者視点／他プレイヤーからの外見。自分の通常の一人称カメラへバッグを映す処理は追加しない。

## 初期配置と調整箇所

`CuriosBackpackRenderer`の`OFFSET_Y`、`OFFSET_Z`、`SCALE`が調整箇所。初期値はY=5/16、Z=6/16、等倍。胴体座標へ追従してから位置補正し、(-1,-1,1)でモデル座標の上下／左右をエンティティ描画へ合わせる。ItemRendererがモデル中心(8,8,8)を引くことを考慮している。

モデル元座標の外形はX=3..13、Y=0..11、Z=5..12.1。初期配置後は胴体原点に対しY=2..13px、背面の最内面Z=3px。通常の胴体半厚2pxから1px離す想定。これは座標からの初期推定であり、実クライアントで見た目を確認した値ではない。

## ゲーム内の確認手順

1. Curios入りでrunClientを再起動する。
2. バックパックをCuriosのbackへ装備する。
3. F5で背面と前面の第三者視点を確認する。Curiosの表示トグルON/OFFで外見だけ切り替わるか確認する。
4. 歩行・しゃがみ・泳ぎ・飛行等で胴体へ追従するか確認する。
5. Steve／Alexの両方、防具やエリトラとの重なりを確認する。位置や縮尺の調整が必要ならスクリーンショットで合わせる。
6. 外すと消えるか、F3+Tの再読込で紫黒にならないか確認する。
7. Curiosなし起動／専用サーバー、実マルチでも別途確認する。

実装時に実GUI・OpenGL描画・実マルチ接続は確認していない。GameTestはリソース同梱・サーバー回帰の検証であり、ベイク結果・UVの見え方・初期配置の最終確認を代替しない。防具やエリトラを自動で隠す／押し出す機能も追加していない。
