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
        this.currentQuizIndex = 0;
        this.isGameStarted = false;
        this.useGPT = useGPT;
        if (useGPT) {
            this.gptConnector = new GPTConnector();
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
            String response = gptConnector.generateQuiz(room.getCategory().getKoreanName());
            Quiz quiz = gptConnector.parseQuizResponse(response);
            if (quiz != null) {
                quiz.setTimeLimit(20);
                quiz.setPoints(10);
                quizList.add(quiz);
                server.printDisplay("GPT 퀴즈 생성 완료");
            } else {
                server.printDisplay("GPT 퀴즈 생성 실패. 파일 퀴즈로 전환합니다.");
                initializeFileQuizzes();
            }
        } catch (Exception e) {
            server.printDisplay("GPT 퀴즈 생성 실패: " + e.getMessage());
            initializeFileQuizzes();
        }
    }

    private void initializeFileQuizzes() {
        QuizDataDAO quizData = new QuizDataDAO();

        // 현재 작업 디렉토리 출력
        String currentPath = System.getProperty("user.dir");
        server.printDisplay("현재 작업 디렉토리: " + currentPath);

        // 파일 존재 여부 확인
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
                quiz.setTimeLimit(20);
                quiz.setPoints(10);
                quizList.add(quiz);
            }

            Collections.shuffle(quizList);
            server.printDisplay("파일 퀴즈 데이터 로드 완료: " + quizList.size() + "개의 문제");
        } else {
            server.printDisplay("퀴즈 파일 로드 실패. 기본 퀴즈를 사용합니다.");
            Quiz defaultQuiz = new Quiz(
                    "대한민국의 수도는?",
                    "서울",
                    Quiz.QuizType.SHORT_ANSWER,
                    "기본"
            );
            quizList.add(defaultQuiz);
        }
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
        Quiz currentQuiz = quizList.get(currentQuizIndex);
        server.broadcastToRoom(roomId, "시간 종료! 정답은 '" + currentQuiz.getAnswer() + "' 입니다.");
        currentQuizIndex++;
        sendNextQuiz();
    }

    public void handleAnswer(String playerName, String answer) {
        if (currentQuizIndex >= quizList.size()) return;

        Quiz currentQuiz = quizList.get(currentQuizIndex);
        if (currentQuiz.checkAnswer(answer)) {
            int score = currentQuiz.calculateScore(remainingTime);
            playerScores.merge(playerName, score, Integer::sum);
            server.broadcastToRoom(roomId,
                    String.format("%s님 정답! (%d점)", playerName, score));

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
