package client.panels;

import client.QuizClient;
import model.Quiz;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private QuizClient client;
    private JTextArea quizDisplay;
    private JTextArea answerDisplay;
    private JPanel answerPanel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JPanel currentQuizPanel;
    private JTextField answerField;

    // 커스텀 색상 정의
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_COLOR = new Color(176, 196, 222);
    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255);
    private static final Color TEXT_COLOR = new Color(25, 25, 25);

    public GamePanel(QuizClient client) {
        this.client = client;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BACKGROUND_COLOR);
        initComponents();
    }

    private void initComponents() {
        // 상단 정보 패널 (타이머, 점수)
        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBackground(BACKGROUND_COLOR);
        infoPanel.setBorder(new EmptyBorder(0, 5, 10, 5));

        timerLabel = new JLabel("남은 시간: --", SwingConstants.LEFT);
        scoreLabel = new JLabel("점수: 0", SwingConstants.RIGHT);
        styleLabel(timerLabel);
        styleLabel(scoreLabel);

        infoPanel.add(timerLabel, BorderLayout.WEST);
        infoPanel.add(scoreLabel, BorderLayout.EAST);
        add(infoPanel, BorderLayout.NORTH);

        // 메인 패널 (상하 분할)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND_COLOR);

        // 상단 문제 패널
        JPanel quizContentPanel = new JPanel(new BorderLayout(10, 10));
        quizContentPanel.setBackground(BACKGROUND_COLOR);
        quizDisplay = createStyledTextArea();
        quizDisplay.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        JScrollPane quizScrollPane = new JScrollPane(quizDisplay);
        quizScrollPane.setBorder(createStyledTitledBorder("문제"));
        quizContentPanel.add(quizScrollPane);

        // 하단 답변 패널
        JPanel answerContentPanel = new JPanel(new BorderLayout(10, 10));
        answerContentPanel.setBackground(BACKGROUND_COLOR);

        // 답변 내역 표시
        answerDisplay = createStyledTextArea();
        JScrollPane answerScrollPane = new JScrollPane(answerDisplay);
        answerScrollPane.setBorder(createStyledTitledBorder("답변 내역"));
        answerContentPanel.add(answerScrollPane, BorderLayout.CENTER);

        // 답안 입력 영역
        answerPanel = new JPanel(new CardLayout());
        answerPanel.setBackground(BACKGROUND_COLOR);
        currentQuizPanel = new JPanel();
        currentQuizPanel.setBackground(BACKGROUND_COLOR);
        answerPanel.add(currentQuizPanel, "DEFAULT");
        answerContentPanel.add(answerPanel, BorderLayout.SOUTH);

        // 분할 패널에 추가
        splitPane.setTopComponent(quizContentPanel);
        splitPane.setBottomComponent(answerContentPanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerLocation(200);

        add(splitPane, BorderLayout.CENTER);
    }

    private JTextArea createStyledTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(Color.WHITE);
        textArea.setForeground(TEXT_COLOR);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        return textArea;
    }

    private void styleLabel(JLabel label) {
        label.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        label.setForeground(PRIMARY_COLOR);
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
    }

    private Border createStyledTitledBorder(String title) {
        TitledBorder titledBorder = new TitledBorder(
                new LineBorder(PRIMARY_COLOR, 2, true),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 14),
                PRIMARY_COLOR
        );
        return new CompoundBorder(titledBorder, new EmptyBorder(8, 8, 8, 8));
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isPressed()) {
                    g.setColor(PRIMARY_COLOR);
                } else if (getModel().isRollover()) {
                    g.setColor(SECONDARY_COLOR);
                } else {
                    g.setColor(PRIMARY_COLOR.brighter());
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 20));
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(text)) / 2;
                int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
                g2.drawString(text, x, y);
            }
        };
        button.setPreferredSize(new Dimension(120, 60));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.addActionListener(e -> submitAnswer(text));
        return button;
    }

    private void createOXPanel() {
        currentQuizPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 15));
        JButton oButton = createStyledButton("O");
        JButton xButton = createStyledButton("X");

        currentQuizPanel.add(oButton);
        currentQuizPanel.add(xButton);
    }

    private void createMultipleChoicePanel(String[] options) {
        currentQuizPanel.setLayout(new GridLayout(0, 1, 8, 8));
        currentQuizPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < options.length; i++) {
            JRadioButton radio = createStyledRadioButton(options[i], i + 1);
            group.add(radio);
            currentQuizPanel.add(radio);
        }
    }

    private JRadioButton createStyledRadioButton(String text, int choiceNum) {
        JRadioButton radio = new JRadioButton(text);
        radio.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        radio.setBackground(BACKGROUND_COLOR);
        radio.setForeground(TEXT_COLOR);
        radio.setFocusPainted(false);
        radio.addActionListener(e -> submitAnswer(String.valueOf(choiceNum)));
        return radio;
    }

    private void createShortAnswerPanel() {
        currentQuizPanel.setLayout(new BorderLayout(10, 10));
        currentQuizPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        answerField = createStyledTextField();
        JButton submitButton = createStyledButton("제출");
        submitButton.setPreferredSize(new Dimension(100, 40));

        submitButton.addActionListener(e -> {
            String answer = answerField.getText().trim();
            if (!answer.isEmpty()) {
                submitAnswer(answer);
                answerField.setText("");
            }
        });

        currentQuizPanel.add(answerField, BorderLayout.CENTER);
        currentQuizPanel.add(submitButton, BorderLayout.EAST);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
                new LineBorder(PRIMARY_COLOR, 2, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    public void displayQuiz(String quiz) {
        SwingUtilities.invokeLater(() -> {
            quizDisplay.setText(quiz);
            // 단답형 패널 생성
            currentQuizPanel.removeAll();
            currentQuizPanel = new JPanel();
            currentQuizPanel.setBackground(BACKGROUND_COLOR);
            createShortAnswerPanel();
            answerPanel.add(currentQuizPanel, "QUIZ");
            ((CardLayout)answerPanel.getLayout()).show(answerPanel, "QUIZ");
            answerPanel.revalidate();
            answerPanel.repaint();
        });
    }

    public void displayAnswer(String message) {
        SwingUtilities.invokeLater(() -> {
            answerDisplay.append(message + "\n");
            answerDisplay.setCaretPosition(answerDisplay.getDocument().getLength());
        });
    }

    private void submitAnswer(String answer) {
        client.sendAnswer(answer);
        displayAnswer("나의 답변: " + answer);
    }

    public void updateTimer(int seconds) {
        SwingUtilities.invokeLater(() -> {
            timerLabel.setText("남은 시간: " + seconds + "초");
        });
    }

    public void updateScore(int score) {
        SwingUtilities.invokeLater(() -> {
            scoreLabel.setText("점수: " + score);
        });
    }

    public void clearChat() {
        SwingUtilities.invokeLater(() -> {
            quizDisplay.setText("");
            answerDisplay.setText("");
            timerLabel.setText("남은 시간: --");
            scoreLabel.setText("점수: 0");
        });
    }
}