package com.vstrike.mixin.client;

import com.vstrike.config.VStrikeConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntity.swingHand() starts a swing with handSwingTicks = -1. tickHandSwing() then runs
 * once per tick: the first tick moves it to 0 (progress 0/duration, so nothing visibly moves),
 * and the arm only starts to move on the second tick. The first-person swing therefore begins
 * 50-100 ms after the click.
 *
 * For the local player only, start at 0 instead. The arm begins moving on the first tick after
 * the click and the swing finishes one tick earlier. Swing duration, damage, attack packets and
 * attack-strength cooldown are not touched; handSwingTicks is read only for animation and for
 * the "may this swing restart" check in swingHand() itself.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public boolean handSwinging;

    @Shadow
    public int handSwingTicks;

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("TAIL"))
    private void vstrike$startSwingOneTickSooner(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if (VStrikeConfig.swingStartCompensation
                && (Object) this instanceof ClientPlayerEntity
                && this.handSwinging
                && this.handSwingTicks == -1) {
            this.handSwingTicks = 0;
        }
    }
}
