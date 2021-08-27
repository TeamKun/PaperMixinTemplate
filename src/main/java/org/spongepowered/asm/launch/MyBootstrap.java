package org.spongepowered.asm.launch;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.platform.CommandLineOptions;

import java.io.File;
import java.util.List;

public class MyBootstrap implements ITweaker {
    private String[] launchArguments = new String[0];

    public MyBootstrap() {
        MixinBootstrap.start();
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (args != null && !args.isEmpty()) {
            this.launchArguments = args.toArray(new String[0]);
        }
        MixinBootstrap.doInit(CommandLineOptions.ofArgs(args));
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.addClassLoaderExclusion("com.mojang.util.QueueLogAppender");
        classLoader.addClassLoaderExclusion("jline.");
        classLoader.addClassLoaderExclusion("org.fusesource.");
        classLoader.addClassLoaderExclusion("org.slf4j.");
        MixinBootstrap.inject();
    }

    @Override
    public String getLaunchTarget() {
        return "org.bukkit.craftbukkit.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArguments;
    }
}
