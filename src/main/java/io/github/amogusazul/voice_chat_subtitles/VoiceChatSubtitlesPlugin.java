package io.github.amogusazul.voice_chat_subtitles;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import io.github.givimad.whisperjni.WhisperFullParams;
import  io.github.givimad.whisperjni.WhisperJNI;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

public class VoiceChatSubtitlesPlugin implements VoicechatPlugin {

	public static final HashMap<String, byte[]> IS_TALKING = new HashMap<>();

	@Override
	public String getPluginId() {
		return VoiceChatSubtitles.MOD_ID;
	}

	@Override
	public void registerEvents(EventRegistration registration){
		// registering all events
		registration.registerEvent(MicrophonePacketEvent.class, this::playerSentPacket);

	}

	// it's triggered everytime a player's packet arrives at the server
	public void playerSentPacket(MicrophonePacketEvent event){

		//making sure that the sender is a player
		if (event.getSenderConnection().getPlayer() == null){
			return;
		}

		//getting player UUID
		String senderUUID = event.getSenderConnection().getPlayer().getUuid().toString();

		byte[] packet = event.getPacket().getOpusEncodedData();

		if (packet.length  > 0) {
			if (IS_TALKING.containsKey(senderUUID)){
				byte[] previousPacket = event.getPacket().getOpusEncodedData();

				//making a new byte array merging the player's previous and new packet
				IS_TALKING.put(senderUUID, mergePackets(IS_TALKING.get(senderUUID), previousPacket));

			} else{
				IS_TALKING.put(senderUUID, packet);
			}
		} else{
			if (IS_TALKING.containsKey(senderUUID)){
				try {
					transcribeAndSend(event.getVoicechat(), packet);
				} catch (IOException e) {
					//giving error to player if the model isn't found
					PlayerEntity player = (PlayerEntity) event.getSenderConnection().getPlayer().getEntity();
					player.sendMessage(Text.translatable("misc.voicechat_subtitles.model_not_found").setStyle(Style.EMPTY.withColor(TextColor.parse("red"))), true);

				}
			}
			IS_TALKING.remove(senderUUID);
		}
	}

	private byte[] mergePackets(byte[] packet1, byte[] packet2){

		byte[] mergedPacket = new byte[packet1.length + packet2.length];

		System.arraycopy(packet1, 0, mergedPacket, 0, packet1.length);
		System.arraycopy(packet2, 0, mergedPacket, packet1.length, packet2.length);

		return mergedPacket;
	}

	public void transcribeAndSend(VoicechatApi api, byte[] packet) throws IOException {

		// Decoding from Opus
		OpusDecoder opusDecoder = api.createDecoder();
		short[] decodedAudio = opusDecoder.decode(packet);

		System.out.println("short[] of opus decoded data:");
		System.out.println(Arrays.toString(decodedAudio));

		// Converting the decoded samples to whisper's format
		float[] whisperSamples = decodedOpus2WhisperSamples(decodedAudio, decodedAudio.length);

		String text = transcribe(whisperSamples);

		System.out.println(text);



	}

	private float[] decodedOpus2WhisperSamples(short[] pcm16Samples, int numSamples) {

		// Convert short[] to float[] (32-bit float)
		float[] floatSamples = new float[numSamples];
		for (int i = 0; i < numSamples; i++){
			floatSamples[i] =  pcm16Samples[i] / 32768f;
		}


		// DownSampling the samples from 48kHz to 16kHz
		int reRatedSampleNum = floatSamples.length / 3;
		float[] resampledSamples = new float[reRatedSampleNum + 1];
		int compressedIndex = 0;
		for (int i = 0; i < numSamples; i++){
			if (i%3 == 0){
				resampledSamples[compressedIndex] = floatSamples[i];
				compressedIndex++;
			}
		}

		return resampledSamples;

	}

	public String transcribe(float[] decodedAudio) throws IOException {

		// whisper initialization
		WhisperJNI.loadLibrary(); // load platform binaries
		WhisperJNI.setLibraryLogger(null);
		var whisper = new WhisperJNI();

		// getting whisper's model (selectable through config)
		var ctx = whisper.init(Path.of(String.valueOf(QuiltLoader.getGameDir()), VoiceChatSubtitlesConfig.INSTANCE.model.value()));
		var params = new WhisperFullParams();
		params.detectLanguage = true;

		// transcribing
		int result = whisper.full(ctx, params, decodedAudio, decodedAudio.length);

		//error handling
		if(result != 0) {
			throw new RuntimeException("Transcription failed with code " + result);
		}


		int numSegments = whisper.fullNSegments(ctx);
		System.out.println(numSegments);

		if (numSegments > 0){
			return whisper.fullGetSegmentText(ctx,0);
		} else{
			return "no segments founded";
		}


	}
}
