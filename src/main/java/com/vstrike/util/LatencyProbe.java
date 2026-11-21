package com.vstrike.util;

import com.vstrike.config.VStrikeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log-only measurement of the delay between an attack-button mouse press and the moment
 * MinecraftClient.doAttack() runs. Main-thread only; no synchronization needed.
 *
 * Compare the logged mean with earlyAttackDispatch on versus off: vanilla quantizes the
 * dispatch to the 20 Hz client tick, so expect roughly 0-50 ms (mean ~25 ms) when off.
 */
public final class LatencyProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("VStrike");
    private static final int WINDOW = 50;

    private static boolean pending;
    private static long pressNanos;

    private static int count;
    private static long sumNanos;
    private static long maxNanos;

    private LatencyProbe() {
    }

    public static void markPress() {
        if (!pending) {
            pending = true;
            pressNanos = System.nanoTime();
        }
    }

    public static void onAttackDispatched() {
        if (!VStrikeConfig.debugLatencyLog || !pending) {
            return;
        }
        pending = false;
        long delta = System.nanoTime() - pressNanos;
        sumNanos += delta;
        maxNanos = Math.max(maxNanos, delta);

        if (++count >= WINDOW) {
            LOGGER.info("click->attack dispatch over {} attacks: mean {} ms, max {} ms (earlyAttackDispatch={})",
                    count,
                    String.format("%.2f", sumNanos / (double) count / 1_000_000.0),
                    String.format("%.2f", maxNanos / 1_000_000.0),
                    VStrikeConfig.earlyAttackDispatch);
            count = 0;
            sumNanos = 0;
            maxNanos = 0;
        }
    }
}
