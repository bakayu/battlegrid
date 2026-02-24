package game.server.game;

import java.util.EnumMap;
import java.util.Map;

import game.common.model.Board;
import game.common.model.ShipType;
import game.common.model.WeaponType;

/**
 * Manages weapon cooldowns for a single player.
 * Cooldown value of 0 means ready; >0 means unavailable for that many turns.
 */
public class CooldownManager {

    private final EnumMap<WeaponType, Integer> cooldowns;

    public CooldownManager() {
        this.cooldowns = new EnumMap<>(WeaponType.class);
        for (WeaponType weapon : WeaponType.values()) {
            cooldowns.put(weapon, 0);
        }
    }

    /**
     * Called at the start of a player's turn.
     * Decrements all active cooldowns by 1.
     */
    public void tickCooldowns() {
        for (WeaponType weapon : WeaponType.values()) {
            int current = cooldowns.get(weapon);
            if (current > 0) {
                cooldowns.put(weapon, current - 1);
            }
        }
    }

    /**
     * Activates the cooldown for a weapon after it's been fired.
     * Sets the cooldown to the weapon's defined cooldown duration.
     */
    public void activateCooldown(WeaponType weapon) {
        cooldowns.put(weapon, weapon.getCooldown());
    }

    /**
     * Returns true if the weapon is off cooldown (counter == 0).
     */
    public boolean isReady(WeaponType weapon) {
        return cooldowns.get(weapon) == 0;
    }

    /**
     * Returns the remaining cooldown turns for a weapon.
     */
    public int getRemainingCooldown(WeaponType weapon) {
        return cooldowns.get(weapon);
    }

    /**
     * Returns all cooldown values as a map.
     */
    public Map<WeaponType, Integer> getAllCooldowns() {
        return new EnumMap<>(cooldowns);
    }

    /**
     * Checks if a weapon is available: off cooldown AND the providing ship is
     * alive.
     * Standard Shot is always available if any ship is alive.
     *
     * @param weapon the weapon to check
     * @param board  the player's board (to check ship status)
     * @return true if the weapon can be fired this turn
     */
    public boolean isWeaponAvailable(WeaponType weapon, Board board) {
        // Standard shot is always available as long as any ship lives
        if (weapon == WeaponType.STANDARD_SHOT) {
            return !board.allShipsSunk();
        }

        // Check cooldown
        if (!isReady(weapon)) {
            return false;
        }

        // Check if the providing ship type is still alive
        for (ShipType shipType : ShipType.values()) {
            if (shipType.getWeapon() == weapon) {
                if (board.aliveShipsOfType(shipType) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns a list of all weapons currently available for this player.
     *
     * @param board the player's board
     * @return list of available weapon types
     */
    public java.util.List<WeaponType> getAvailableWeapons(Board board) {
        java.util.List<WeaponType> available = new java.util.ArrayList<>();
        for (WeaponType weapon : WeaponType.values()) {
            if (isWeaponAvailable(weapon, board)) {
                available.add(weapon);
            }
        }
        return available;
    }
}
