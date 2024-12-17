package server;

import model.Quiz;
import model.QuizDataDAO;
import model.QuestionDTO;
import model.Room;

import java.io.File;
import java.util.*;

public class GameManager {
    private QuizServer server;
    private int roomId;
    private List<Quiz> quizList;
    private int currentQuizIndex;
    private Map<String, Integer> playerScores;
    private Map<String, Boolean> currentQuizAnswered;
    private Timer quizTimer;
    private boolean isGameStarted;
    private int remainingTime;
    private GPTConnector gptConnector;
    private boolean useGPT;

    public GameManager(QuizServer server, int roomId, boolean useGPT) {
        this.server = server;
        this.roomId = roomId;
        this.quizList = new ArrayList<>();
        this.playerScores = new HashMap<>();
        this.currentQuizAnswered = new HashMap<>();
        this.currentQuizIndex = 0;
        this.isGameStarted = false;
        this.useGPT = useGPT;
        if (useGPT) {
            this.gptConnector = new GPTConnector();
        }
        initializeQuizzes();
    }

    private void initializeQuizzes() {
        Room room = server.getRoom(roomId);
        int targetProblemCount = room.getProblemCount();
        int timeLimit = room.getTimeLimit();

        if (useGPT) {
            for (int i = 0; i < targetProblemCount; i++) {
                try {
                    String response = gptConnector.generateQuiz(room.getCategory().getKoreanName());
                    Quiz quiz = gptConnector.parseQuizResponse(response);
                    if (quiz != null) {
                        // 방에서 설정한 제한시간을 적용
                        quiz.setTimeLimit(timeLimit);
                        quiz.setPoints(10);
                        quizList.add(quiz);
                        server.printDisplay("GPT 퀴즈 생성 완료 - 제한시간: " + timeLimit + "초");
                    }
                } catch (Exception e) {
                    server.printDisplay("GPT 퀴즈 생성 실패: " + e.getMessage());
                }
            }

            if (quizList.isEmpty()) {
                server.printDisplay("GPT 퀴즈 생성 실패. 파일 퀴즈로 전환합니다.");
                initializeFileQuizzes(targetProblemCount, timeLimit);
            }
        } else {
            initializeFileQuizzes(targetProblemCount, timeLimit);
        }

        if (!quizList.isEmpty()) {
            server.printDisplay("총 " + quizList.size() + "개의 문제가 준비되었습니다. 제한시간: " + timeLimit + "초");
        }
    }

    private void initializeFileQuizzes(int targetProblemCount, int timeLimit) {
        QuizDataDAO quizData = new QuizDataDAO();
        boolean loadError = quizData.loadQuiz(1);

        if (!loadError) {
            Collections.shuffle(quizData);
            for (int i = 0; i < Math.min(targetProblemCount, quizData.size()); i++) {
                QuestionDTO questionDTO = quizData.get(i);
                Quiz quiz = new Quiz(
                        questionDTO.getQuestion(),
                        questionDTO.getAnswer(),
                        Quiz.QuizType.SHORT_ANSWER,
                        "일반상식"
                );
                quiz.setTimeLimit(timeLimit);
                quiz.setPoints(10);
                quizList.add(quiz);
            }
            server.printDisplay("파일 퀴즈 데이터 로드 완료: " + quizList.size() + "개의 문제");
        } else {
            server.printDisplay("퀴즈 파일 로드 실패. 기본 퀴즈를 사용합니다.");
            Quiz defaultQuiz = new Quiz(
                    "대한민국의 수도는?",
                    "서울",
                    Quiz.QuizType.SHORT_ANSWER,
                    "기본"
            );
            defaultQuiz.setTimeLimit(timeLimit);
            quizList.add(defaultQuiz);
        }
    }

    public void startGame() {
        if (!isGameStarted && !quizList.isEmpty()) {
            isGameStarted = true;
            playerScores.clear();
            currentQuizIndex = 0;
            resetCurrentQuizAnswered();
            Room room = server.getRoom(roomId);
            if (room != null) {
                for (String playerName : room.getPlayers()) {
                    server.broadcastToRoom(roomId, "SCORE:" + playerName + ":0");
                }
            }
            sendNextQuiz();
        }
    }

    private void resetCurrentQuizAnswered() {
        currentQuizAnswered.clear();
        Room room = server.getRoom(roomId);
        if (room != null) {
            for (String playerName : room.getPlayers()) {
                currentQuizAnswered.put(playerName, false);
            }
        }
    }

    private void sendNextQuiz() {
        if (currentQuizIndex < quizList.size()) {
            Quiz currentQuiz = quizList.get(currentQuizIndex);
            resetCurrentQuizAnswered();
            server.broadcastToRoom(roomId, "QUIZ:" + currentQuiz.toString());
            startQuizTimer(currentQuiz.getTimeLimit());
        } else {
            endGame();
        }
    }

    public void handleAnswer(String playerName, String answer) {
        if (currentQuizIndex >= quizList.size() || !isGameStarted) return;
        if (currentQuizAnswered.getOrDefault(playerName, false)) return;

        Quiz currentQuiz = quizList.get(currentQuizIndex);
        currentQuizAnswered.put(playerName, true);

        if (currentQuiz.checkAnswer(answer)) {
            int score = currentQuiz.calculateScore(remainingTime);
            playerScores.merge(playerName, score, Integer::sum);
            server.broadcastToRoom(roomId, playerName + "님 정답입니다.");
            server.broadcastToRoom(roomId, "SCORE:" + playerName + ":" + playerScores.get(playerName));

            if (allPlayersAnswered()) {
                quizTimer.cancel();
                currentQuizIndex++;
                sendNextQuiz();
            }
        } else {
            server.broadcastToRoom(roomId, playerName + "님 오답입니다.");
        }
    }

    private boolean allPlayersAnswered() {
        return currentQuizAnswered.values().stream().allMatch(answered -> answered);
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
        Quiz currentQuiz = quizList.get(currentQuizIndex);

        List<String> correctPlayers = new ArrayList<>();
        List<String> incorrectPlayers = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : currentQuizAnswered.entrySet()) {
            if (!entry.getValue()) {
                incorrectPlayers.add(entry.getKey());
            } else if (playerScores.containsKey(entry.getKey())) {
                correctPlayers.add(entry.getKey());
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("시간이 종료되었습니다.\n");
        result.append("정답: ").append(currentQuiz.getAnswer()).append("\n");

        if (correctPlayers.isEmpty()) {
            result.append("아쉽게도 이번 문제는 정답자가 없습니다.");
        } else {
            result.append("정답자: ").append(String.join(", ", correctPlayers));
        }

        if (!incorrectPlayers.isEmpty()) {
            result.append("\n미응답: ").append(String.join(", ", incorrectPlayers));
        }

        server.broadcastToRoom(roomId, result.toString());
        currentQuizIndex++;
        sendNextQuiz();
    }

    public void playerLeft(String playerName) {
        if (playerScores.containsKey(playerName)) {
            int finalScore = playerScores.get(playerName);
            server.broadcastToRoom(roomId,
                    String.format("%s님이 게임을 중단하셨습니다. (최종 점수: %d점)",
                            playerName, finalScore));
        }

        currentQuizAnswered.remove(playerName);

        Room room = server.getRoom(roomId);
        if (room != null && room.getPlayers().size() <= 1) {
            endGame();
        } else if (isGameStarted && allPlayersAnswered()) {
            quizTimer.cancel();
            currentQuizIndex++;
            sendNextQuiz();
        }
    }

    public void endGame() {
        isGameStarted = false;
        if (quizTimer != null) {
            quizTimer.cancel();
        }

        List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>(playerScores.entrySet());
        sortedScores.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        StringBuilder result = new StringBuilder("게임이 종료되었습니다!\n최종 결과:\n");
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