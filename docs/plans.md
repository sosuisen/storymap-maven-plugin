2026-10-31 ギア5速：テストもコードもAIが書く。人間は最初の設計に関わるだけで、コードレビューはしない

# plans

## ユーザータスク
- [x] Markdownのアウトラインを、HTML・SVG・PNGのストーリーマップに描画する`render`ゴール
- [x] Windows・macOS・LinuxのネイティブライブラリをCIでビルドし、1つのJARにまとめてリリースする
- [x] リリースのワークフローから、Maven Centralへdeployする
- [x] カードの幅の上限を、パラメーターで変えられる
  - [x] HTMLのカードの幅の上限を変えられる
  - [x] SVG・PNGのカードの幅の上限を変えられる
  - [x] 上限を指定しなければ、160pxになる
  - [x] 上限は、正の有限の数でなければならない

## 後の課題
- [ ] カードの幅の上限の入った版（1.0.2）を、Maven Centralに公開する
  - 2026年10月は、Maven Centralの公開上限に達したため、公開を見送った。今はローカルに`mvn install`して使っている
  - [x] 公開の前に、storymapクレート0.3.0をcrates.ioに公開し、`native/Cargo.toml`の依存をGitのコミットからcrates.ioの版に戻す
