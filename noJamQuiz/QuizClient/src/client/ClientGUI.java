package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class ClientGUI extends JPanel {
    private QuizClient client;
    private JTextArea displayArea;
    private JTextField answerField;
    private JButton submitButton;
    private JButton leaveButton;

    public ClientGUI(QuizClient client) {
        this.client = client;
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initComponents();
    }

    private void initComponents() {
        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        leaveButton = new JButton("방 나가기");
        leaveButton.addActionListener(e -> client.leaveRoom());

        topPanel.add(leaveButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 디스플레이 영역
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(new CompoundBorder(
                new TitledBorder("대화"),
                new EmptyBorder(5, 5, 5, 5)));
        add(scrollPane, BorderLayout.CENTER);

        // 답안 입력 패널
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        answerField = new JTextField();
        submitButton = new JButton("전송");

        submitButton.addActionListener(e -> sendAnswer());
        answerField.addActionListener(e -> sendAnswer());

        inputPanel.add(answerField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    private void sendAnswer() {
        String answer = answerField.getText().trim();
        if (!answer.isEmpty()) {
            client.sendAnswer(answer);
            answerField.setText("");
            answerField.requestFocus();
        }
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            displayArea.append(message + "\n");
            displayArea.setCaretPosition(displayArea.getDocument().getLength());
        });
    }

    public void clearChat() {
        SwingUtilities.invokeLater(() -> {
            displayArea.setText("");
        });
    }
}