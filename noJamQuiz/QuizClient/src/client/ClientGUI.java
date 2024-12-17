package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import model.Quiz;
import client.panels.RPSPanel;
import client.panels.GameResultPanel;

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

    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_COLOR = new Color(176, 196, 222);
    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255);
    private static final Color TEXT_COLOR = new Color(25, 25, 25);

    public ClientGUI(QuizClient client, String playerName) {
        this.client = client;
        this.playerName = playerName;
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BACKGROUND_COLOR);
        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(BACKGROUND_COLOR);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        startButton = createStyledButton("게임 시작");
        leaveButton = createStyledButton("방 나가기");

        startButton.addActionListener(e -> client.startGame());
        leaveButton.addActionListener(e -> client.leaveRoom());

        buttonPanel.add(startButton);
        buttonPanel.add(leaveButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBackground(BACKGROUND_COLOR);
        infoPanel.setBorder(new EmptyBorder(0, 5, 5, 5));

        timerLabel = new JLabel("남은 시간: --", SwingConstants.LEFT);
        scoreLabel = new JLabel("점수: 0", SwingConstants.RIGHT);
        styleLabel(timerLabel);
        styleLabel(scoreLabel);

        infoPanel.add(timerLabel, BorderLayout.WEST);
        infoPanel.add(scoreLabel, BorderLayout.EAST);
        topPanel.add(infoPanel, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(BACKGROUND_COLOR);

        JPanel quizContentPanel = new JPanel(new BorderLayout(10, 10));
        quizContentPanel.setBackground(BACKGROUND_COLOR);

        quizDisplay = createStyledTextArea();
        quizDisplay.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        JScrollPane quizScrollPane = new JScrollPane(quizDisplay);
        quizScrollPane.setBorder(createStyledTitledBorder("문제"));
        quizContentPanel.add(quizScrollPane, BorderLayout.CENTER);

        JPanel answerPanel = new JPanel(new BorderLayout(10, 10));
        answerPanel.setBackground(BACKGROUND_COLOR);

        answerDisplay = createStyledTextArea();
        JScrollPane answerScrollPane = new JScrollPane(answerDisplay);
        answerScrollPane.setBorder(createStyledTitledBorder("답변 내역"));
        answerPanel.add(answerScrollPane, BorderLayout.CENTER);

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

            if (content.equals("SELECT_MODE")) {
                Object[] options = {"일반 모드", "GPT 모드"};
                String[] descriptions = {
                        "서버에 저장된 일반 문제를 사용합니다.",
                        "GPT가 실시간으로 문제를 생성합니다."
                };

                int choice = JOptionPane.showOptionDialog(
                        this,
                        "퀴즈 모드를 선택해주세요.\n\n" +
                                "일반 모드: " + descriptions[0] + "\n" +
                                "GPT 모드: " + descriptions[1],
                        "모드 선택",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                if (choice != JOptionPane.CLOSED_OPTION) {
                    client.sendMessage("MODE_CHOICE:" + (choice == 1 ? "GPT" : "NORMAL"));
                }
                return;
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

            if (content.startsWith("SCORE:")) {
                try {
                    String[] parts = content.substring("SCORE:".length()).split(":");
                    if (parts.length == 2 && parts[0].equals(playerName)) {
                        scoreLabel.setText("점수: " + parts[1]);
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

    public void startRPSGame(List<String> players) {
        RPSPanel rpsPanel = new RPSPanel(null, players, playerName, client);
        rpsPanel.setVisible(true);
    }

    public void showGameResult(Map<String, Integer> scores, boolean rpsDecided) {
        GameResultPanel resultPanel = new GameResultPanel(client.getMainFrame(), scores, playerName, client, rpsDecided);
        resultPanel.setVisible(true);
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