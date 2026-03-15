# Java 9+ 対応調査レポート

対象ファイル: `bootstrap/src/main/java/net/kunmc/lab/papermixin/Main.java`

---

## 問題の背景

元のコードは Java 8 専用の実装であり、Java 9 以降では起動に失敗する。

---

## 原因の調査

### 1. 直接的な原因（Java 9+ でのキャスト失敗）

元のコード（`Main.java:87-90`）:

```java
URLClassLoader classLoader = (URLClassLoader) Main.class.getClassLoader();
Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
addURL.setAccessible(true);
addURL.invoke(classLoader, path.toUri().toURL());
```

Java 8 ではシステムクラスローダーが `URLClassLoader` の実装だったが、Java 9 以降は内部クラス `jdk.internal.loader.ClassLoaders$AppClassLoader` に変わり、`URLClassLoader` を継承しない。そのため **`ClassCastException`** が発生する。

### 2. 単純な修正案とその失敗

#### 試み①: カスタム `URLClassLoader` + `getSystemClassLoader()` を親に

```java
URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
// → Launch をリフレクション経由で呼び出す
```

**失敗の理由:**

`-javaagent:mixin-0.8.2.jar` によって `mixin.jar` が AppClassLoader（システムクラスローダー）に追加される。
`mixin.jar` 内の `org.spongepowered.asm.launch.MyBootstrap` は `ITweaker` を implements している。

LaunchWrapper が MyBootstrap を読み込む際のクラスローダー委譲フロー:

```
LaunchClassLoader.findClass("org.spongepowered.asm.launch.MyBootstrap")
  → exclusion により parent.loadClass() へ委譲
  → parent = カスタム UCL
  → カスタム UCL.loadClass() → 親の AppClassLoader へ委譲 (通常の親委譲モデル)
  → AppClassLoader: mixin.jar から MyBootstrap を発見 → defineClass 試行
  → defineClass 中に ITweaker が必要
  → AppClassLoader には launchWrapper.jar がない → ClassNotFoundException
  → NoClassDefFoundError: net/minecraft/launchwrapper/ITweaker
```

#### 試み②: `appendToClassPathForInstrumentation` で AppClassLoader に追加

```java
Method m = cl.getClass().getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
m.setAccessible(true);
m.invoke(cl, path.toAbsolutePath().toString());
```

**失敗の理由:**

launchWrapper.jar が AppClassLoader に追加されると `Launch.class.getClassLoader()` = AppClassLoader になる。
しかし LaunchWrapper 1.12 の内部コード (`Launch.java`) は:

```java
classLoader = new LaunchClassLoader(((URLClassLoader) Launch.class.getClassLoader()).getURLs());
```

AppClassLoader は `URLClassLoader` ではないため **`ClassCastException`** が発生する。
加えて `appendToClassPathForInstrumentation` は Java 17 では `--add-opens java.base/jdk.internal.loader=ALL-UNNAMED` なしではアクセスできない。

---

## 根本的な問題の構造

試み①②が失敗する本質的な原因は、**`-javaagent:mixin.jar` による mixin クラスの AppClassLoader への先行読み込み**にある。

| 要件 | 必要な条件 |
|------|-----------|
| LaunchWrapper 1.12 の内部 `(URLClassLoader)` キャスト成功 | `Launch` を `URLClassLoader` のサブクラスが読み込む必要がある |
| `MyBootstrap` の `ITweaker` 解決 | `MyBootstrap` と `ITweaker` が**同じ**クラスローダー空間で定義されている必要がある |
| `-javaagent` による Mixin 初期化 | `mixin.jar` が AppClassLoader に追加される → 上記2要件と競合 |

---

## 最終的な解決策

### 方針

**`-javaagent:mixin.jar` を廃止し、LaunchWrapper の tweaker 機構で Mixin を初期化する。**

[BukkitBootstrap](https://github.com/LXGaming/BukkitBootstrap) の実装を参考に、tweaker クラス (`MyBootstrap`) が `MixinBootstrap.start()` / `inject()` を直接呼び出す方式に切り替える。これにより mixin.jar が AppClassLoader に先行読み込みされる問題が消滅する。

Mixin は `implementation` 依存として shadow jar (server.jar) にバンドル済みであり、`-javaagent` なしでも `java.class.path` 経由でクラスパス上に存在する。

### Mixin の初期化方式の比較

| 方式 | 仕組み | 問題点 |
|------|--------|--------|
| **Java agent 方式** (`-javaagent:mixin.jar`) | JVM が起動前に `premain()` を実行、`Instrumentation` で ClassFileTransformer を登録 | `mixin.jar` が AppClassLoader に追加され、Java 9+ でのクラスローダー分離問題を引き起こす |
| **tweaker 方式** (今回採用) | LaunchWrapper が tweaker を読み込み、`MixinBootstrap.start()` → `inject()` を呼び出す。Mixin が `LaunchClassLoader.registerTransformer()` で自身のトランスフォーマーを登録 | なし |

### child-first URLClassLoader の構成

```
bootstrap classloader
  └── Platform ClassLoader (Java 9+)  /  null (Java 8)
        └── カスタム URLClassLoader [child-first]  ← ★ここで全クラスを管理
              URL: [launchWrapper.jar, serverJar, java.class.path (server.jar with bundled mixin)]
              ├── Launch (LaunchWrapper 本体)
              ├── LaunchClassLoader
              ├── ITweaker
              ├── MyBootstrap    ← tweaker として動作し Mixin を初期化
              └── Minecraft server classes
```

AppClassLoader はこのチェーンに含まれない。

### 動作フロー

```
1. launchWrapper.jar (なければダウンロード) と patched server jar のパスを解決
2. java.class.path (= server.jar with bundled mixin) を読み取り URL リストを構築
3. 親 = getPlatformClassLoader() のカスタム child-first UCL を生成
4. Launch をカスタム UCL で読み込む
   → Launch.class.getClassLoader() = カスタム UCL (URLClassLoader) ✓
   → LaunchWrapper 1.12 内部の URLClassLoader キャスト成功 ✓
5. LaunchClassLoader が MyBootstrap を exclusion 経由で parent (カスタム UCL) に委譲
   → child-first: server.jar 内の MyBootstrap をカスタム UCL で defineClass
   → ITweaker が必要 → カスタム UCL が launchWrapper.jar から読み込み
   → MyBootstrap と ITweaker が同じ UCL 空間 → ClassCastException なし ✓
6. LaunchWrapper が MyBootstrap の各メソッドを呼び出す
   → MixinBootstrap.start() / doInit() / inject() により Mixin が LaunchClassLoader に登録
7. LaunchClassLoader が Minecraft server クラスを変換しながら読み込む
```

---

## 変更ファイル一覧

### `bootstrap/src/main/java/net/kunmc/lab/papermixin/Main.java`

| # | 変更内容 | 理由 |
|---|---------|------|
| 1 | `(URLClassLoader) Main.class.getClassLoader()` キャストを削除 | Java 9+ で ClassCastException |
| 2 | `addURL()` ヘルパーメソッドを削除 | Java 8 専用のため不要 |
| 3 | `java.class.path` から全 JAR を収集し child-first UCL に集約 | mixin 等を同一 CL 空間で管理 |
| 4 | 親を `getPlatformClassLoader()` に変更 | AppClassLoader を親チェーンから排除 |
| 5 | `loadClass` をオーバーライドして child-first 動作を実現 | 自前 URL を AppClassLoader より優先して検索 |
| 6 | `Launch.main()` → リフレクション経由に変更 | Launch がカスタム UCL に属するため |
| 7 | `Files.walk` を `try-with-resources` で囲む | `Stream<Path>` のリソースリーク修正 |
| 8 | `.get()` → `.orElseThrow()` に変更 | `Optional.get()` の警告解消 |

### `bootstrap/build.gradle`

| # | 変更内容 | 理由 |
|---|---------|------|
| 1 | `copyMixinLibToServer` タスクを削除 | `-javaagent` が不要になったため mixin.jar のコピーも不要 |
| 2 | `copyMixinLibToLibsDir` タスクを削除 | 同上 |
| 3 | `buildServer` タスクの依存から `copyMixinLibToServer` を除外 | 同上 |

---

## 起動コマンド

### 変更前

```
java -Xmx4G -javaagent:..\libs\mixin-0.8.2.jar -jar server.jar nogui --serverJar ../libs/patched_1.16.5.jar
```

### 変更後

```
java -Xmx4G -jar server.jar nogui --serverJar ../libs/patched_1.16.5.jar
```

`-javaagent` の指定が不要になった。追加の JVM フラグ (`--add-opens` 等) も不要。