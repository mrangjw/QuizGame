package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import model.Quiz;

public class ClientGUI extends JPanel {
    private QuizClient client;
    private String playerName;
    private JTextArea displayArea;
    private JPanel quizPanel;
    private JTextArea quizDisplay;
    private JTextArea answerDisplay;
    private JTextField answerField;
    private JButton submitButton;
    private JButton leaveButton;
    private JButton startButton;
    private JLabel timerLabel;
    private JLabel scoreLabel;

    // 커스텀 색상 정의
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_COLOR = new Color(176, 196, 222);
    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255);
    private static final Color TEXT_COLOR = new Color(25, 25, 25);

    public ClientGUI(QuizClient client, String playerName) {  // 생성자 수정
        this.client = client;
        this.playerName = playerName;  // 플레이어 이름 저장
        setLayout(new BorderLayout(5, 5));
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
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(BACKGROUND_COLOR);

        // 상단 퀴즈 패널
        JPanel quizContentPanel = new JPanel(new BorderLayout(10, 10));
        quizContentPanel.setBackground(BACKGROUND_COLOR);

        quizDisplay = createStyledTextArea();
        quizDisplay.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        JScrollPane quizScrollPane = new JScrollPane(quizDisplay);
        quizScrollPane.setBorder(createStyledTitledBorder("문제"));
        quizContentPanel.add(quizScrollPane, BorderLayout.CENTER);

        // 하단 답변 패널
        JPanel answerPanel = new JPanel(new BorderLayout(10, 10));
        answerPanel.setBackground(BACKGROUND_COLOR);

        answerDisplay = createStyledTextArea();
        JScrollPane answerScrollPane = new JScrollPane(answerDisplay);
        answerScrollPane.setBorder(createStyledTitledBorder("답변 내역"));
        answerPanel.add(answerScrollPane, BorderLayout.CENTER);

        // 답변 입력 패널
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(BACKGROUND_COLOR);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        answerField = createStyledTextField();
        submitButton = createStyledButton("전송");
        submitButton.setPreferredSize(new Dimension(100, 40));

        submitButton.addActionListener(e -> sendAnswer());
        answerField.addActionListener(e -> sendAnswer());

        inputPanel.add(answerField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        answerPanel.add(inputPanel, BorderLayout.SOUTH);

        // 분할 패널 설정
        mainSplitPane.setTopComponent(quizContentPanel);
        mainSplitPane.setBottomComponent(answerPanel);
        mainSplitPane.setDividerLocation(200);
        mainSplitPane.setResizeWeight(0.4);

        add(mainSplitPane, BorderLayout.CENTER);
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
                new EmptyBorder(5, 10, 5, 10)
        ));
        return field;
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
            String roomPrefix = "[방 1] ";
            if (content.startsWith(roomPrefix)) {
                content = content.substring(roomPrefix.length());
            }

            if (content.startsWith("QUIZ:")) {
                String quizContent = content.substring("QUIZ:".length());
                quizDisplay.setText(quizContent);
                return;
            }

            if (content.startsWith("TIME:")) {
                try {
                    String timeStr = content.substring("TIME:".length());
                    int seconds = Integer.parseInt(timeStr);
                    timerLabel.setText("남은 시간: " + seconds + "초");
                } catch (NumberFormatException e) {
                    System.err.println("타이머 파싱 오류: " + e.getMessage());
                }
                return;
            }

            // 점수 메시지 처리 수정
            if (content.startsWith("SCORE:")) {
                try {
                    String[] parts = content.substring("SCORE:".length()).split(":");
                    if (parts.length == 2) {
                        String scorePlayerName = parts[0];
                        // 자신의 점수일 때만 업데이트
                        if (scorePlayerName.equals(playerName)) {
                            scoreLabel.setText("점수: " + parts[1]);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("점수 파싱 오류: " + e.getMessage());
                }
                return;
            }

            answerDisplay.append(message + "\n");
            answerDisplay.setCaretPosition(answerDisplay.getDocument().getLength());
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

    public void setStartButtonEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
    }

    public void reset() {
        clearChat();
        setStartButtonEnabled(true);
    }
}