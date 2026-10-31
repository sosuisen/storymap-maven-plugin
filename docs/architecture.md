# architecture

## 全体像

Markdownのアウトラインを、ユーザーストーリーマップに描画するMaven pluginである。
描画は、Rustの[storymap](https://crates.io/crates/storymap)クレートが行う。
Javaからは、FFM（`java.lang.foreign`）でネイティブライブラリを呼ぶ。

ゴールは`storymap:render`の1つだけである。

## 構成

```
native/          Rustのcdylib（storymap_ffi）。storymapクレートをC ABIで包む
src/main/java/   Maven pluginのJava側（パッケージ com.sosuisha.storymap）
```

### Rust側（`native/src/lib.rs`）

C ABIの関数を4つ公開する。

| 関数 | 役割 |
|---|---|
| `storymap_render_html(input, len, out_len)` | 単独で開けるHTMLページを返す |
| `storymap_render_svg(input, len, out_len)` | SVG文書を返す |
| `storymap_render_png(input, len, scale, out_len)` | PNG画像を返す |
| `storymap_free(ptr, len)` | 返したバッファーを解放する |

- 入力はUTF-8のバイト列である。
- 失敗するとnullを返す。panicは`catch_unwind`で止め、JVMへ伝えない。
- メモリー確保の失敗は`abort`になり、止められない。そのため、PNGは描画の前に画素数を確かめ、大きすぎれば断る。
- OSのフォントの読み込みは遅いので、プロセスごとに1回だけ行う。

### Java側（`com.sosuisha.storymap`）

| クラス | 役割 |
|---|---|
| `RenderMojo` | `render`ゴール。パラメーターを読み、形式ごとにファイルを書く |
| `StoryMapRenderer` | FFMでネイティブ関数を呼び、描画結果のバイト列を返す |
| `NativeLibrary` | このOSとアーキテクチャのライブラリを、クラスパスから取り出して読み込む |
| `Format` | 出力形式（HTML・SVG・PNG）と拡張子 |
| `RenderException` | ネイティブ側の描画の失敗 |

依存の向きは、`RenderMojo` → `StoryMapRenderer` → `NativeLibrary`の一方向である。
`Format`と`RenderException`は、どこからも使われる値の型である。

## 外部とのやり取り

- ネイティブライブラリは、JARの`native/<os>-<arch>/`に入っている。`<os>-<arch>`は`windows-x86_64`、`linux-x86_64`、`linux-aarch64`、`macos-x86_64`、`macos-aarch64`のいずれかである。
- 読み込む前に、`<java.io.tmpdir>/storymap-maven-plugin/<内容のSHA-256の先頭>/`へ書き出す。
  - Windowsでは、読み込み中のDLLを消せない。実行ごとに一時ファイルを作ると溜まり続けるので、同じ内容なら前のコピーを使う。
- 出力ファイルは、`outputDirectory`に`<入力ファイルの拡張子を除いた名前>.<形式>`で書く。

## 主な処理の流れ

1. Mavenが`RenderMojo`のフィールドにパラメーターを入れ、`execute`を呼ぶ。
2. `RenderMojo`が形式の名前を`Format`に変え、入力をUTF-8で読む。
3. 形式ごとに`StoryMapRenderer.render`を呼ぶ。
4. 初回の呼び出しで、`StoryMapRenderer.Natives`が`NativeLibrary.load`でライブラリを読み込み、関数のハンドルを作る。
5. 入力をネイティブメモリーに写して関数を呼び、結果をJavaの配列に写してから`storymap_free`で解放する。

## ビルド

- `mvn`のビルドは、`generate-resources`で`cargo build --release`を実行する。手元のOSのライブラリだけが、JARに入る。
- リリースのワークフローは、各OSでライブラリをビルドし、`native-libs/`に集める。そのうえで`-Dcargo.skip=true`を付けてJARを作る。
- 続けて`release` profileで、sources・javadocのJARを作り、署名してCentral Portalへアップロードする。公開は、Central Portalで人が行う。
- Centralに置くpomは、flatten-maven-pluginが`${revision}`をバージョンに置き換えたものである。
- ライブラリの置き場所とファイル名は、pom.xmlのOSごとのprofileが決める。
