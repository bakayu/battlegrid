package game.common.model;

import java.util.List;

/**
 * Game modes with their grid sizes and fleet compositions.
 */
public enum GameMode {
    BLITZ("Blitz", 8, List.of(
            ShipType.PATROL_BOAT,
            ShipType.SUBMARINE,
            ShipType.DESTROYER)),
    STRIKE("Strike", 12, List.of(
            ShipType.PATROL_BOAT,
            ShipType.SUBMARINE,
            ShipType.DESTROYER,
            ShipType.BATTLESHIP,
            ShipType.CARRIER)),
    WAR("War", 16, List.of(
            ShipType.PATROL_BOAT,
            ShipType.PATROL_BOAT,
            ShipType.SUBMARINE,
            ShipType.DESTROYER,
            ShipType.DESTROYER,
            ShipType.BATTLESHIP,
            ShipType.CARRIER));

    private final String displayName;
    private final int gridSize;
    private final List<ShipType> fleet;

    GameMode(String displayName, int gridSize, List<ShipType> fleet) {
        this.displayName = displayName;
        this.gridSize = gridSize;
        this.fleet = fleet;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getGridSize() {
        return gridSize;
    }

    /** Ordered list of ships deployed in this mode */
    public List<ShipType> getFleet() {
        return fleet;
    }

    public int getShipCount() {
        return fleet.size();
    }
}
