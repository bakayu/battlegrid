package game.server.game;

import java.util.List;

import game.common.model.CellState;
import game.common.model.Coordinate;
import game.common.model.Ship;
import game.common.model.WeaponType;

/**
 * Represents the result of firing a weapon at the board.
 */
public class AttackResult {

    /**
     * Result for a single tile within an attack.
     */
    public record TileResult(Coordinate coordinate, CellState outcome) {
    }

    private final WeaponType weapon;
    private final Coordinate target;
    private final List<TileResult> tileResults;
    private final List<Ship> sunkShips;
    private final boolean gameOver;

    public AttackResult(WeaponType weapon, Coordinate target,
            List<TileResult> tileResults, List<Ship> sunkShips, boolean gameOver) {
        this.weapon = weapon;
        this.target = target;
        this.tileResults = tileResults;
        this.sunkShips = sunkShips;
        this.gameOver = gameOver;
    }

    public WeaponType getWeapon() {
        return weapon;
    }

    public Coordinate getTarget() {
        return target;
    }

    public List<TileResult> getTileResults() {
        return tileResults;
    }

    /** Ships that were sunk as a result of this attack */
    public List<Ship> getSunkShips() {
        return sunkShips;
    }

    /** True if this attack ended the game (all opponent ships sunk) */
    public boolean isGameOver() {
        return gameOver;
    }

    public int hitCount() {
        return (int) tileResults.stream()
                .filter(r -> r.outcome() == CellState.HIT || r.outcome() == CellState.SUNK)
                .count();
    }

    public int missCount() {
        return (int) tileResults.stream()
                .filter(r -> r.outcome() == CellState.MISS)
                .count();
    }
}
