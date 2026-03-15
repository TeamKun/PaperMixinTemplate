package net.kunmc.lab.papermixin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Main {
    private static final String launchWrapperUrl = "https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar";
    private static final Path launchWrapperJarPath = new File("launchWrapper.jar").toPath();

    public static void main(String[] args) throws Exception {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.add("--tweakClass=org.spongepowered.asm.launch.MyBootstrap");

        Path serverJarPath = resolveServerJar(arguments);
        Path launchWrapperPath = resolveLaunchWrapper();

        // URL リスト:
        //   1. launchWrapper.jar  ... LaunchWrapper 本体 (URLClassLoader キャスト対策で先頭に)
        //   2. serverJar          ... パッチ済みサーバー JAR
        //   3. java.class.path    ... bootstrap jar (mixin 等がバンドル済み)
        //
        // Mixin は -javaagent ではなく tweaker (MyBootstrap) 経由で初期化するため、
        // mixin クラスは server.jar (bootstrap) にバンドルされており java.class.path 経由で取得できる。
        // これらを全て同一の child-first UCL に集約することで、
        // MyBootstrap (ITweaker を実装) と ITweaker が同じ ClassLoader 空間で解決され
        // ClassCastException を防ぐ。
        List<URL> urls = new ArrayList<>();
        urls.add(launchWrapperPath.toUri().toURL());
        urls.add(serverJarPath.toUri().toURL());
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }

        // Java 9+ は getPlatformClassLoader() を親にする (Java 8 は null = bootstrap classloader)。
        //
        // AppClassLoader を親チェーンから外す理由:
        //   -jar server.jar で起動した時点で AppClassLoader のクラスパスには server.jar が含まれる。
        //   server.jar には mixin 等がバンドルされているため、AppClassLoader を親にすると
        //   「AppClassLoader 経由のコードパス」で同じクラスが AppClassLoader の名前空間にも
        //   定義されてしまい、カスタム UCL 側との二重定義による ClassCastException が起きうる。
        //   PlatformClassLoader は server.jar を持たないため、アプリケーションレベルの
        //   クラスが全てカスタム UCL の名前空間に一本化される。
        ClassLoader parent;
        try {
            parent = (ClassLoader) ClassLoader.class
                    .getMethod("getPlatformClassLoader").invoke(null);
        } catch (NoSuchMethodException e) {
            parent = null; // Java 8: bootstrap classloader
        }

        // child-first: 親への委譲より先に自分の URL から探す
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c != null) {
                        if (resolve) resolveClass(c);
                        return c;
                    }
                    try {
                        c = findClass(name); // 自分の URL を優先
                    } catch (ClassNotFoundException ignored) {
                        c = super.loadClass(name, false); // java.* 等は親 (platform/bootstrap) へ
                    }
                    if (resolve) resolveClass(c);
                    return c;
                }
            }
        };

        Thread.currentThread().setContextClassLoader(classLoader);

        System.out.printf("Initializing LaunchWrapper with arguments %s.%n", arguments);
        Class<?> launchClass = classLoader.loadClass("net.minecraft.launchwrapper.Launch");
        Method mainMethod = launchClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) arguments.toArray(new String[0]));
    }

    private static Path resolveServerJar(List<String> arguments) throws IOException {
        int index = arguments.indexOf("--serverJar");

        Path serverJarPath;
        if (index != -1) {
            arguments.remove(index);
            serverJarPath = Paths.get(arguments.remove(index));
        } else {
            try (Stream<Path> stream = Files.walk(Paths.get("./"))) {
                serverJarPath = stream
                        .filter(p -> p.toFile().getName().matches("patched.*.jar"))
                        .findFirst()
                        .orElseThrow(() -> new IOException("Failed to find patched server jar"));
            }
        }

        if (Files.notExists(serverJarPath)) {
            throw new IOException("Failed to find server jar");
        }

        System.out.println("Loaded Server Jar " + serverJarPath.toAbsolutePath());
        return serverJarPath;
    }

    private static Path resolveLaunchWrapper() throws IOException {
        if (!launchWrapperJarPath.toFile().exists()) {
            System.out.println("Downloading LaunchWrapper...");
            try (InputStream stream = new URL(launchWrapperUrl).openStream()) {
                Files.copy(stream, launchWrapperJarPath);
            }
        }
        System.out.println("Loaded LaunchWrapper Jar " + launchWrapperJarPath.toAbsolutePath());
        return launchWrapperJarPath;
    }
}
