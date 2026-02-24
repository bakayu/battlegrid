package game.server.game;

import game.common.model.Board;

/**
 * Holds all server-side state for a single player in a game.
 */
public class PlayerState {

    private final String username;
    private final Board board;
    private final CooldownManager cooldownManager;

    // Stats
    private int shotsFired;
    private int shotsHit;
    private int turnsTaken;

    public PlayerState(String username, Board board) {
        this.username = username;
        this.board = board;
        this.cooldownManager = new CooldownManager();
        this.shotsFired = 0;
        this.shotsHit = 0;
        this.turnsTaken = 0;
    }

    public String getUsername() {
        return username;
    }

    public Board getBoard() {
        return board;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public int getShotsFired() {
        return shotsFired;
    }

    public int getShotsHit() {
        return shotsHit;
    }

    public int getTurnsTaken() {
        return turnsTaken;
    }

    public void recordShots(int fired, int hit) {
        this.shotsFired += fired;
        this.shotsHit += hit;
    }

    public void incrementTurns() {
        this.turnsTaken++;
    }

    public double getHitRate() {
        if (shotsFired == 0)
            return 0.0;
        return (double) shotsHit / shotsFired * 100.0;
    }

    public int shipsLost() {
        return (int) board.getShips().stream().filter(s -> s.isSunk()).count();
    }

    public int totalShips() {
        return board.getShips().size();
    }
}
