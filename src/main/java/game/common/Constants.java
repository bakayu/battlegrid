package game.common;

/**
 * Shared constants for the BattleGrid protocol.
 */
public final class Constants {

    private Constants() {
    }

    // --- Server Config ---
    public static final int SERVER_PORT = 8025;
    public static final String WEBSOCKET_PATH = "/websockets/battlegrid";
    public static final String DEFAULT_USERNAME = "Player";
    public static final int TURN_TIMEOUT_SECONDS = 60;

    // --- Message Types: Server → Client ---
    public static final String MSG_HANDSHAKE_RSA_KEY = "handshake_rsa_key";
    public static final String MSG_HANDSHAKE_COMPLETE = "handshake_complete";
    public static final String MSG_LOBBY_WAITING = "lobby_waiting";
    public static final String MSG_LOBBY_MODE_SELECT = "lobby_mode_select";
    public static final String MSG_GAME_START = "game_start";
    public static final String MSG_YOUR_TURN = "your_turn";
    public static final String MSG_WAIT_TURN = "wait_turn";
    public static final String MSG_ATTACK_RESULT = "attack_result";
    public static final String MSG_INCOMING_ATTACK = "incoming_attack";
    public static final String MSG_GAME_OVER = "game_over";
    public static final String MSG_ERROR = "error";
    public static final String MSG_OPPONENT_DISCONNECTED = "opponent_disconnected";
    public static final String MSG_PLAY_AGAIN_PROMPT = "play_again_prompt";
    public static final String MSG_PLAY_AGAIN_WAITING = "play_again_waiting";

    // --- Message Types: Client → Server ---
    public static final String MSG_SELECT_MODE = "select_mode";
    public static final String MSG_ATTACK = "attack";
    public static final String MSG_FORFEIT = "forfeit";
    public static final String MSG_PLAY_AGAIN = "play_again";

    // --- Message Log ---
    public static final int MAX_RECENT_MESSAGES = 5;
}
