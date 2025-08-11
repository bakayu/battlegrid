package game.common;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * Handles both encoding (Object -> String) and decoding (String -> Object) for Box class.
 */
public class BoxCodec implements Encoder.Text<Box>, Decoder.Text<Box> {

	// Encoder
	@Override
	public String encode(Box box) throws EncodeException {
		return box.toEncryptedString();
	}

	// Decoder
	@Override
	public Box decode(String s) throws DecodeException {
		return Box.fromEncryptedString(s);
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
