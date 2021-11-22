# PaperMixinTemplate

**PaperMixinTemplate**はPaperサーバーでMixinを使用可能にするテンプレートプロジェクトです.

### Environmental Dependence

* Java 8

## Installation

1. ```Use this template``` and ```git clone```
2. setupタスクの```generatePatchedJar```タスクを実行  
   \*上手く行かない場合  
   Paperサーバーのcacheディレクトリにある```patched_1.16.5.jar```ファイルをルートプロジェクトの```libs```ディレクトリにコピー

## Usage

Intellij IDEAのRunConfigurationにある```RunServer```を実行すればMixinが適用されたサーバーを起動することが出来ます.  
(実行時にはプロジェクトのビルド及び必要なファイルをコピーするタスクが走ります.)

他環境でサーバーを実行したい場合は```buildServer```というGradleタスクを実行することでビルドタスク及びserverディレクトリへのコピータスクが実行されます.   
(実行環境は必ずJava 8とすること.)

## FAQ

* Mixinクラスを追加したい  
  bootstrapプロジェクトの```net.kunmc.lab.papermixin.mixin```に新たなクラスを作成してください.  
  また,resourcesの```papermixin.mixins.json```の```mixins```フィールドに作成したクラス名を追加してください.


* プラグインの名前を変更したい  
  ルートプロジェクトの```settings.gradle```の```rootProject.name```を変更してください.


* バージョンを変更したい  
  ルートプロジェクトの```build.gradle```の```version```を変更してください.
  
* Pterodactylで実行したい
   * adminページのStartupにあるStartup Commandを`java  -javaagent:"./mixin-0.8.2.jar" -Xms128M -Xmx{{SERVER_MEMORY}}M -Dterminal.jline=false -Dterminal.ansi=true -jar {{SERVER_JARFILE}}`とする
   * 上のページの左下部にあるDockerImageConfigurationからImageをJava8(`quay.io/pterodactyl/core:java`)にする
   * `buildServerタスク`実行後のserverディレクトリの中身をPterodactylにコピーする
   
  
## References

https://github.com/LXGaming/BukkitBootstrap \
SiguServerBootstrap (private)
