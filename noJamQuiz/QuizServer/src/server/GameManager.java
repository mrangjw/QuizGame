package server;

import model.Quiz;
import java.util.*;

public class GameManager {
    private QuizServer server;
    private int roomId;
    private List<Quiz> quizList;
    private int currentQuizIndex;
    private Map<String, Integer> playerScores;  // 플레이어별 점수
    private Timer quizTimer;
    private boolean isGameStarted;
    private int remainingTime;

    public GameManager(QuizServer server, int roomId) {
        this.server = server;
        this.roomId = roomId;
        this.quizList = new ArrayList<>();
        this.playerScores = new HashMap<>();
        this.currentQuizIndex = 0;
        this.isGameStarted = false;
        initializeQuizzes();
    }

    private void initializeQuizzes() {
        // 임시 퀴즈 데이터
        quizList.add(new Quiz("1+1=2 입니까?", "O", Quiz.QuizType.OX, "수학"));
        quizList.add(new Quiz("대한민국의 수도는?", "서울",
                new String[]{"서울", "부산", "대구", "인천"},
                Quiz.QuizType.MULTIPLE_CHOICE, "상식"));
        // 추후 ChatGPT API로 대체 예정
    }

    public void startGame() {
        if (!isGameStarted && !quizList.isEmpty()) {
            isGameStarted = true;
            playerScores.clear();
            currentQuizIndex = 0;
            sendNextQuiz();
        }
    }

    private void sendNextQuiz() {
        if (currentQuizIndex < quizList.size()) {
            Quiz currentQuiz = quizList.get(currentQuizIndex);
            server.broadcastToRoom(roomId, "QUIZ:" + currentQuiz.toString());
            startQuizTimer(currentQuiz.getTimeLimit());
        } else {
            endGame();
        }
    }

    private void startQuizTimer(int seconds) {
        if (quizTimer != null) {
            quizTimer.cancel();
        }
        remainingTime = seconds;
        quizTimer = new Timer();
        quizTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (--remainingTime <= 0) {
                    timeUp();
                } else {
                    server.broadcastToRoom(roomId, "TIME:" + remainingTime);
                }
            }
        }, 0, 1000);
    }

    private void timeUp() {
        quizTimer.cancel();
        server.broadcastToRoom(roomId, "시간 종료!");
        currentQuizIndex++;
        sendNextQuiz();
    }

    public void handleAnswer(String playerName, String answer) {
        Quiz currentQuiz = quizList.get(currentQuizIndex);
        if (currentQuiz.checkAnswer(answer)) {
            int score = currentQuiz.calculateScore(remainingTime);
            playerScores.merge(playerName, score, Integer::sum);
            server.broadcastToRoom(roomId,
                    String.format("%s님 정답! (%d점)", playerName, score));
        }
    }

    public void endGame() {  // private를 public으로 변경
        isGameStarted = false;
        if (quizTimer != null) {
            quizTimer.cancel();
        }

        // 결과 정렬 및 전송
        List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>(playerScores.entrySet());
        sortedScores.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        StringBuilder result = new StringBuilder("게임 종료!\n최종 결과:\n");
        for (Map.Entry<String, Integer> entry : sortedScores) {
            result.append(String.format("%s: %d점\n",
                    entry.getKey(), entry.getValue()));
        }
        server.broadcastToRoom(roomId, result.toString());
    }

    public boolean isGameInProgress() {
        return isGameStarted;
    }
}