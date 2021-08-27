package com.yu212.papermixin.mixin;

import net.minecraft.server.v1_16_R3.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DedicatedServer.class, remap = false)
public class MixinDedicatedServer {
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue()) {
            LogManager.getLogger().info(Bukkit.getServer().getName() + " successfully bootstrapped.");
        }
    }
}
