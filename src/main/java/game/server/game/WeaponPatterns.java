package game.server.game;

import java.util.ArrayList;
import java.util.List;

import game.common.model.Coordinate;
import game.common.model.Direction;
import game.common.model.WeaponType;

/**
 * Computes the set of tiles affected by each weapon type.
 * Tiles that fall outside the grid are clipped.
 */
public class WeaponPatterns {

    private WeaponPatterns() {
    }

    /**
     * Returns the list of coordinates affected by firing the given weapon
     * at the target coordinate.
     *
     * @param weapon   the weapon being fired
     * @param target   the center/origin coordinate
     * @param dir      direction (only relevant for LINE_BARRAGE)
     * @param gridSize the board dimension (for bounds clipping)
     * @return list of valid in-bounds coordinates that are affected
     */
    public static List<Coordinate> getAffectedTiles(
            WeaponType weapon, Coordinate target, Direction dir, int gridSize) {

        List<Coordinate> tiles = new ArrayList<>();
        int r = target.row();
        int c = target.col();

        switch (weapon) {
            case STANDARD_SHOT -> {
                tiles.add(target);
            }

            case LINE_BARRAGE -> {
                // 3 tiles in a line centered on target
                if (dir == Direction.HORIZONTAL) {
                    for (int dc = -1; dc <= 1; dc++) {
                        tiles.add(new Coordinate(r, c + dc));
                    }
                } else {
                    for (int dr = -1; dr <= 1; dr++) {
                        tiles.add(new Coordinate(r + dr, c));
                    }
                }
            }

            case CROSS_BOMBER -> {
                // Plus shape: center + 4 cardinal neighbors
                tiles.add(new Coordinate(r, c)); // center
                tiles.add(new Coordinate(r - 1, c)); // up
                tiles.add(new Coordinate(r + 1, c)); // down
                tiles.add(new Coordinate(r, c - 1)); // left
                tiles.add(new Coordinate(r, c + 1)); // right
            }

            case NUKE -> {
                // 3×3 square centered on target
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        tiles.add(new Coordinate(r + dr, c + dc));
                    }
                }
            }
        }

        // Clip out-of-bounds tiles
        return tiles.stream()
                .filter(coord -> coord.isWithinBounds(gridSize))
                .toList();
    }
}
