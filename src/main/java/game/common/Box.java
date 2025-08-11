package game.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
/**
 * A Wrapper Class for sending data between client and server.
 * It uses JsonObject for flexible data storage and includes a simple encryption/decryption mechanism.
 */
public class Box {

    private static final Gson gson = new Gson();

    // main data payload.
    private JsonObject payload;
    
    // default constructor
    public Box() {
        this.payload = new JsonObject();
    }

    // Constructor to create a box with some initial payload.
    public Box(JsonObject payload) {
        this.payload = payload;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }


    /**
     * Generates an RSA key pair.
     *
     * @return a KeyPair object containing the public and private keys.
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available.
     * @throws NoSuchProviderException  if the Bouncy Castle provider is not available.
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // 2048-bit key size is standard
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Encrypts data using the provided RSA public key.
     *
     * @param data      The byte array to encrypt.
     * @param publicKey The public key of the recipient.
     * @return The encrypted byte array.
     * @throws GeneralSecurityException if an encryption error occurs.
     */
    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts data using the provided RSA private key.
     *
     * @param data       The byte array to decrypt.
     * @param privateKey The private key of the recipient.
     * @return The decrypted byte array.
     * @throws GeneralSecurityException if a decryption error occurs.
     */
    public static byte[] decrypt(byte[] data, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    /**
     * Converts the Box object to an encrypted, Base64-encoded JSON String for transmission.
     *
     * @param publicKey The public key of the recipient.
     * @return An encrypted, Based64-encoded representation of the object.
     */
    public String toEncryptedString(PublicKey publicKey) {
        try {
            String jsonString = gson.toJson(this.payload);
            byte[] encryptedBytes = encrypt(jsonString.getBytes(), publicKey);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Creates a Box object from an encrypted, Base64-encoded JSON string.
     *
     * @param encryptedString The string received from over the network.
     * @param privateKey      The private key of the recipient.
     * @return A new Box object with the decrypted payload.
     */
    public static Box fromEncryptedString(String encryptedString, PrivateKey privateKey) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedString);
            byte[] decryptedBytes = decrypt(decodedBytes, privateKey);
            String jsonString = new String(decryptedBytes);
            JsonObject payload = gson.fromJson(jsonString, JsonObject.class);
            return new Box(payload);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}