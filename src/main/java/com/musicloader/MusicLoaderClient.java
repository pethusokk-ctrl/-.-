package com.musicloader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicLoaderClient implements ClientModInitializer {
    public static final String MOD_ID = "musicloader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        MusicLibrary.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MusicPlayer.shutdown());
        LOGGER.info("Music Loader запущен. Папка: {}", MusicLibrary.DIR);
    }
}
