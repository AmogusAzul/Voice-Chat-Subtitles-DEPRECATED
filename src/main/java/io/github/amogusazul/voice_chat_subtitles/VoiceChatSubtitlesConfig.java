package io.github.amogusazul.voice_chat_subtitles;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.loader.api.config.v2.QuiltConfig;

public class VoiceChatSubtitlesConfig extends ReflectiveConfig {

	public static final VoiceChatSubtitlesConfig INSTANCE = QuiltConfig.create(VoiceChatSubtitles.MOD_ID, VoiceChatSubtitles.MOD_ID, VoiceChatSubtitlesConfig.class);

	@Comment("This is the ggml's whisper model, more info in: https://github.com/ggerganov/whisper.cpp")
	public final TrackedValue<String> model = this.value("subtitles/ggml-base.bin");
}
