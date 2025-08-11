package game.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Base64;

/**
 * A Wrapper Class for sending data between client and server.
 * It uses JsonObject for flexible data storage and includes a simple encryption/decryption mechanism.
 */
public class Box {

    private static final Gson gson = new Gson();

    // main data payload.
    private JsonObject payload;

    // A simple "encryption" key.
    // TODO: This is for DEMO ONLY, handle properly in PRODUCTION.
    private static final byte[] SECRET_KEY = "this-is-a-demo-private-key".getBytes();

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
     * A Simple XOR-based encryption method.
     *
     * @param data The byte array to encrypt.
     * @return The encrypted byte array.
     */
    // TODO: This is for DEMO ONLY, use a cryptographically secure method later.
    private byte[] encrypt(byte[] data) {
        byte[] output = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            output[i] = (byte) (data[i] ^ SECRET_KEY[i % SECRET_KEY.length]);
        }
        return output;
    }

    /**
     * Decrypts data using XOR-based encryption.
     *
     * @param data The byte array to decrypt.
     * @return The decrypted byte array.
     */
    private byte[] decrypt(byte[] data) {
        // XOR-based decryption, same as method `encrypt`.
        return encrypt(data);
    }

    /**
     * Converts the Box object to an encrypted, Base64-encoded JSON String for transmission.
     *
     * @return An encrypted, Based64-encoded representation of the object.
     */
    public String toEncryptedString() {
        String jsonString = gson.toJson(this.payload);
        byte[] encryptedString = encrypt(jsonString.getBytes());
        return Base64.getEncoder().encodeToString(encryptedString);
    }

    /**
     * Creates a Box object from an encrypted, Base64-encoded JSON string.
     *
     * @param encryptedString The string received from over the network.
     * @return A new Box object with the decrypted payload.
     */
    public static Box fromEncryptedString(String encryptedString) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedString);
        byte[] decryptedBytes = new Box().decrypt(decodedBytes);
        String jsonString = new String(decryptedBytes);
        JsonObject payload = gson.fromJson(jsonString, JsonObject.class);
        return new Box(payload);
    }
}
