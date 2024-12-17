package model;

public enum RPS {
    ROCK("바위"),
    PAPER("보"),
    SCISSORS("가위");

    private final String korean;

    RPS(String korean) {
        this.korean = korean;
    }

    public String getKorean() {
        return korean;
    }

    public static RPS getRPS(String korean) {
        for (RPS rps : RPS.values()) {
            if (rps.korean.equals(korean)) {
                return rps;
            }
        }
        return null;
    }

    // 가위바위보 승패 판정
    public boolean beats(RPS other) {
        return (this == ROCK && other == SCISSORS) ||
                (this == SCISSORS && other == PAPER) ||
                (this == PAPER && other == ROCK);
    }
}