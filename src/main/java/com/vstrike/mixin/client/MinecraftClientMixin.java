package com.vstrike.mixin.client;

import com.vstrike.util.LatencyProbe;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void vstrike$probeAttack(CallbackInfoReturnable<Boolean> cir) {
        LatencyProbe.onAttackDispatched();
    }
}
