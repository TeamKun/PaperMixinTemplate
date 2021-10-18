package net.kunmc.lab.papermixin.mixin;

import net.minecraft.server.v1_16_R3.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, remap = false)
public class MixinCraftPlayer {
    @Inject(method = "setSneaking", at = @At("RETURN"))
    public void setSneaking(boolean sneak, CallbackInfo info) {
        if (sneak) {
            System.out.println("mixin: sneak!");
        }
    }
}