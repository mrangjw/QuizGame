package client.panels;

import client.QuizClient;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import model.RPS;

public class RPSPanel extends JDialog {
    private final java.util.List<String> players;
    private final String currentPlayer;
    private final QuizClient client;
    private final JLabel statusLabel;
    private final Map<String, RPS> playerChoices;
    private final javax.swing.Timer rpsTimer;

    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255);

    public RPSPanel(JDialog parent, java.util.List<String> players, String currentPlayer, QuizClient client) {
        super(parent, "가위바위보 결정전", true);
        this.players = new ArrayList<>(players);
        this.currentPlayer = currentPlayer;
        this.client = client;
        this.playerChoices = new HashMap<>();
        this.statusLabel = new JLabel("동점자 결정전: 가위바위보", SwingConstants.CENTER);
        this.rpsTimer = new javax.swing.Timer(1000, e -> checkResults());

        initComponents();
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);

        statusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        JPanel playersPanel = createPlayersPanel();
        mainPanel.add(playersPanel, BorderLayout.CENTER);

        if (players.contains(currentPlayer)) {
            JPanel buttonPanel = createRPSButtonPanel();
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        } else {
            JLabel waitLabel = new JLabel("다른 플레이어의 선택을 기다리는 중...", SwingConstants.CENTER);
            mainPanel.add(waitLabel, BorderLayout.SOUTH);
        }

        add(mainPanel);
    }

    private JPanel createPlayersPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (String player : players) {
            JLabel label = new JLabel("- " + player);
            label.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            label.setBorder(new EmptyBorder(5, 0, 5, 0));
            panel.add(label);
        }

        return panel;
    }

    private JPanel createRPSButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(BACKGROUND_COLOR);

        for (RPS choice : RPS.values()) {
            JButton button = createRPSButton(choice);
            panel.add(button);
        }

        return panel;
    }

    private JButton createRPSButton(RPS choice) {
        JButton button = new JButton();
        try {
            // 이미지 로드 및 크기 조정
            ImageIcon originalIcon = new ImageIcon(getClass().getResource("/resources/images/rps_" +
                    choice.name().toLowerCase() + ".png"));
            Image scaledImage = originalIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            button.setIcon(scaledIcon);
            button.setToolTipText(choice.getKorean());
        } catch (Exception e) {
            button.setText(choice.getKorean());
            System.err.println("이미지 로드 실패: " + e.getMessage());
        }

        button.setPreferredSize(new Dimension(80, 80));
        button.addActionListener(e -> {
            client.sendMessage("RPS_CHOICE:" + choice.name());
            disableButtons();
            statusLabel.setText("선택 완료: " + choice.getKorean());
            rpsTimer.start();
        });

        return button;
    }

    private void disableButtons() {
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            }
        }
    }

    private void checkResults() {
        if (playerChoices.size() == players.size()) {
            rpsTimer.stop();
            determineWinner();
        }
    }

    public void handleRPSChoice(String player, RPS choice) {
        playerChoices.put(player, choice);
        if (playerChoices.size() == players.size()) {
            determineWinner();
        }
    }

    private void determineWinner() {
        java.util.List<String> winners = new ArrayList<>();
        for (String player1 : players) {
            boolean isWinner = true;
            RPS choice1 = playerChoices.get(player1);

            for (String player2 : players) {
                if (!player1.equals(player2)) {
                    RPS choice2 = playerChoices.get(player2);
                    if (choice2.beats(choice1)) {
                        isWinner = false;
                        break;
                    }
                }
            }

            if (isWinner) {
                winners.add(player1);
            }
        }

        if (winners.size() == 1) {
            client.sendMessage("RPS_WINNER:" + winners.get(0));
            dispose();
        } else {
            players.clear();
            players.addAll(winners);
            playerChoices.clear();
            statusLabel.setText("동점! 다시 한 번!");
            refreshComponents();
        }
    }

    private void refreshComponents() {
        getContentPane().removeAll();
        initComponents();
        revalidate();
        repaint();
    }
}