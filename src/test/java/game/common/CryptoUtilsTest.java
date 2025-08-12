package game.common;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

	@Test
		// Should decrypt to original data using RSA keys
	void testRSAEncryptAndDecrypt() throws Exception {
		KeyPair keyPair = CryptoUtils.generateRSAKeyPair();
		byte[] data = "hello".getBytes();

		byte[] encrypted = CryptoUtils.rsaEncrypt(data, keyPair.getPublic());
		byte[] decrypted = CryptoUtils.rsaDecrypt(encrypted, keyPair.getPrivate());

		assertArrayEquals(data, decrypted);
	}

	@Test
		// Should decrypt to original data using AES key
	void testAesEncryptAndDecrypt() throws Exception {
		SecretKey key = CryptoUtils.generateAESKey();
		byte[] data = "world".getBytes();

		byte[] encrypted = CryptoUtils.aesEncrypt(data, key);
		byte[] decrypted = CryptoUtils.aesDecrypt(encrypted, key);

		assertArrayEquals(data, decrypted);
	}

	@Test
		// Should convert public key to string and back without loss
	void testKeyToStringAndStringToPublicKey() throws Exception {
		KeyPair keyPair = CryptoUtils.generateRSAKeyPair();

		String pubKeyStr = CryptoUtils.keyToString(keyPair.getPublic());

		assertNotNull(pubKeyStr);
		assertFalse(pubKeyStr.isEmpty());
		assertNotNull(CryptoUtils.stringToPublicKey(pubKeyStr));
	}

	@Test
		// Should convert AES key to string and back without loss
	void testKeyToStringAndStringToAesKey() throws Exception {
		SecretKey key = CryptoUtils.generateAESKey();

		String keyStr = CryptoUtils.keyToString(key);

		assertNotNull(keyStr);
		assertFalse(keyStr.isEmpty());

		SecretKey restoredKey = CryptoUtils.stringToAesKey(keyStr);

		assertEquals(key.getAlgorithm(), restoredKey.getAlgorithm());
		assertArrayEquals(key.getEncoded(), restoredKey.getEncoded());
	}
}