package game.common;

import com.google.gson.JsonObject;

/**
 * A simple data transfer object.
 * It uses JsonObject for flexible data storage
 */
public class Box {
	private JsonObject payload;

	public Box() {
		this.payload = new JsonObject();
	}

	public Box(JsonObject payload) {
		this.payload = payload;
	}

	public JsonObject getPayload() {
		return payload;
	}

	public void setPayload(JsonObject payload) {
		this.payload = payload;
	}
}