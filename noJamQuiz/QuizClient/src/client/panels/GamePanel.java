package client.panels;

import client.QuizClient;
import model.Quiz;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private QuizClient client;
    private JTextArea quizDisplay;
    private JPanel answerPanel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JPanel currentQuizPanel;

    public GamePanel(QuizClient client) {
        this.client = client;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initComponents();
    }

    private void initComponents() {
        // 상단 패널 (타이머, 점수)
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        timerLabel = new JLabel("남은 시간: --", SwingConstants.LEFT);
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        scoreLabel = new JLabel("점수: 0", SwingConstants.RIGHT);
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        topPanel.add(timerLabel, BorderLayout.WEST);
        topPanel.add(scoreLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 문제 표시 영역
        quizDisplay = new JTextArea();
        quizDisplay.setEditable(false);
        quizDisplay.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        quizDisplay.setLineWrap(true);
        quizDisplay.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(quizDisplay);
        scrollPane.setBorder(new CompoundBorder(
                new TitledBorder("문제"),
                new EmptyBorder(5, 5, 5, 5)));
        add(scrollPane, BorderLayout.CENTER);

        // 답안 입력 영역
        answerPanel = new JPanel(new CardLayout());
        currentQuizPanel = new JPanel();  // 기본 패널
        answerPanel.add(currentQuizPanel, "DEFAULT");
        add(answerPanel, BorderLayout.SOUTH);
    }

    public void displayQuiz(Quiz quiz) {
        quizDisplay.setText(quiz.toString());
        updateAnswerPanel(quiz);
    }

    private void updateAnswerPanel(Quiz quiz) {
        currentQuizPanel.removeAll();
        currentQuizPanel = new JPanel();

        switch (quiz.getType()) {
            case OX:
                createOXPanel();
                break;
            case MULTIPLE_CHOICE:
                createMultipleChoicePanel(quiz.getOptions());
                break;
            case SHORT_ANSWER:
                createShortAnswerPanel();
                break;
        }

        answerPanel.add(currentQuizPanel, "QUIZ");
        ((CardLayout)answerPanel.getLayout()).show(answerPanel, "QUIZ");
        answerPanel.revalidate();
        answerPanel.repaint();
    }

    private void createOXPanel() {
        currentQuizPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton oButton = new JButton("O");
        JButton xButton = new JButton("X");

        oButton.setPreferredSize(new Dimension(100, 50));
        xButton.setPreferredSize(new Dimension(100, 50));

        oButton.addActionListener(e -> submitAnswer("O"));
        xButton.addActionListener(e -> submitAnswer("X"));

        currentQuizPanel.add(oButton);
        currentQuizPanel.add(xButton);
    }

    private void createMultipleChoicePanel(String[] options) {
        currentQuizPanel.setLayout(new GridLayout(0, 1, 5, 5));
        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < options.length; i++) {
            JRadioButton radio = new JRadioButton(options[i]);
            final int choiceNum = i + 1;
            radio.addActionListener(e -> submitAnswer(String.valueOf(choiceNum)));
            group.add(radio);
            currentQuizPanel.add(radio);
        }
    }

    private void createShortAnswerPanel() {
        currentQuizPanel.setLayout(new BorderLayout(5, 5));
        JTextField answerField = new JTextField();
        JButton submitButton = new JButton("제출");

        submitButton.addActionListener(e -> {
            submitAnswer(answerField.getText());
            answerField.setText("");
        });

        currentQuizPanel.add(answerField, BorderLayout.CENTER);
        currentQuizPanel.add(submitButton, BorderLayout.EAST);
    }

    private void submitAnswer(String answer) {
        client.sendAnswer(answer);
    }

    public void updateTimer(int seconds) {
        timerLabel.setText("남은 시간: " + seconds + "초");
    }

    public void updateScore(int score) {
        scoreLabel.setText("점수: " + score);
    }
}