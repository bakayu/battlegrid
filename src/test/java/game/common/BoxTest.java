package game.common;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.junit.jupiter.api.Assertions.*;

class BoxTest {

	@Test
	void testToEncryptedString_AND_FromEncryptedString() throws NoSuchAlgorithmException, NoSuchProviderException {
		JsonObject payload = new JsonObject();
		payload.addProperty("key", "value");

        KeyPair keyPair = Box.generateKeyPair();
		Box originalBox = new Box(payload);

		String encryptedString = originalBox.toEncryptedString(keyPair.getPublic());
		Box decryptedBox = Box.fromEncryptedString(encryptedString, keyPair.getPrivate());

		assertEquals(payload, decryptedBox.getPayload());
	}
}