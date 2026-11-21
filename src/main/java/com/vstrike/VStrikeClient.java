package com.vstrike;

import com.vstrike.config.VStrikeConfig;
import net.fabricmc.api.ClientModInitializer;

public final class VStrikeClient implements ClientModInitializer {
    public static final String MOD_ID = "vstrike";

    @Override
    public void onInitializeClient() {
        VStrikeConfig.load();
    }
}
