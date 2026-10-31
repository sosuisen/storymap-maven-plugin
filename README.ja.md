# storymap-maven-plugin

[English](README.md) | 日本語

Markdownのアウトラインを、ユーザーストーリーマップとして描画するMaven pluginです。
HTML、SVG、PNGで出力できます。

描画にはRustの[storymap](https://crates.io/crates/storymap)クレートを使います。
JavaからはFFMで呼び出します。
アウトラインの書き方は、storymapのREADMEを見てください。

## 必要なもの

- Java 25以降で動くMaven 3.9以降
- 対応するOSとアーキテクチャ
  - Windows（x86_64）
  - macOS（x86_64、aarch64）
  - Linux（x86_64、aarch64。glibc 2.28以降）

## 使い方

`pom.xml`にpluginを足します。

```xml
<plugin>
  <groupId>com.sosuisha</groupId>
  <artifactId>storymap-maven-plugin</artifactId>
  <version>1.0.0</version>
  <configuration>
    <input>${project.basedir}/docs/storymap.md</input>
    <formats>
      <format>html</format>
      <format>png</format>
    </formats>
    <scale>2.0</scale>
  </configuration>
</plugin>
```

実行します。

```sh
mvn storymap:render
```

`target/storymap/storymap.html`と`target/storymap/storymap.png`ができます。

コマンドラインでも指定できます。

```sh
mvn storymap:render -Dstorymap.input=docs/map.md -Dstorymap.formats=html,svg,png
```

### パラメーター

| パラメーター | プロパティ | 既定値 | 意味 |
|---|---|---|---|
| `input` | `storymap.input` | `${project.basedir}/docs/storymap.md` | 読み込むアウトライン。UTF-8で読む |
| `outputDirectory` | `storymap.outputDirectory` | `${project.build.directory}/storymap` | 出力先のフォルダー |
| `formats` | `storymap.formats` | `html` | 出力形式。`html`、`svg`、`png`から選ぶ |
| `scale` | `storymap.scale` | `1.0` | PNGの倍率。1.0で、CSSの1pxが1画素になる |
| `maxCardWidth` | `storymap.maxCardWidth` | `160` | カードの幅の上限（CSSのpx）。長い文は折り返す |

出力ファイルの名前は、入力ファイルの名前から拡張子を除いたものです。

### 警告を消す

このpluginは、FFMでネイティブライブラリを呼びます。
最近のMavenは、これを初めから許可しています。
古いMavenでは、次の警告が出ます。

```
WARNING: A restricted method in java.lang.foreign.SymbolLookup has been called
```

消すには、プロジェクトの`.mvn/jvm.config`に次の1行を書きます。

```
--enable-native-access=ALL-UNNAMED
```

### 画像のフォント

SVGとPNGは、OSのフォントで文字の幅を測ります。
そのため、OSによって画像の見た目が少し変わります。

## 開発

ビルドには、JDK 25とRustのツールチェーンが要ります。

```sh
mvn formatter:validate
mvn clean test
cd native && cargo fmt --check && cargo clippy --all-targets
```

`mvn`のビルドは、最初に`cargo build --release`を実行します。
手元のOSのライブラリだけがJARに入ります。

## リリース

- バージョンはGitのタグだけで決めます。`pom.xml`の`revision`は、手元のビルド用の仮の値です。書き換えません。
- タグを打ってpushすると、リリースのワークフローが動きます。

  ```sh
  git tag v1.0.0
  git push origin v1.0.0
  ```

- ワークフローは、次のことをします。
  1. Windows、macOS、Linuxの各ランナーで、5つのプラットフォームのネイティブライブラリをビルドする。
  2. それらを`native-libs/`に集め、テストをやり直してから、1つのJARを作る。
  3. JARに5つのライブラリが入っているかを確かめる。
  4. JARに署名し、Central Portalにアップロードする。
  5. GitHub Releaseを作り、JARとpomを添付する。
- タグは`v1.2.0`の形でなければなりません。それ以外の形では、ワークフローが止まります。
- アップロードしただけでは、まだ公開されません。[Central Portal](https://central.sonatype.com/publishing/deployments)でdeploymentを開き、中身を確かめてから「Publish」を押します。
  - Maven Centralに出たバージョンは、変えることも消すこともできません。そのため、この手順だけは手で行います。

ワークフローには、リポジトリに次のSecretsが要ります。

| Secret | 中身 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portalのユーザートークンの名前 |
| `MAVEN_CENTRAL_PASSWORD` | 同じトークンのパスワード |
| `GPG_PRIVATE_KEY` | `gpg --armor --export-secret-keys <鍵ID>`の出力 |
| `GPG_PASSPHRASE` | 鍵のパスフレーズ |

## ライセンス

MIT
