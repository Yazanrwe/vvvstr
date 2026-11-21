package com.vstrike.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    /** The 10-tick lockout vanilla applies after a swing that hit nothing. */
    @Accessor("attackCooldown")
    int vstrike$getAttackCooldown();

    @Invoker("doAttack")
    boolean vstrike$invokeDoAttack();
}
