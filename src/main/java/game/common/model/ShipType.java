package game.common.model;

/**
 * Ship classes in BattleGrid.
 * Each ship type has a size (length), a width, a display code,
 * and a unique weapon it provides.
 */
public enum ShipType {
    PATROL_BOAT("Patrol Boat", "PB", 2, 1, WeaponType.STANDARD_SHOT),
    SUBMARINE("Submarine", "SUB", 3, 1, WeaponType.STANDARD_SHOT),
    DESTROYER("Destroyer", "DS", 3, 1, WeaponType.LINE_BARRAGE),
    BATTLESHIP("Battleship", "BATS", 4, 1, WeaponType.CROSS_BOMBER),
    CARRIER("Carrier", "CARR", 5, 2, WeaponType.NUKE);

    private final String displayName;
    private final String code;
    private final int length;
    private final int width;
    private final WeaponType weapon;

    ShipType(String displayName, String code, int length, int width, WeaponType weapon) {
        this.displayName = displayName;
        this.code = code;
        this.length = length;
        this.width = width;
        this.weapon = weapon;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Short code for HUD display (e.g., "PB", "DS", "CARR") */
    public String getCode() {
        return code;
    }

    /** Length of the ship in tiles */
    public int getLength() {
        return length;
    }

    /** Width of the ship in tiles (1 for most, 2 for Carrier) */
    public int getWidth() {
        return width;
    }

    /** The special weapon this ship class provides */
    public WeaponType getWeapon() {
        return weapon;
    }

    /** Total number of tiles this ship occupies */
    public int getTileCount() {
        return length * width;
    }
}
