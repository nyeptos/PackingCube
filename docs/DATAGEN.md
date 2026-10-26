# モデル・レシピ・翻訳のdatagen

## IDEAでの実行

1. 更新ファイルを適用し、Gradleを再読み込みする。
2. 実行構成の `runData`（表示名はIDEAにより `data`）を実行する。
3. 見当たらない場合は、IDEAのGradleツールウィンドウから `runData` タスクを実行する。ターミナルは不要。
4. 生成先は `src/generated/resources`。生成ファイルを確認してソースと一緒にGit管理する。
5. `runClient` で表示を確認する。ビルド時は生成済みリソースをそのまま使うので、通常のビルドのたびにdatagenを起動する必要はない。

`.cache` は生成器用キャッシュなのでGit管理しない。生成JSONは直接編集せず、プロバイダーのJavaを変更して再生成する。

## アイテムモデル

編集先：`data/ModItemModelProvider.java` の `registerModels()`。

- バックパックは`packingcube:item/backpack_equipped`を親にする薄いアイテムモデルを生成し、ユーザー制作の形状・UV・画像・Blockbenchのdisplayを継承する。生成ラッパーにdisplay／elements／texturesを重複させない。ポーチは引き続きバニラleatherの仮テクスチャ。
- 背負い用32×32画像をアイテムにも共用する。モデル本体は`src/main/resources/assets/packingcube/models/item/backpack_equipped.json`、画像は`textures/item/backpack.png`。Blockbenchで表示設定を調整する場合はモデル本体のdisplayを更新し、生成JSONを直接編集しない。
- 以前の `src/main/resources/assets/packingcube/models/item/{large_bag,filter_bag}.json` は削除し、同じリソースIDの生成版へ移した。二重管理しない。

自作テクスチャ `src/main/resources/assets/packingcube/textures/item/example.png` を使う新アイテムなら、レジストリ登録後に以下の形で追加する。

```java
basicItem(ModItems.EXAMPLE.get());
```

`EXAMPLE` は説明用の名前で、現状登録されていない。実際のフィールド名へ置き換える。画像ファイルを先に置くこと。手持ち武器型なら `handheldItem(...)`、単純なブロックアイテムなら `simpleBlockItem(...)` を使える。

## レシピ

編集先：`data/ModRecipeProvider.java` の `buildRecipes(RecipeOutput output)`。

**材料・コストは未指定なので、製品版には新しいレシピを追加していない。** レシピ用プロバイダーの登録・生成経路は用意してある。

以下は書き方の例であり、確定したゲーム仕様ではない。`buildRecipes` 内へ入れて材料・配置を調整する。

```java
ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FILTER_BAG.get())
        .pattern(" S ")
        .pattern("L L")
        .pattern("LLL")
        .define('S', Items.STRING)
        .define('L', Items.LEATHER)
        .unlockedBy("has_leather", has(Items.LEATHER))
        .save(output);
```

必要なimport：

```java
import github.nyeptos.packingcube.registry.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
```

不定形なら `ShapelessRecipeBuilder` を使う。別名のレシピが必要な場合は `save(output, ResourceLocation.fromNamespaceAndPath("packingcube", "recipe_name"))` のように明示する。

Minecraft 1.21.1では次へ出力される。

- レシピ：`src/generated/resources/data/packingcube/recipe/<id>.json`
- レシピ解放進捗：`src/generated/resources/data/packingcube/advancement/recipes/<category>/<id>.json`

一時的なレシピを検証した後はJavaから定義を削除して `runData` を再実行する。生成器のキャッシュが残っていれば古い生成物も掃除される。キャッシュを消した場合は不要な生成JSONが残っていないか確認する。

## 手書きリソースとの分担

| 場所 | 担当 |
|---|---|
| `src/main/resources` |作者のテクスチャ、Curiosのタグ・設定、GameTest構造 |
| `src/generated/resources` | モデル・レシピ・翻訳プロバイダーの生成物 |

同じ相対パスのJSONを両方へ置かない。生成済みモデルも配布物に含めるため、初回起動のためだけに `runData` を実行する必要はない。


## 日本語・英語の翻訳

編集先：`data/ModLanguageProvider.java` の `addTranslations()`。

```java
translated("gui.packingcube.example", "Example", "表示例");
```

引数は翻訳キー・英語・日本語の順。例のキーは説明用で、実際に使用するキーへ置き換える。
両言語を一組で登録するので、キーを片方だけ追加する必要はない。`%s` などの引数プレースホルダーは両言語で数・型を合わせる。

IDEAの `runData` を実行すると次を生成する。

- `src/generated/resources/assets/packingcube/lang/en_us.json`
- `src/generated/resources/assets/packingcube/lang/ja_jp.json`

生成JSONを直接編集せず、Javaを変更して再生成する。生成済みJSONもGit管理する。
以前の `src/main/resources/assets/packingcube/lang/en_us.json` と `ja_jp.json` は削除する。同じ相対パスの翻訳を手書き側へ残さないこと。
既存の翻訳キーと文言は両言語とも維持している。Minecraft標準キーやアイテムID・タグIDは翻訳定義に複製しない。
画面での確認はMinecraftの言語を日本語／English (US)に切り替えて行う。
