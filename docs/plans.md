2026-10-31 ギア5速：テストもコードもAIが書く。人間は最初の設計に関わるだけで、コードレビューはしない

# plans

## ユーザータスク

## 後の課題
- [ ] `release.yml`の`setup-java`の入力を、非推奨でない名前に変える
  - 2026-11-01のv1.0.2のリリースで、`setup-java`が3つの警告を出した。リリース自体は成功した
  - 警告の内容
    - `The 'server-username' input is deprecated and may be removed in a future release. Please use 'server-username-env-var' instead.`
    - `The 'server-password' input is deprecated and may be removed in a future release. Please use 'server-password-env-var' instead.`
    - `The 'gpg-passphrase' input is deprecated and may be removed in a future release. Please use 'gpg-passphrase-env-var' instead.`
  - 次のリリースまでに、`server-username`・`server-password`・`gpg-passphrase`を`-env-var`付きの名前に変える
