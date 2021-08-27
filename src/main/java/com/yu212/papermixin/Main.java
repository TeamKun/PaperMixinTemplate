package com.yu212.papermixin;

import net.minecraft.launchwrapper.Launch;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        loadServerJar(arguments);
        arguments.add("--tweakClass=org.spongepowered.asm.launch.MyBootstrap");
        System.out.printf("Initializing LaunchWrapper with arguments %s.%n", arguments);
        Launch.main(arguments.toArray(new String[0]));
    }

    private static void loadServerJar(List<String> arguments) throws IOException, ReflectiveOperationException {
        int index = arguments.indexOf("--serverJar");
        if (index == -1) {
            throw new IllegalArgumentException("--serverJar argument is not present");
        }
        arguments.remove(index);
        Path serverJarPath = Paths.get(arguments.remove(index));
        if (Files.notExists(serverJarPath)) {
            throw new IOException("Failed to find server jar");
        }
        URLClassLoader classLoader = (URLClassLoader)Main.class.getClassLoader();
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, serverJarPath.toUri().toURL());
        System.out.println("Loaded Server Jar " + serverJarPath.toAbsolutePath());
    }
}
