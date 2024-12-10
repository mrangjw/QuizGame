package model;

public class Quiz {
    private String question;    // 문제
    private String answer;      // 정답
    private String[] options;   // 객관식 보기
    private QuizType type;      // 문제 유형

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

    public Quiz(String question, String answer, QuizType type) {
        this.question = question;
        this.answer = answer;
        this.type = type;
    }

    public Quiz(String question, String answer, String[] options, QuizType type) {
        this(question, answer, type);
        this.options = options;
    }

    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String[] getOptions() { return options; }
    public QuizType getType() { return type; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type.getKoreanName()).append("] ");
        sb.append(question);

        if (options != null) {
            sb.append("\n보기:");
            for (int i = 0; i < options.length; i++) {
                sb.append("\n").append(i + 1).append(". ").append(options[i]);
            }
        }
        return sb.toString();
    }
}