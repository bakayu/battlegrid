package game.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Renders the HUD elements: fleet status, weapons panel, messages, and stats.
 */
public class HudRenderer {

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";

    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";
    private static final String GRAY = "\033[90m";
    private static final String MAGENTA = "\033[35m";

    // ANSI cursor control
    private static final String ERASE_LINE = "\033[2K";
    private static final String MOVE_UP = "\033[A";

    /**
     * Renders the fleet status panel.
     */
    public static String renderFleetStatus(String title, JsonArray fleet) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(YELLOW).append("  ── ").append(title).append(" ──").append(RESET).append("\n");

        for (int i = 0; i < fleet.size(); i++) {
            JsonObject ship = fleet.get(i).getAsJsonObject();
            String type = ship.get("type").getAsString();
            boolean alive = ship.get("alive").getAsBoolean();
            int health = ship.get("health").getAsInt();
            int maxHealth = ship.get("maxHealth").getAsInt();

            String statusIcon;
            String color;
            if (!alive) {
                statusIcon = "☠";
                color = RED + DIM;
            } else if (health < maxHealth) {
                statusIcon = "⚠";
                color = YELLOW;
            } else {
                statusIcon = "✓";
                color = GREEN;
            }

            sb.append("  ").append(color);
            sb.append(String.format("%s %-6s ", statusIcon, type));

            // Health bar
            sb.append("[");
            for (int h = 0; h < maxHealth; h++) {
                if (h < health) {
                    sb.append(GREEN).append("█").append(color);
                } else {
                    sb.append(RED).append("░").append(color);
                }
            }
            sb.append("] ").append(health).append("/").append(maxHealth);

            if (!alive) {
                sb.append(" SUNK");
            }
            sb.append(RESET).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Renders the available weapons panel.
     */
    public static String renderWeapons(JsonArray weapons) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(MAGENTA).append("  ── WEAPONS ──").append(RESET).append("\n");

        for (int i = 0; i < weapons.size(); i++) {
            JsonObject weapon = weapons.get(i).getAsJsonObject();
            String displayName = weapon.get("displayName").getAsString();
            String description = weapon.get("description").getAsString();
            boolean available = weapon.get("available").getAsBoolean();
            int cooldownRemaining = weapon.get("cooldownRemaining").getAsInt();
            boolean needsDirection = weapon.get("needsDirection").getAsBoolean();

            sb.append("  ");
            if (available) {
                sb.append(GREEN).append(BOLD);
                sb.append(String.format("[%d] %s", i + 1, displayName));
                sb.append(RESET).append(DIM);
                sb.append(" — ").append(description);
                if (needsDirection) {
                    sb.append(" ").append(YELLOW).append("(needs H/V)").append(RESET).append(DIM);
                }
            } else {
                sb.append(GRAY);
                sb.append(String.format("[%d] %s", i + 1, displayName));
                if (cooldownRemaining > 0) {
                    sb.append(String.format(" (CD: %d)", cooldownRemaining));
                } else {
                    sb.append(" (ship sunk)");
                }
            }
            sb.append(RESET).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Renders the game message log.
     */
    public static String renderMessages(JsonArray messages) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(BLUE).append("  ── BATTLE LOG ──").append(RESET).append("\n");

        if (messages.isEmpty()) {
            sb.append(GRAY).append("    No messages yet.").append(RESET).append("\n");
        } else {
            for (int i = 0; i < messages.size(); i++) {
                String msg = messages.get(i).getAsString();
                sb.append(GRAY).append("    ").append(msg).append(RESET).append("\n");
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Renders the game over stats screen.
     */
    public static String renderGameOver(JsonObject gameOverPayload) {
        StringBuilder sb = new StringBuilder();
        boolean youWon = gameOverPayload.get("youWon").getAsBoolean();
        String winner = gameOverPayload.get("winner").getAsString();

        sb.append("\n");
        if (youWon) {
            sb.append(GREEN).append(BOLD);
            sb.append("  ┌──────────────────────────────┐\n");
            sb.append("  │       🎉 VICTORY! 🎉         │\n");
            sb.append("  └──────────────────────────────┘\n");
        } else {
            sb.append(RED).append(BOLD);
            sb.append("  ┌──────────────────────────────┐\n");
            sb.append("  │         💀 DEFEAT 💀          │\n");
            sb.append("  └──────────────────────────────┘\n");
        }
        sb.append(RESET).append("\n");

        sb.append(BOLD).append("  Winner: ").append(CYAN).append(winner).append(RESET).append("\n\n");

        // Your stats
        JsonObject yourStats = gameOverPayload.getAsJsonObject("yourStats");
        sb.append(renderStats("YOUR STATS", yourStats));

        // Opponent stats
        JsonObject oppStats = gameOverPayload.getAsJsonObject("opponentStats");
        sb.append(renderStats("OPPONENT STATS", oppStats));

        // Messages
        if (gameOverPayload.has("messages")) {
            sb.append(renderMessages(gameOverPayload.getAsJsonArray("messages")));
        }

        return sb.toString();
    }

    private static String renderStats(String title, JsonObject stats) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(YELLOW).append("  ── ").append(title).append(" ──").append(RESET).append("\n");
        sb.append(String.format("    Turns taken : %d\n", stats.get("turnsTaken").getAsInt()));
        sb.append(String.format("    Shots fired : %d\n", stats.get("shotsFired").getAsInt()));
        sb.append(String.format("    Shots hit   : %d\n", stats.get("shotsHit").getAsInt()));
        sb.append(String.format("    Hit rate    : %s\n", stats.get("hitRate").getAsString()));
        sb.append(String.format("    Ships lost  : %d/%d\n",
                stats.get("shipsLost").getAsInt(), stats.get("totalShips").getAsInt()));
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Renders the input prompt area.
     */
    public static String renderInputBlock(String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(WHITE);
        sb.append("  ─────────────────────────────────────────────────\n");
        sb.append("  ").append(CYAN).append("Command: ").append(WHITE)
                .append("<weapon#> <target> [H/V]");
        sb.append("    ").append(DIM).append("e.g. ").append(GREEN).append("1 B5")
                .append(WHITE).append(BOLD).append("  ").append(GREEN).append("2 D4 H\n");
        sb.append(RESET).append(DIM);
        sb.append("  Type 'forfeit' to surrender.\n");
        sb.append(BOLD).append(WHITE);
        sb.append("  ─────────────────────────────────────────────────\n");
        sb.append(RESET);

        // Error line
        if (errorMessage != null && !errorMessage.isEmpty()) {
            sb.append("  ").append(RED).append(BOLD).append("✖ ").append(errorMessage).append(RESET).append("\n");
        } else {
            sb.append("\n");
        }

        // Prompt
        sb.append(CYAN).append(BOLD).append("  ❯ ").append(RESET);
        return sb.toString();
    }

    /**
     * Overwrites the error line and prompt in-place (2 lines up, clear, reprint).
     */
    public static String updateErrorInPlace(String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(MOVE_UP).append(ERASE_LINE);
        sb.append(MOVE_UP).append(ERASE_LINE);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            sb.append("\r  ").append(RED).append(BOLD).append("✖ ").append(errorMessage).append(RESET).append("\n");
        } else {
            sb.append("\r\n");
        }

        sb.append(CYAN).append(BOLD).append("  ❯ ").append(RESET);
        return sb.toString();
    }

    /**
     * Renders the turn prompt for input.
     * Convenience method — delegates to renderInputBlock with no error.
     */
    public static String renderTurnPrompt() {
        return renderInputBlock(null);
    }

    /**
     * Renders a waiting message.
     */
    public static String renderWaiting(String message) {
        return "\n" + YELLOW + BOLD + "  ⏳ " + message + RESET + "\n";
    }

    /**
     * Clears the terminal screen and moves cursor to top-left.
     */
    public static String clearScreen() {
        return "\033[2J\033[H";
    }

    /**
     * Renders the header bar.
     */
    public static String renderHeader(int turnNumber, String mode, String opponentName) {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(CYAN);
        sb.append("  ⚔ BattleGrid — Turn ").append(turnNumber).append(" ⚔\n");
        sb.append(RESET).append(DIM);
        sb.append("  Mode: ").append(mode).append("  │  Opponent: ").append(opponentName);
        sb.append(RESET).append("\n\n");
        return sb.toString();
    }
}
