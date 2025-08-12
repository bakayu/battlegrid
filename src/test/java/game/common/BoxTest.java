// src/test/java/game/common/BoxTest.java
package game.common;

import com.google.gson.JsonObject;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxTest {

	@Test
	void testBoxCodecEncodeDecode() throws EncodeException, DecodeException {
		JsonObject payload = new JsonObject();
		payload.addProperty("key", "value");
		Box originalBox = new Box(payload);
		BoxCodec codec = new BoxCodec();

		String encoded = codec.encode(originalBox);
		Box decodedBox = codec.decode(encoded);

		assertEquals(payload, decodedBox.getPayload());
	}

	@Test
	void testBoxSetAndGetPayload() {
		JsonObject payload = new JsonObject();
		payload.addProperty("foo", "bar");
		Box box = new Box();

		box.setPayload(payload);

		assertEquals(payload, box.getPayload());
	}
}