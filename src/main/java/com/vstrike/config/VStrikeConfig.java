package com.vstrike.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Plain properties file at config/vstrike.properties. There is deliberately no in-game
 * screen for it; edit the file and restart the game.
 */
public final class VStrikeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("VStrike");

    /** Send entity attacks from the mouse event instead of waiting for the next client tick. */
    public static volatile boolean earlyAttackDispatch = true;

    /** Start the local player's swing animation one tick sooner (cosmetic only). */
    public static volatile boolean swingStartCompensation = true;

    /** Log (to latest.log only, never on screen) how long attack clicks wait before dispatch. */
    public static volatile boolean debugLatencyLog = false;

    private VStrikeConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("vstrike.properties");
        Properties props = new Properties();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                LOGGER.warn("Could not read {}, using defaults", path, e);
            }
        }

        earlyAttackDispatch = readBool(props, "earlyAttackDispatch", earlyAttackDispatch);
        swingStartCompensation = readBool(props, "swingStartCompensation", swingStartCompensation);
        debugLatencyLog = readBool(props, "debugLatencyLog", debugLatencyLog);

        // Rewrite so new keys appear and the file stays self-documenting.
        props.setProperty("earlyAttackDispatch", Boolean.toString(earlyAttackDispatch));
        props.setProperty("swingStartCompensation", Boolean.toString(swingStartCompensation));
        props.setProperty("debugLatencyLog", Boolean.toString(debugLatencyLog));
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out,
                    "VStrike\n"
                            + "earlyAttackDispatch: dispatch entity attacks at mouse-event time instead of the next client tick\n"
                            + "swingStartCompensation: start the local swing animation one tick sooner (visual only)\n"
                            + "debugLatencyLog: write click->attack dispatch latency stats to latest.log");
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", path, e);
        }

        LOGGER.info("VStrike config: earlyAttackDispatch={}, swingStartCompensation={}, debugLatencyLog={}",
                earlyAttackDispatch, swingStartCompensation, debugLatencyLog);
    }

    private static boolean readBool(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }
}
