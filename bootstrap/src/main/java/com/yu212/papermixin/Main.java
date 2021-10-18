package com.yu212.papermixin;

import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final String launchWrapperUrl = "https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar";
    private static final Path launchWrapperJarPath = new File("launchWrapper.jar").toPath();

    public static void main(String[] args) throws IOException {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.add("--tweakClass=org.spongepowered.asm.launch.MyBootstrap");

        loadServerJar(arguments);
        loadLaunchWrapper();

        System.out.printf("Initializing LaunchWrapper with arguments %s.%n", arguments);
        Launch.main(arguments.toArray(new String[0]));
    }

    private static void loadServerJar(List<String> arguments) throws IOException {
        int index = arguments.indexOf("--serverJar");
        if (index == -1) {
            throw new IllegalArgumentException("--serverJar argument is not present");
        }
        arguments.remove(index);
        Path serverJarPath = Paths.get(arguments.remove(index));
        if (Files.notExists(serverJarPath)) {
            throw new IOException("Failed to find server jar");
        }

        try {
            addURL(serverJarPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Loaded Server Jar " + serverJarPath.toAbsolutePath());
    }

    private static void loadLaunchWrapper() {
        try {
            if (!launchWrapperJarPath.toFile().exists()) {
                System.out.println("Downloading LaunchWrapper...");
                downloadLaunchWrapper();
            }
            addURL(launchWrapperJarPath);
            System.out.println("Loaded LaunchWrapper Jar " + launchWrapperJarPath.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void downloadLaunchWrapper() throws IOException {
        URL url = new URL(launchWrapperUrl);
        try (InputStream stream = url.openStream()) {
            Files.copy(stream, launchWrapperJarPath);
        }
    }

    private static void addURL(Path path) throws MalformedURLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        URLClassLoader classLoader = (URLClassLoader) Main.class.getClassLoader();
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, path.toUri().toURL());
    }
}
