package model;


import java.util.Vector;

public class Room {
    private int roomId;
    private String roomName;
    private String hostName;
    private int maxPlayers;
    private Vector<String> players;
    private QuizCategory category;
    private boolean isGameStarted;

    public enum QuizCategory {
        TOTAL("통합"),
        ECONOMY("경제"),
        SOCIETY("사회"),
        NONSENSE("넌센스");

        private final String koreanName;

        QuizCategory(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }

        public static QuizCategory fromKoreanName(String koreanName) {
            for (QuizCategory category : QuizCategory.values()) {
                if (category.getKoreanName().equals(koreanName)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid category name: " + koreanName);
        }
    }

    public Room(int roomId, String roomName, String hostName, int maxPlayers, QuizCategory category) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostName = hostName;
        this.maxPlayers = maxPlayers;
        this.category = category;
        this.players = new Vector<>();
        this.players.add(hostName);
        this.isGameStarted = false;
    }

    // Getters and Setters
    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getHostName() { return hostName; }
    public int getMaxPlayers() { return maxPlayers; }
    public Vector<String> getPlayers() { return players; }
    public QuizCategory getCategory() { return category; }
    public boolean isGameStarted() { return isGameStarted; }
    public void setGameStarted(boolean gameStarted) { isGameStarted = gameStarted; }

    public boolean addPlayer(String playerName) {
        if (players.size() < maxPlayers && !players.contains(playerName)) {
            players.add(playerName);
            return true;
        }
        return false;
    }

    public boolean removePlayer(String playerName) {
        return players.remove(playerName);
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
}
