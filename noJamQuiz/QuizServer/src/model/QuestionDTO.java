package model;

// QuestionDTO.java
// 하나의 문제와 그에 대한 답을 저장하는 클래스
// 데이터 전송을 목적으로 사용하는 객체
public class QuestionDTO {
    private String question; // 문제를 저장할 변수
    private String answer;   // 답을 저장할 변수

    // 기본 생성자
    public QuestionDTO() { }

    // 문제와 답을 초기화하는 생성자
    public QuestionDTO(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    // 문제를 반환하는 getter 메서드
    public String getQuestion() {
        return question;
    }

    // 답을 반환하는 getter 메서드
    public String getAnswer() {
        return answer;
    }

    // 객체의 문자열 표현을 반환하는 메서드
    @Override
    public String toString() {
        return "Question [question=" + question + ", answer=" + answer + "]";
    }
}