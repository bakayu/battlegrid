package game.common;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for cryptographic operations - RSA and AES
 * encryption/decryption,
 * key generation, and key conversion.
 */
public class CryptoUtils {

	private CryptoUtils() {
		// Private constructor to prevent instantiation
	}

	// RSA Methods

	/**
	 * Generates an RSA key pair (public and private keys).
	 *
	 * @return a KeyPair object containing the RSA public and private key.
	 * @throws NoSuchAlgorithmException if the RSA algorithm is not available.
	 */
	public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Encrypts data using the provided RSA public key.
	 *
	 * @param data      the byte data to encrypt.
	 * @param publicKey the RSA public key.
	 * @return the encrypted byte array.
	 * @throws GeneralSecurityException
	 */
	public static byte[] rsaEncrypt(byte[] data, Key publicKey) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(data);
	}

	/**
	 * Decrypts data using the provided RSA private key.
	 *
	 * @param data       the RSA encrypted byte data to decrypt.
	 * @param privateKey the RSA private key.
	 * @return the decrypted byte array
	 * @throws GeneralSecurityException
	 */
	public static byte[] rsaDecrypt(byte[] data, Key privateKey) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data);
	}

	// AES Methods

	/**
	 * Generates a new AES secret key.
	 *
	 * @return a SecretKey object for AES encryption.
	 * @throws NoSuchAlgorithmException if the AES algorithm is not available.
	 */
	public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256); // 256-bit AES key
		return keyGen.generateKey();
	}

	/**
	 * Encrypts data using the provided AES secret key.
	 *
	 * @param data the byte array to encrypt.
	 * @param key  the AES secret key.
	 * @return the encrypted byte array.
	 * @throws GeneralSecurityException
	 */
	public static byte[] aesEncrypt(byte[] data, SecretKey key) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	/**
	 * Decrypts data using the provided AES secret key.
	 *
	 * @param data the AES encrypted byte data to decrypt.
	 * @param key  the AES secret key.
	 * @return the decrypted byte array.
	 * @throws GeneralSecurityException
	 */
	public static byte[] aesDecrypt(byte[] data, SecretKey key) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	/**
	 * Converts an RSA PublicKey to a Base64-encoded string.
	 */
	public static String publicKeyToString(PublicKey publicKey) {
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	/**
	 * Converts raw AES key bytes into a SecretKey object.
	 */
	public static SecretKey bytesToAesKey(byte[] keyBytes) {
		return new SecretKeySpec(keyBytes, "AES");
	}

	// KEY Conversion Methods

	/**
	 * Converts a Key object to a Base64-encoded string.
	 *
	 * @param key the Key object to convert.
	 * @return the Base64-encoded string representation of the key.
	 */
	public static String keyToString(Key key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	/**
	 * Converts a Base64-encoded string to an RSA PublicKey object.
	 *
	 * @param keyString the Base64-encoded public key string.
	 * @return the PublicKey object.
	 * @throws GeneralSecurityException if a conversion error occurs.
	 */
	public static PublicKey stringToPublicKey(String keyString) throws GeneralSecurityException {
		byte[] keyBytes = Base64.getDecoder().decode(keyString);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(spec);
	}

	/**
	 * Converts a Base64-encoded string to an AES SecretKey object.
	 *
	 * @param keyString the Base64-encoded AES key string.
	 * @return the SecretKey object.
	 */
	public static SecretKey stringToAesKey(String keyString) {
		byte[] decodedKey = Base64.getDecoder().decode(keyString);
		return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
	}
}
