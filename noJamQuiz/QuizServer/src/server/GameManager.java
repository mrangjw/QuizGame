package server;

import model.Quiz;
import model.RPS;
import model.QuizDataDAO;
import model.QuestionDTO;
import model.Room;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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

    // RPS 관련 필드
    private Map<String, RPS.Choice> rpsChoices;
    private boolean rpsInProgress;
    private Timer rpsTimer;
    private List<String> tiedPlayers;

    public GameManager(QuizServer server, int roomId, boolean useGPT) {
        this.server = server;
        this.roomId = roomId;
        this.quizList = new ArrayList<>();
        this.playerScores = new HashMap<>();
        this.currentQuizAnswered = new HashMap<>();
        this.currentQuizIndex = 0;
        this.isGameStarted = false;
        this.useGPT = useGPT;
        this.rpsChoices = new HashMap<>();
        this.rpsInProgress = false;

        if (useGPT) {
            this.gptConnector = new GPTConnector();
        }
        initializeQuizzes();
    }

    private void initializeQuizzes() {
        Room room = server.getRoom(roomId);
        if (room == null) return;

        int targetProblemCount = room.getProblemCount();
        int timeLimit = room.getTimeLimit();

        quizList.clear();

        if (useGPT) {
            for (int i = 0; i < targetProblemCount; i++) {
                try {
                    String response = gptConnector.generateQuiz(room.getCategory().getKoreanName());
                    Quiz quiz = gptConnector.parseQuizResponse(response);
                    if (quiz != null) {
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
            defaultQuiz.setPoints(10);
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
                    playerScores.put(playerName, 0);
                }
            }
            sendNextQuiz();
        }
    }

    public void restartGame() {
        stopAllTimers();
        playerScores.clear();
        rpsChoices.clear();
        currentQuizIndex = 0;
        isGameStarted = false;
        rpsInProgress = false;

        initializeQuizzes();
        startGame();
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

    private void checkForTie() {
        int maxScore = playerScores.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        tiedPlayers = playerScores.entrySet().stream()
                .filter(e -> e.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (tiedPlayers.size() > 1) {
            startRPSGame();
        } else {
            sendFinalResults();
        }
    }

    private void startRPSGame() {
        rpsInProgress = true;
        rpsChoices.clear();
        String tiedPlayerList = String.join(",", tiedPlayers);
        server.broadcastToRoom(roomId, "START_RPS:" + tiedPlayerList);
        startRPSTimer();
        updateRPSStatus();
    }

    private void startRPSTimer() {
        if (rpsTimer != null) {
            rpsTimer.cancel();
        }

        rpsTimer = new Timer();
        rpsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                determineRPSWinner();
            }
        }, 10000);
    }

    private void updateRPSStatus() {
        StringBuilder status = new StringBuilder("RPS_STATUS:");
        for (String player : tiedPlayers) {
            status.append(player)
                    .append(":")
                    .append(rpsChoices.containsKey(player) ? "READY" : "WAITING")
                    .append(";");
        }
        server.broadcastToRoom(roomId, status.toString());
    }

    public void handleRPSChoice(String playerName, RPS.Choice choice) {
        if (rpsInProgress && tiedPlayers.contains(playerName)) {
            rpsChoices.put(playerName, choice);
            updateRPSStatus();

            if (rpsChoices.size() == tiedPlayers.size()) {
                rpsTimer.cancel();
                determineRPSWinner();
            }
        }
    }

    private void determineRPSWinner() {
        for (String player : tiedPlayers) {
            if (!rpsChoices.containsKey(player)) {
                RPS.Choice[] choices = RPS.Choice.values();
                rpsChoices.put(player, choices[new Random().nextInt(choices.length)]);
            }
        }

        List<String> winners = new ArrayList<>(tiedPlayers);
        for (int i = 0; i < tiedPlayers.size(); i++) {
            for (int j = i + 1; j < tiedPlayers.size(); j++) {
                String player1 = tiedPlayers.get(i);
                String player2 = tiedPlayers.get(j);
                RPS.Choice choice1 = rpsChoices.get(player1);
                RPS.Choice choice2 = rpsChoices.get(player2);

                if (choice1.beats(choice2)) {
                    winners.remove(player2);
                } else if (choice2.beats(choice1)) {
                    winners.remove(player1);
                }
            }
        }

        StringBuilder result = new StringBuilder("RPS_RESULT:");
        for (String player : tiedPlayers) {
            result.append(player).append("->").append(rpsChoices.get(player).getKoreanName()).append(", ");
        }
        result.append("\n");

        if (winners.size() == 1) {
            result.append("승자: ").append(winners.get(0));
            tiedPlayers = winners;
            sendFinalResults();
        } else {
            result.append("무승부! 다시 시작합니다.");
            startRPSGame();
        }

        server.broadcastToRoom(roomId, result.toString());
    }

    private void sendFinalResults() {
        rpsInProgress = false;
        StringBuilder resultMessage = new StringBuilder("GAME_RESULT:");
        List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>(playerScores.entrySet());
        sortedScores.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        for (Map.Entry<String, Integer> entry : sortedScores) {
            resultMessage.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue())
                    .append(";");
        }

        server.broadcastToRoom(roomId, resultMessage.toString());
    }

    public void playerLeft(String playerName) {
        if (playerScores.containsKey(playerName)) {
            int finalScore = playerScores.get(playerName);
            server.broadcastToRoom(roomId,
                    String.format("%s님이 게임을 중단하셨습니다. (최종 점수: %d점)",
                            playerName, finalScore));
        }

        playerScores.remove(playerName);
        currentQuizAnswered.remove(playerName);

        if (rpsInProgress) {
            rpsChoices.remove(playerName);
            tiedPlayers.remove(playerName);
            if (tiedPlayers.size() <= 1) {
                if (rpsTimer != null) {
                    rpsTimer.cancel();
                }
                sendFinalResults();
            } else {
                updateRPSStatus();
            }
        }

        Room room = server.getRoom(roomId);
        if (room != null && room.getPlayers().size() <= 1) {
            endGame();
        } else if (isGameStarted && allPlayersAnswered()) {
            if (quizTimer != null) {
                quizTimer.cancel();
            }
            currentQuizIndex++;
            sendNextQuiz();
        }
    }

    public void stopAllTimers() {
        if (quizTimer != null) {
            quizTimer.cancel();
            quizTimer = null;
        }
        if (rpsTimer != null) {
            rpsTimer.cancel();
            rpsTimer = null;
        }
    }

    public void endGame() {
        isGameStarted = false;
        stopAllTimers();
        checkForTie();
    }

    public void terminateGame() {
        stopAllTimers();
        playerScores.clear();
        rpsChoices.clear();
        isGameStarted = false;
        rpsInProgress = false;
    }

    public boolean isGameInProgress() {
        return isGameStarted;
    }
}