package game.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudRendererTest {

    @Test
    void testRenderFleetStatus() {
        JsonArray fleet = new JsonArray();
        fleet.add(createShip("PB-A", "PB", true, 2, 2));
        fleet.add(createShip("DS-A", "DS", true, 1, 3));
        fleet.add(createShip("SUB-A", "SUB", false, 0, 3));

        String rendered = HudRenderer.renderFleetStatus("YOUR FLEET", fleet);

        assertNotNull(rendered);
        assertTrue(rendered.contains("YOUR FLEET"));
        assertTrue(rendered.contains("PB"));
        assertTrue(rendered.contains("SUNK"));
    }

    @Test
    void testRenderWeapons() {
        JsonArray weapons = new JsonArray();

        JsonObject w1 = new JsonObject();
        w1.addProperty("name", "STANDARD_SHOT");
        w1.addProperty("displayName", "Standard Shot");
        w1.addProperty("description", "Single tile");
        w1.addProperty("available", true);
        w1.addProperty("cooldownRemaining", 0);
        w1.addProperty("needsDirection", false);
        weapons.add(w1);

        JsonObject w2 = new JsonObject();
        w2.addProperty("name", "NUKE");
        w2.addProperty("displayName", "Nuke");
        w2.addProperty("description", "3x3 area");
        w2.addProperty("available", false);
        w2.addProperty("cooldownRemaining", 3);
        w2.addProperty("needsDirection", false);
        weapons.add(w2);

        String rendered = HudRenderer.renderWeapons(weapons);

        assertNotNull(rendered);
        assertTrue(rendered.contains("WEAPONS"));
        assertTrue(rendered.contains("Standard Shot"));
        assertTrue(rendered.contains("Nuke"));
        assertTrue(rendered.contains("CD: 3"));
    }

    @Test
    void testRenderMessages() {
        JsonArray messages = new JsonArray();
        messages.add("Alice fired Standard Shot at B5");
        messages.add("  → 1 hit(s)!");

        String rendered = HudRenderer.renderMessages(messages);

        assertTrue(rendered.contains("BATTLE LOG"));
        assertTrue(rendered.contains("Alice fired"));
    }

    @Test
    void testRenderGameOver_win() {
        JsonObject payload = createGameOverPayload(true);
        String rendered = HudRenderer.renderGameOver(payload);

        assertTrue(rendered.contains("VICTORY"));
        assertTrue(rendered.contains("YOUR STATS"));
        assertTrue(rendered.contains("OPPONENT STATS"));
    }

    @Test
    void testRenderGameOver_loss() {
        JsonObject payload = createGameOverPayload(false);
        String rendered = HudRenderer.renderGameOver(payload);

        assertTrue(rendered.contains("DEFEAT"));
    }

    @Test
    void testRenderTurnPrompt() {
        String rendered = HudRenderer.renderTurnPrompt();

        assertTrue(rendered.contains("weapon#"));
        assertTrue(rendered.contains("target"));
        assertTrue(rendered.contains("forfeit"));
    }

    @Test
    void testRenderInputBlock_noError() {
        String rendered = HudRenderer.renderInputBlock(null);

        assertTrue(rendered.contains("weapon#"));
        assertTrue(rendered.contains("❯"));
    }

    @Test
    void testRenderInputBlock_withError() {
        String rendered = HudRenderer.renderInputBlock("Invalid coordinate");

        assertTrue(rendered.contains("Invalid coordinate"));
        assertTrue(rendered.contains("✖"));
        assertTrue(rendered.contains("❯"));
    }

    @Test
    void testRenderWaiting() {
        String rendered = HudRenderer.renderWaiting("Waiting for Bob...");
        assertTrue(rendered.contains("Waiting for Bob"));
    }

    @Test
    void testClearScreen() {
        String clear = HudRenderer.clearScreen();
        assertEquals("\033[2J\033[H", clear);
    }

    @Test
    void testRenderHeader() {
        String header = HudRenderer.renderHeader(5, "BLITZ", "Bob");

        assertTrue(header.contains("Turn 5"));
        assertTrue(header.contains("BLITZ"));
        assertTrue(header.contains("Bob"));
    }

    private JsonObject createShip(String id, String type, boolean alive, int health, int maxHealth) {
        JsonObject ship = new JsonObject();
        ship.addProperty("id", id);
        ship.addProperty("type", type);
        ship.addProperty("alive", alive);
        ship.addProperty("health", health);
        ship.addProperty("maxHealth", maxHealth);
        return ship;
    }

    private JsonObject createGameOverPayload(boolean youWon) {
        JsonObject payload = new JsonObject();
        payload.addProperty("youWon", youWon);
        payload.addProperty("winner", youWon ? "Alice" : "Bob");

        JsonObject stats = new JsonObject();
        stats.addProperty("turnsTaken", 10);
        stats.addProperty("shotsFired", 15);
        stats.addProperty("shotsHit", 8);
        stats.addProperty("hitRate", "53.3%");
        stats.addProperty("shipsLost", 2);
        stats.addProperty("totalShips", 5);

        payload.add("yourStats", stats);
        payload.add("opponentStats", stats);
        payload.add("messages", new JsonArray());

        return payload;
    }
}
