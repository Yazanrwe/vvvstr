package com.vstrike.mixin.client;

import com.vstrike.config.VStrikeConfig;
import com.vstrike.util.LatencyProbe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla only polls the attack key inside MinecraftClient.handleInputEvents(), which runs once
 * per client tick (20 Hz). A click that arrives just after a tick therefore waits up to 50 ms
 * before the attack packet is built, even though the mouse event itself was handled within the
 * current frame.
 *
 * This hook runs after vanilla has registered the press (KeyBinding.onKeyPressed). If the press
 * would be accepted by the tick path anyway, it is consumed here and doAttack() is called
 * immediately. Anything vanilla would reject or handle differently is left to the tick path:
 * block and miss targets, the miss lockout, item use, screens, riding, and so on.
 *
 * It does not add attacks, change which attacks are legal, or touch attack-strength cooldown
 * (that is tracked in ticks by the player and server).
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void vstrike$markPress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (VStrikeConfig.debugLatencyLog
                && action == GLFW.GLFW_PRESS
                && this.client.options.attackKey.matchesMouse(button)) {
            LatencyProbe.markPress();
        }
    }

    @Inject(method = "onMouseButton", at = @At("TAIL"))
    private void vstrike$earlyAttack(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!VStrikeConfig.earlyAttackDispatch || action != GLFW.GLFW_PRESS) {
            return;
        }

        MinecraftClient mc = this.client;
        if (!mc.options.attackKey.matchesMouse(button)) {
            return;
        }

        // Same gates the tick path applies before it would call doAttack().
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        if (mc.currentScreen != null || mc.getOverlay() != null || mc.isPaused() || !mc.mouse.isCursorLocked()) {
            return;
        }
        // While an item is in use vanilla discards attack presses instead of attacking.
        if (mc.player.isUsingItem()) {
            return;
        }

        // Entity targets only. Blocks (break start) and misses (10-tick lockout) stay on the tick path.
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            return;
        }

        MinecraftClientAccessor accessor = (MinecraftClientAccessor) mc;
        if (accessor.vstrike$getAttackCooldown() > 0) {
            return;
        }

        // Consume the press that vanilla just queued so the tick path does not attack a second time.
        if (!mc.options.attackKey.wasPressed()) {
            return;
        }
        accessor.vstrike$invokeDoAttack();
    }
}
