package game.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.websocket.*;

/**
 * The codec's job is to convert a Box object to a JSON string and back.
 */
public class BoxCodec implements Encoder.Text<Box>, Decoder.Text<Box> {

	private static final Gson gson = new Gson();

	@Override
	public String encode(Box box) throws EncodeException {
		return gson.toJson(box.getPayload());
	}

	@Override
	public Box decode(String s) throws DecodeException {
		return new Box(gson.fromJson(s, JsonObject.class));
	}

	@Override
	public boolean willDecode(String s) {
		// We want to try decoding all non-empty messages.
		return (s != null && !s.isEmpty());
	}

	@Override
	public void init(EndpointConfig config) {
		// No initialization needed for now
	}

	@Override
	public void destroy() {
		// No cleanup needed for this simple codec
	}
}
