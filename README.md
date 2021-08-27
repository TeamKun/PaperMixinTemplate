# Paper-Mixin-Example
 
## 使用方法
このリポジトリのトップページにある `Use this template` を押すとなんかうまいことなりそうです\
プロジェクトディレクトリ直下に `server`, `libs` ディレクトリをつくる\
`patched_1.16.5.jar` を用意して `libs` 直下に置く\
`settings.gradle`, `plugin.yml`, `PaperMixinExamplePlugin.java`, `MixinCraftPlayer.java` と
`papermixin` で検索して出てくるすべての場所は適宜書き換えてください\
ビルドしたサーバーのjarファイルとpluginのjarファイルは、それぞれ\
`bootstrap/build/libs/` と `plugin/build/libs/` にあります\
通常のプラグインとMixinを同梱していますが、Mixinしかいらない場合はpluginのほうを無視してもjarファイル内に余計なものは入っていないはずです

実行構成は画像の通りにすると、MixinにHotSwapが使えて幸せになれます
![image](https://user-images.githubusercontent.com/41502287/131144343-1d0feefe-9f5d-4e06-8b83-e17fb02c02ab.png)
VMオプション: `-Xmx2G -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -javaagent:"{mixinライブラリのキャッシュディレクトリ}"`

## 参考
https://github.com/LXGaming/BukkitBootstrap \
SiguServerBootstrap (private)
