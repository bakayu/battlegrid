package game.common;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxTest {

	@Test
	void testToEncryptedString_AND_FromEncryptedString() {
		JsonObject payload = new JsonObject();
		payload.addProperty("key", "value");
		Box originalBox = new Box(payload);

		String encryptedString = originalBox.toEncryptedString();
		Box decryptedBox = Box.fromEncryptedString(encryptedString);

		assertEquals(payload, decryptedBox.getPayload());
	}
}
