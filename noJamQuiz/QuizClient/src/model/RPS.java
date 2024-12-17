package model;

public class RPS {
    public enum Choice {
        ROCK("바위", "rps_rock.png"),
        PAPER("보", "rps_paper.png"),
        SCISSORS("가위", "rps_scissors.png");

        private final String koreanName;
        private final String imagePath;

        Choice(String koreanName, String imagePath) {
            this.koreanName = koreanName;
            this.imagePath = imagePath;
        }

        public String getKoreanName() {
            return koreanName;
        }

        public String getImagePath() {
            return imagePath;
        }

        public boolean beats(Choice other) {
            return (this == ROCK && other == SCISSORS) ||
                    (this == SCISSORS && other == PAPER) ||
                    (this == PAPER && other == ROCK);
        }
    }

    private String playerName;
    private Choice choice;
    private boolean ready;

    public RPS(String playerName) {
        this.playerName = playerName;
        this.ready = false;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Choice getChoice() {
        return choice;
    }

    public void setChoice(Choice choice) {
        this.choice = choice;
        this.ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    public void reset() {
        this.choice = null;
        this.ready = false;
    }
}