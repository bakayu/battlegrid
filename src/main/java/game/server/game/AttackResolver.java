package game.server.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import game.common.model.Board;
import game.common.model.CellState;
import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.Ship;
import game.common.model.WeaponType;

/**
 * Resolves weapon attacks against a target board.
 * Computes affected tiles, applies damage, detects sinks and game-over.
 */
public class AttackResolver {

    /**
     * Resolves a weapon attack against the defender's board.
     *
     * @param defenderBoard the board being attacked
     * @param weapon        the weapon type used
     * @param target        the target coordinate (center of the pattern)
     * @param direction     direction for directional weapons (LINE_BARRAGE)
     * @return an AttackResult with all outcomes
     */
    public AttackResult resolve(Board defenderBoard, WeaponType weapon,
            Coordinate target, Direction direction) {

        List<Coordinate> affectedTiles = WeaponPatterns.getAffectedTiles(
                weapon, target, direction, defenderBoard.getSize());

        List<AttackResult.TileResult> tileResults = new ArrayList<>();
        Set<Ship> newlySunkShips = new HashSet<>();

        for (Coordinate tile : affectedTiles) {
            // Track which ships were already sunk before this attack
            Set<Ship> previouslySunk = new HashSet<>();
            for (Ship s : defenderBoard.getShips()) {
                if (s.isSunk()) {
                    previouslySunk.add(s);
                }
            }

            CellState outcome = defenderBoard.attack(tile);
            tileResults.add(new AttackResult.TileResult(tile, outcome));

            // Check if any ship just became sunk
            if (outcome == CellState.SUNK) {
                for (Ship s : defenderBoard.getShips()) {
                    if (s.isSunk() && !previouslySunk.contains(s)) {
                        newlySunkShips.add(s);
                    }
                }
            }
        }

        boolean gameOver = defenderBoard.allShipsSunk();

        return new AttackResult(weapon, target,
                tileResults, new ArrayList<>(newlySunkShips), gameOver);
    }
}
