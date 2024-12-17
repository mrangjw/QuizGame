package client.panels;

import client.QuizClient;
import model.Quiz;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel {
    private QuizClient client;
    private String playerName;
    private JTextArea quizDisplay;
    private JTextArea answerDisplay;
    private JPanel answerPanel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JButton leaveButton;
    private JButton startButton;
    private JTextField answerField;

    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_COLOR = new Color(176, 196, 222);
    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255);
    private static final Color TEXT_COLOR = new Color(25, 25, 25);

    public GamePanel(QuizClient client) {
        this.client = client;
        this.playerName = client.getPlayerName();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BACKGROUND_COLOR);
        initComponents();
    }

    private void initComponents() {
        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(BACKGROUND_COLOR);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        startButton = createStyledButton("게임 시작");
        leaveButton = createStyledButton("방 나가기");

        startButton.addActionListener(e -> client.startGame());
        leaveButton.addActionListener(e -> client.leaveRoom());

        buttonPanel.add(startButton);
        buttonPanel.add(leaveButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // 상태 정보 패널 (타이머, 점수)
        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setBackground(BACKGROUND_COLOR);
        statusPanel.setBorder(new EmptyBorder(0, 5, 5, 5));

        timerLabel = new JLabel("남은 시간: --", SwingConstants.LEFT);
        scoreLabel = new JLabel("점수: 0", SwingConstants.RIGHT);
        styleLabel(timerLabel);
        styleLabel(scoreLabel);

        statusPanel.add(timerLabel, BorderLayout.WEST);
        statusPanel.add(scoreLabel, BorderLayout.EAST);
        topPanel.add(statusPanel, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);

        // 메인 분할 패널
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
        quizScrollPane.setPreferredSize(new Dimension(0, 200));
        quizContentPanel.add(quizScrollPane);

        // 하단 답변 패널
        JPanel answerContentPanel = new JPanel(new BorderLayout(10, 10));
        answerContentPanel.setBackground(BACKGROUND_COLOR);

        answerDisplay = createStyledTextArea();
        JScrollPane answerScrollPane = new JScrollPane(answerDisplay);
        answerScrollPane.setBorder(createStyledTitledBorder("답변 내역"));
        answerContentPanel.add(answerScrollPane, BorderLayout.CENTER);

        // 답변 입력 패널
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(BACKGROUND_COLOR);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        answerField = createStyledTextField();
        JButton submitButton = createStyledButton("전송");
        submitButton.setPreferredSize(new Dimension(100, 40));

        submitButton.addActionListener(e -> sendAnswer());
        answerField.addActionListener(e -> sendAnswer());

        inputPanel.add(answerField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        answerContentPanel.add(inputPanel, BorderLayout.SOUTH);

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
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 14));
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(text)) / 2;
                int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
                g2.drawString(text, x, y);
            }
        };
        button.setPreferredSize(new Dimension(120, 40));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        return button;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
                new LineBorder(PRIMARY_COLOR, 2, true),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    private Border createStyledTitledBorder(String title) {
        TitledBorder titledBorder = new TitledBorder(
                new LineBorder(PRIMARY_COLOR, 2, true),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 14),
                PRIMARY_COLOR);
        return new CompoundBorder(titledBorder, new EmptyBorder(8, 8, 8, 8));
    }

    private void sendAnswer() {
        String answer = answerField.getText().trim();
        if (!answer.isEmpty()) {
            client.sendAnswer(answer);
            answerField.setText("");
            answerField.requestFocus();
            answerDisplay.append("나의 답변: " + answer + "\n");
        }
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String content = message;

            // 방 메시지 제거
            String roomPrefix = "[방 ";
            if (content.startsWith(roomPrefix)) {
                content = content.substring(content.indexOf("]") + 2);
            }

            if (content.startsWith("GAME_RESULT:")) {
                handleGameResult(content.substring(12));
                return;
            }

            if (content.startsWith("RPS_")) {
                handleRPSMessage(content);
                return;
            }

            if (content.startsWith("SCORE:")) {
                String[] parts = content.substring(6).split(":");
                if (parts.length == 2 && parts[0].equals(playerName)) {
                    scoreLabel.setText("점수: " + parts[1]);
                }
                return;
            }

            if (content.startsWith("TIME:")) {
                try {
                    int seconds = Integer.parseInt(content.substring(5));
                    timerLabel.setText("남은 시간: " + seconds + "초");
                } catch (NumberFormatException e) {
                    System.err.println("타이머 파싱 오류: " + e.getMessage());
                }
                return;
            }

            if (content.startsWith("QUIZ:")) {
                quizDisplay.setText(content.substring(5));
                answerField.requestFocus();
                return;
            }

            if (content.contains("정답입니다") || content.contains("오답입니다") ||
                    content.contains("시간이 종료되었습니다") || content.startsWith("정답:")) {
                answerDisplay.append(content + "\n");
                return;
            }

            if (!content.trim().isEmpty()) {
                answerDisplay.append(content + "\n");
                answerDisplay.setCaretPosition(answerDisplay.getDocument().getLength());
            }
        });
    }

    private void handleGameResult(String resultData) {
        String[] entries = resultData.split(";");
        List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>();

        for (String entry : entries) {
            if (!entry.trim().isEmpty()) {
                String[] parts = entry.split(":");
                sortedScores.add(new AbstractMap.SimpleEntry<>(parts[0], Integer.parseInt(parts[1])));
            }
        }

        firePropertyChange("gameResult", null, sortedScores);
    }

    private void handleRPSMessage(String message) {
        firePropertyChange("rpsMessage", null, message);
    }

    public void clearChat() {
        SwingUtilities.invokeLater(() -> {
            quizDisplay.setText("");
            answerDisplay.setText("");
            timerLabel.setText("남은 시간: --");
            scoreLabel.setText("점수: 0");
        });
    }

    public void setStartButtonEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
    }

    public void reset() {
        clearChat();
        setStartButtonEnabled(true);
    }
}