package game.common.model;

/**
 * Weapon types available in the game.
 * Each weapon has a fire pattern and a cooldown duration (in turns).
 */
public enum WeaponType {
    STANDARD_SHOT("Standard Shot", "1x1 single tile", 0),
    LINE_BARRAGE("Line Barrage", "1x3 line (H or V)", 2),
    CROSS_BOMBER("Cross Bomber", "+ shape (5 tiles)", 3),
    NUKE("Nuke", "3×3 square (9 tiles)", 5);

    private final String displayName;
    private final String description;
    private final int cooldown;

    WeaponType(String displayName, String description, int cooldown) {
        this.displayName = displayName;
        this.description = description;
        this.cooldown = cooldown;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Number of turns this weapon is unavailable after being fired.
     * 0 means no cooldown (can fire every turn).
     */
    public int getCooldown() {
        return cooldown;
    }
}
