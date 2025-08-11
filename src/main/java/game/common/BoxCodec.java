package game.common;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

import java.security.PrivateKey;
import java.security.PublicKey;
/**
 * Handles both encoding (Object -> String) and decoding (String -> Object) for Box class.
 */
public class BoxCodec implements Encoder.Text<Box>, Decoder.Text<Box> {

    // Keys are stored as instance variables
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // Use a default constructor to avoid errors with Tyrus, and a setter
    // to provide the keys after the object is created.
    public BoxCodec() {}

    public void setKeys(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    @Override
    public String encode(Box box) throws EncodeException {
        // We use the public key to encrypt data for the other party.
        if (this.publicKey == null) {
            throw new EncodeException(box, "Public key not set for encoding");
        }
        return box.toEncryptedString(this.publicKey);
    }

	// Decoder
	@Override
    public Box decode(String s) throws DecodeException {
        // We use our private key to decrypt data received from the other party.
        if (this.privateKey == null) {
            throw new DecodeException(s, "Private key not set for decoding");
        }
        return Box.fromEncryptedString(s, this.privateKey);
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
