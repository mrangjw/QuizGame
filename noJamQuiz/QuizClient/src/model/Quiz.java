package model;

public class Quiz {
    private String question;    // 문제
    private String answer;      // 정답
    private String[] options;   // 객관식 보기
    private QuizType type;      // 문제 유형
    private int points;         // 문제 배점
    private int timeLimit;      // 제한 시간(초)
    private String category;    // 문제 카테고리

    public enum QuizType {
        OX("O/X"),
        MULTIPLE_CHOICE("객관식"),
        SHORT_ANSWER("단답형");

        private final String koreanName;

        QuizType(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }

    public Quiz(String question, String answer, QuizType type, String category) {
        this.question = question;
        this.answer = answer;
        this.type = type;
        this.category = category;
        this.points = 10;       // 기본 배점
        this.timeLimit = 30;    // 기본 제한시간
    }

    public Quiz(String question, String answer, String[] options, QuizType type, String category) {
        this(question, answer, type, category);
        this.options = options;
    }

    public boolean checkAnswer(String userAnswer) {
        if (type == QuizType.OX) {
            return answer.equalsIgnoreCase(userAnswer);
        } else if (type == QuizType.MULTIPLE_CHOICE) {
            try {
                int idx = Integer.parseInt(userAnswer) - 1;
                return answer.equals(options[idx]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return false;
            }
        } else {  // SHORT_ANSWER
            return answer.trim().equalsIgnoreCase(userAnswer.trim());
        }
    }

    public int calculateScore(int remainingTime) {
        double timeBonus = (double) remainingTime / timeLimit;
        return (int) (points * (1 + timeBonus));
    }

    // Getters
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String[] getOptions() { return options; }
    public QuizType getType() { return type; }
    public int getPoints() { return points; }
    public int getTimeLimit() { return timeLimit; }
    public String getCategory() { return category; }

    // Setters
    public void setPoints(int points) { this.points = points; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type.getKoreanName()).append("] ");
        sb.append("[").append(category).append("] ");
        sb.append(question);
        sb.append(" (").append(points).append("점)");
        sb.append(" [제한시간: ").append(timeLimit).append("초]");

        if (options != null) {
            sb.append("\n보기:");
            for (int i = 0; i < options.length; i++) {
                sb.append("\n").append(i + 1).append(". ").append(options[i]);
            }
        }
        return sb.toString();
    }
}