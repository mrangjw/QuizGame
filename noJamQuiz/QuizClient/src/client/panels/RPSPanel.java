package client.panels;

import model.RPS;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.List;

public class RPSPanel extends JPanel {
    private final String playerName;
    private RPS.Choice selectedChoice;
    private JLabel timerLabel;
    private JLabel statusLabel;
    private JPanel playerPanel;
    private Timer countdownTimer;
    private int remainingTime;
    private boolean isParticipant;

    private JButton rockButton;
    private JButton paperButton;
    private JButton scissorsButton;

    private final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private final Color SELECTED_COLOR = new Color(135, 206, 250);
    private final Font TITLE_FONT = new Font("맑은 고딕", Font.BOLD, 24);
    private final Font CONTENT_FONT = new Font("맑은 고딕", Font.PLAIN, 16);

    private final ImageIcon rockIcon;
    private final ImageIcon paperIcon;
    private final ImageIcon scissorsIcon;

    public RPSPanel(String playerName) {
        this.playerName = playerName;
        this.isParticipant = false;

        rockIcon = loadResizedImageIcon("/resources/images/rps_rock.png", 100, 100);
        paperIcon = loadResizedImageIcon("/resources/images/rps_paper.png", 100, 100);
        scissorsIcon = loadResizedImageIcon("/resources/images/rps_scissors.png", 100, 100);

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        playerPanel = new JPanel();
        playerPanel.setBackground(Color.WHITE);
        playerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(playerPanel, BorderLayout.CENTER);

        JPanel choicePanel = createChoicePanel();
        add(choicePanel, BorderLayout.SOUTH);

        statusLabel = new JLabel("선택해주세요!", SwingConstants.CENTER);
        statusLabel.setFont(CONTENT_FONT);
        add(statusLabel, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("가위바위보로 승자 결정하기", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);

        timerLabel = new JLabel("10초", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        timerLabel.setBorder(new EmptyBorder(0, 0, 0, 20));

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(timerLabel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createChoicePanel() {
        JPanel choicePanel = new JPanel(new GridLayout(1, 3, 20, 0));
        choicePanel.setBackground(Color.WHITE);
        choicePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        rockButton = createChoiceButton(rockIcon, "바위", RPS.Choice.ROCK);
        paperButton = createChoiceButton(paperIcon, "보", RPS.Choice.PAPER);
        scissorsButton = createChoiceButton(scissorsIcon, "가위", RPS.Choice.SCISSORS);

        choicePanel.add(rockButton);
        choicePanel.add(paperButton);
        choicePanel.add(scissorsButton);

        return choicePanel;
    }

    private JButton createChoiceButton(ImageIcon icon, String text, RPS.Choice choice) {
        JButton button = new JButton();
        button.setIcon(icon);
        button.setToolTipText(text);
        button.setPreferredSize(new Dimension(120, 120));
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true));
        button.setFocusPainted(false);

        button.addActionListener(e -> {
            if (selectedChoice == null && isParticipant) {
                selectedChoice = choice;
                updateButtonStates();
                firePropertyChange("choiceMade", null, choice);
                enableAllButtons(false);
                statusLabel.setText(text + "를 선택했습니다!");
            }
        });

        return button;
    }

    private void updateButtonStates() {
        rockButton.setBackground(RPS.Choice.ROCK == selectedChoice ? SELECTED_COLOR : Color.WHITE);
        paperButton.setBackground(RPS.Choice.PAPER == selectedChoice ? SELECTED_COLOR : Color.WHITE);
        scissorsButton.setBackground(RPS.Choice.SCISSORS == selectedChoice ? SELECTED_COLOR : Color.WHITE);
    }

    public void startTimer(int seconds) {
        remainingTime = seconds;
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        if (isParticipant) {
            enableAllButtons(true);
            selectedChoice = null;
            updateButtonStates();
        } else {
            enableAllButtons(false);
            statusLabel.setText("다른 플레이어들의 가위바위보를 기다리는 중...");
        }

        countdownTimer = new Timer(1000, e -> {
            remainingTime--;
            timerLabel.setText(remainingTime + "초");

            if (remainingTime <= 0) {
                countdownTimer.stop();
                if (selectedChoice == null && isParticipant) {
                    statusLabel.setText("시간이 초과되었습니다!");
                    enableAllButtons(false);
                }
            }
        });
        countdownTimer.start();
    }

    private void enableAllButtons(boolean enabled) {
        rockButton.setEnabled(enabled);
        paperButton.setEnabled(enabled);
        scissorsButton.setEnabled(enabled);
    }

    public void updatePlayers(String msg) {
        String[] players = msg.split(",");
        isParticipant = false;

        for (String player : players) {
            if (player.equals(playerName)) {
                isParticipant = true;
                break;
            }
        }

        SwingUtilities.invokeLater(() -> {
            playerPanel.removeAll();
            playerPanel.setLayout(new GridLayout(1, players.length, 10, 0));

            for (String player : players) {
                JPanel playerCard = new JPanel();
                playerCard.setLayout(new BoxLayout(playerCard, BoxLayout.Y_AXIS));
                playerCard.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true));
                playerCard.setBackground(Color.WHITE);

                JLabel nameLabel = new JLabel(player);
                nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                nameLabel.setFont(CONTENT_FONT);

                playerCard.add(Box.createVerticalStrut(10));
                playerCard.add(nameLabel);
                playerCard.add(Box.createVerticalStrut(10));

                playerPanel.add(playerCard);
            }

            if (!isParticipant) {
                enableAllButtons(false);
                statusLabel.setText("다른 플레이어들의 가위바위보를 기다리는 중...");
            }

            playerPanel.revalidate();
            playerPanel.repaint();
        });
    }

    private ImageIcon loadResizedImageIcon(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    public void reset() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        selectedChoice = null;
        updateButtonStates();
        enableAllButtons(true);
        statusLabel.setText("선택해주세요!");
        timerLabel.setText("10초");
    }

    public void updatePlayersStatus(List<String> readyPlayers, List<String> waitingPlayers) {
    }
}