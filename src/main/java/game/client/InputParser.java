package game.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Parses player input commands into structured attack data.
 * 
 * Format: <weapon#> <target> [direction]
 * Examples:
 * 1 B5 → Standard Shot at B5
 * 3 D4 H → Line Barrage at D4, horizontal
 * forfeit → Player forfeits
 */
public class InputParser {

    /**
     * Result of parsing player input.
     */
    public record ParsedInput(
            boolean isForfeit,
            boolean isValid,
            String weaponName,
            String target,
            String direction,
            String errorMessage) {
        public static ParsedInput forfeit() {
            return new ParsedInput(true, true, null, null, null, null);
        }

        public static ParsedInput valid(String weaponName, String target, String direction) {
            return new ParsedInput(false, true, weaponName, target, direction, null);
        }

        public static ParsedInput invalid(String errorMessage) {
            return new ParsedInput(false, false, null, null, null, errorMessage);
        }
    }

    /**
     * Parses raw input string into a ParsedInput.
     *
     * @param input   the raw user input
     * @param weapons the available weapons array from the server
     * @return parsed result
     */
    public static ParsedInput parse(String input, JsonArray weapons) {
        if (input == null || input.trim().isEmpty()) {
            return ParsedInput.invalid("No input provided.");
        }

        String trimmed = input.trim();

        // Check for forfeit
        if (trimmed.equalsIgnoreCase("forfeit") || trimmed.equalsIgnoreCase("ff")) {
            return ParsedInput.forfeit();
        }

        // Split into parts
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) {
            return ParsedInput.invalid("Usage: <weapon#> <target> [H/V]  — Example: 1 B5");
        }

        // Parse weapon number
        int weaponIndex;
        try {
            weaponIndex = Integer.parseInt(parts[0]) - 1; // 1-indexed to 0-indexed
        } catch (NumberFormatException e) {
            return ParsedInput.invalid("Invalid weapon number: " + parts[0]);
        }

        if (weapons == null || weaponIndex < 0 || weaponIndex >= weapons.size()) {
            return ParsedInput.invalid("Weapon number out of range. Valid: 1-" +
                    (weapons != null ? weapons.size() : "?"));
        }

        JsonObject weapon = weapons.get(weaponIndex).getAsJsonObject();

        // Check if weapon is available
        if (!weapon.get("available").getAsBoolean()) {
            int cd = weapon.get("cooldownRemaining").getAsInt();
            if (cd > 0) {
                return ParsedInput.invalid(weapon.get("displayName").getAsString()
                        + " is on cooldown (" + cd + " turns remaining).");
            } else {
                return ParsedInput.invalid(weapon.get("displayName").getAsString()
                        + " is unavailable (ship sunk).");
            }
        }

        String weaponName = weapon.get("name").getAsString();

        // Parse target coordinate
        String target = parts[1].toUpperCase();
        if (!isValidCoordinate(target)) {
            return ParsedInput.invalid("Invalid coordinate: " + parts[1]
                    + ". Use format like B5, A1, L12.");
        }

        // Parse direction (optional)
        String direction = "HORIZONTAL"; // default
        boolean needsDirection = weapon.get("needsDirection").getAsBoolean();

        if (parts.length >= 3) {
            String dirInput = parts[2].toUpperCase();
            if (dirInput.equals("H") || dirInput.equals("HORIZONTAL")) {
                direction = "HORIZONTAL";
            } else if (dirInput.equals("V") || dirInput.equals("VERTICAL")) {
                direction = "VERTICAL";
            } else {
                return ParsedInput.invalid("Invalid direction: " + parts[2]
                        + ". Use H (horizontal) or V (vertical).");
            }
        } else if (needsDirection) {
            return ParsedInput.invalid(weapon.get("displayName").getAsString()
                    + " requires a direction. Usage: " + (weaponIndex + 1) + " " + target + " H/V");
        }

        return ParsedInput.valid(weaponName, target, direction);
    }

    /**
     * Validates a coordinate string like "A1", "B5", "L12".
     */
    private static boolean isValidCoordinate(String coord) {
        if (coord == null || coord.length() < 2 || coord.length() > 3) {
            return false;
        }
        char row = coord.charAt(0);
        if (row < 'A' || row > 'P') { // max 16x16 grid
            return false;
        }
        try {
            int col = Integer.parseInt(coord.substring(1));
            return col >= 1 && col <= 16;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
