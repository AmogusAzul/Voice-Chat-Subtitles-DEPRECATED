package io.github.amogusazul.voice_chat_subtitles;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoiceChatSubtitles implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Voice Chat Subtitles");

    @Override
    public void onInitialize(ModContainer mod) {
        LOGGER.info("Hello Quilt world from {}! Stay fresh!", mod.metadata().name());
    }
}