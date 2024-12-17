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
            server.printDisplay("GPT 모드로 게임을 시작합니다.");
            server.broadcastToRoom(roomId, "GPT 모드로 게임이 시작됩니다. 잠시만 기다려주세요...");
        } else {
            server.printDisplay("일반 모드로 게임을 시작합니다.");
            server.broadcastToRoom(roomId, "일반 모드로 게임이 시작됩니다.");
        }
        initializeQuizzes();
    }

    private void initializeQuizzes() {
        if (useGPT) {
            initializeGPTQuizzes();
        } else {
            initializeFileQuizzes();
        }
    }

    private void initializeGPTQuizzes() {
        Room room = server.getRoom(roomId);
        try {
            int targetQuizCount = room.getQuestionCount();
            server.printDisplay("GPT를 통해 " + targetQuizCount + "개의 퀴즈를 생성합니다.");
            server.broadcastToRoom(roomId, "GPT를 통해 퀴즈를 생성하고 있습니다. 잠시만 기다려주세요...");

            for (int i = 0; i < targetQuizCount; i++) {
                String response = gptConnector.generateQuiz(room.getCategory().getKoreanName());
                if (response == null) {
                    throw new Exception("GPT 응답이 null입니다.");
                }

                Quiz quiz = gptConnector.parseQuizResponse(response);
                if (quiz == null) {
                    throw new Exception("퀴즈 파싱에 실패했습니다.");
                }

                quiz.setTimeLimit(room.getTimePerQuestion());
                quiz.setPoints(10);
                quizList.add(quiz);

                server.printDisplay("GPT 퀴즈 생성 완료 (" + (i + 1) + "/" + targetQuizCount + ")");
                server.broadcastToRoom(roomId, "퀴즈 생성중... (" + (i + 1) + "/" + targetQuizCount + ")");

                // API 호출 간격 조절
                Thread.sleep(2000);
            }

            server.printDisplay("GPT 퀴즈 생성이 모두 완료되었습니다. 총 " + quizList.size() + "개의 문제");
            server.broadcastToRoom(roomId, "모든 퀴즈가 준비되었습니다. 게임을 시작합니다!");

        } catch (Exception e) {
            server.printDisplay("GPT 퀴즈 생성 실패: " + e.getMessage());
            if (!quizList.isEmpty()) {
                // 이미 생성된 퀴즈가 있다면 그것으로 진행
                server.printDisplay("생성된 " + quizList.size() + "개의 퀴즈로 진행합니다.");
                server.broadcastToRoom(roomId, "퀴즈 생성이 일부 완료되었습니다. " + quizList.size() + "개의 문제로 진행합니다.");
            } else {
                // 아예 실패한 경우에만 일반 모드로 전환
                server.printDisplay("GPT 퀴즈 생성에 완전히 실패했습니다. 일반 모드로 전환합니다.");
                server.broadcastToRoom(roomId, "GPT 퀴즈 생성에 실패했습니다. 일반 모드로 전환됩니다.");
                initializeFileQuizzes();
            }
        }
    }

    private void initializeFileQuizzes() {
        Room room = server.getRoom(roomId);
        QuizDataDAO quizData = new QuizDataDAO();

        String currentPath = System.getProperty("user.dir");
        server.printDisplay("현재 작업 디렉토리: " + currentPath);

        File quizFile = new File("Data/Quiz1.dat");
        server.printDisplay("Quiz1.dat 파일 경로: " + quizFile.getAbsolutePath());
        server.printDisplay("파일 존재 여부: " + quizFile.exists());

        boolean loadError = quizData.loadQuiz(1);

        if (!loadError) {
            for (QuestionDTO questionDTO : quizData) {
                Quiz quiz = new Quiz(
                        questionDTO.getQuestion(),
                        questionDTO.getAnswer(),
                        Quiz.QuizType.SHORT_ANSWER,
                        "일반상식"
                );
                quiz.setTimeLimit(room.getTimePerQuestion());
                quiz.setPoints(10);
                quizList.add(quiz);
            }

            Collections.shuffle(quizList);
            while (quizList.size() > room.getQuestionCount()) {
                quizList.remove(quizList.size() - 1);
            }

            server.printDisplay("파일 퀴즈 데이터 로드 완료: " + quizList.size() + "개의 문제");
            server.broadcastToRoom(roomId, "퀴즈 준비가 완료되었습니다!");
        } else {
            server.printDisplay("퀴즈 파일 로드 실패. 기본 퀴즈를 사용합니다.");
            Quiz defaultQuiz = new Quiz(
                    "대한민국의 수도는?",
                    "서울",
                    Quiz.QuizType.SHORT_ANSWER,
                    "기본"
            );
            defaultQuiz.setTimeLimit(room.getTimePerQuestion());
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
                // 모든 플레이어의 초기 점수를 0으로 설정
                for (String playerName : room.getPlayers()) {
                    server.broadcastToRoom(roomId, "SCORE:" + playerName + ":0");
                }
                server.broadcastToRoom(roomId, "게임을 시작합니다!");
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

            server.broadcastToRoom(roomId, String.format("\n===== 문제 %d/%d =====",
                    currentQuizIndex + 1, quizList.size()));
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
            int score = currentQuiz.getPoints();
            playerScores.merge(playerName, score, Integer::sum);
            server.broadcastToRoom(roomId, playerName + "님 정답입니다!");
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

        Map<Integer, List<String>> scoreGroups = new HashMap<>();
        for (Map.Entry<String, Integer> entry : sortedScores) {
            scoreGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        int topScore = sortedScores.isEmpty() ? 0 : sortedScores.get(0).getValue();
        List<String> topPlayers = scoreGroups.get(topScore);

        if (topPlayers != null && topPlayers.size() > 1) {
            server.broadcastToRoom(roomId, "동점자가 발생했습니다! 가위바위보로 승자를 결정합니다.");
            server.broadcastToRoom(roomId, "RPS_START:" + String.join(",", topPlayers));
        } else {
            sendFinalResults(sortedScores, false);
        }
    }

    private void sendFinalResults(List<Map.Entry<String, Integer>> sortedScores, boolean rpsDecided) {
        StringBuilder result = new StringBuilder("GAME_END:");
        if (rpsDecided) {
            result.append("RPS_DECIDED:");
        }

        for (Map.Entry<String, Integer> entry : sortedScores) {
            result.append(entry.getKey()).append(",")
                    .append(entry.getValue()).append(";");
        }

        server.broadcastToRoom(roomId, result.toString());
    }

    public void handleRPSResult(String winner, List<String> players) {
        for (String player : players) {
            if (player.equals(winner)) {
                playerScores.put(player, playerScores.get(player) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>(playerScores.entrySet());
        sortedScores.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        sendFinalResults(sortedScores, true);
    }

    public boolean isGameInProgress() {
        return isGameStarted;
    }
}