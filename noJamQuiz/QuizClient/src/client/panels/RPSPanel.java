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
    private JPanel playersPanel;
    private Timer countdownTimer;
    private int remainingTime;
    private boolean canParticipate;

    private final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private final Color SELECTED_COLOR = new Color(135, 206, 250);
    private final Font TITLE_FONT = new Font("맑은 고딕", Font.BOLD, 24);
    private final Font CONTENT_FONT = new Font("맑은 고딕", Font.PLAIN, 16);

    private ImageIcon rockIcon;
    private ImageIcon paperIcon;
    private ImageIcon scissorsIcon;

    public RPSPanel(String playerName) {
        this.playerName = playerName;
        this.canParticipate = false;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        loadImages();
        initializeComponents();
    }

    private void loadImages() {
        rockIcon = loadResizedImageIcon("/resources/images/rps_rock.png", 100, 100);
        paperIcon = loadResizedImageIcon("/resources/images/rps_paper.png", 100, 100);
        scissorsIcon = loadResizedImageIcon("/resources/images/rps_scissors.png", 100, 100);
    }

    private void initializeComponents() {
        // 상단 패널 (타이틀 + 타이머)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("가위바위보로 승자 결정하기", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);

        timerLabel = new JLabel("10초", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        timerLabel.setBorder(new EmptyBorder(0, 0, 0, 20));

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(timerLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 플레이어 상태 패널
        playersPanel = new JPanel();
        playersPanel.setBackground(Color.WHITE);
        playersPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(playersPanel, BorderLayout.CENTER);

        // 상태 라벨
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(CONTENT_FONT);
        add(statusLabel, BorderLayout.CENTER);

        // 선택 버튼 패널
        JPanel choicePanel = createChoicePanel();
        add(choicePanel, BorderLayout.SOUTH);
    }

    private JPanel createChoicePanel() {
        JPanel choicePanel = new JPanel(new GridLayout(1, 3, 20, 0));
        choicePanel.setBackground(Color.WHITE);
        choicePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        createRPSButton("가위", scissorsIcon, RPS.Choice.SCISSORS, choicePanel);
        createRPSButton("바위", rockIcon, RPS.Choice.ROCK, choicePanel);
        createRPSButton("보", paperIcon, RPS.Choice.PAPER, choicePanel);

        return choicePanel;
    }

    private void createRPSButton(String text, ImageIcon icon, RPS.Choice choice, JPanel panel) {
        JButton button = new JButton(icon);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true));
        button.setToolTipText(text);
        button.setPreferredSize(new Dimension(120, 120));

        button.addActionListener(e -> {
            if (selectedChoice == null && canParticipate) {
                selectedChoice = choice;
                updateButtonStates();
                firePropertyChange("choiceMade", null, choice);
                statusLabel.setText(text + "를 선택했습니다!");
                disableAllButtons();
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (selectedChoice == null && canParticipate) {
                    button.setBackground(SELECTED_COLOR.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedChoice != choice) {
                    button.setBackground(Color.WHITE);
                }
            }
        });

        panel.add(button);
    }

    private void updateButtonStates() {
        Component[] buttons = ((JPanel)getComponent(3)).getComponents();
        for (Component comp : buttons) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setEnabled(false);
                if (button.getToolTipText().equals(getChoiceName(selectedChoice))) {
                    button.setBackground(SELECTED_COLOR);
                }
            }
        }
    }

    private String getChoiceName(RPS.Choice choice) {
        return switch (choice) {
            case ROCK -> "바위";
            case PAPER -> "보";
            case SCISSORS -> "가위";
        };
    }

    public void startTimer(int seconds) {
        remainingTime = seconds;
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timer(1000, e -> {
            remainingTime--;
            timerLabel.setText(remainingTime + "초");

            if (remainingTime <= 0) {
                countdownTimer.stop();
                if (selectedChoice == null && canParticipate) {
                    statusLabel.setText("시간이 초과되었습니다!");
                    disableAllButtons();
                }
            }
        });
        countdownTimer.start();
    }

    public void updatePlayersStatus(List<String> readyPlayers, List<String> waitingPlayers) {
        SwingUtilities.invokeLater(() -> {
            playersPanel.removeAll();
            playersPanel.setLayout(new GridLayout(1, readyPlayers.size() + waitingPlayers.size(), 10, 0));

            // 준비된 플레이어 표시
            for (String player : readyPlayers) {
                addPlayerCard(player, true);
            }

            // 대기중인 플레이어 표시
            for (String player : waitingPlayers) {
                addPlayerCard(player, false);
            }

            // 참가 가능한 플레이어인지 확인
            canParticipate = readyPlayers.contains(playerName) || waitingPlayers.contains(playerName);

            if (!canParticipate) {
                statusLabel.setText("동점자들의 가위바위보를 기다리는 중...");
                disableAllButtons();
            } else {
                statusLabel.setText("선택해주세요!");
                if (selectedChoice == null) {
                    enableAllButtons();
                }
            }

            playersPanel.revalidate();
            playersPanel.repaint();
        });
    }

    private void addPlayerCard(String player, boolean isReady) {
        JPanel playerCard = new JPanel();
        playerCard.setLayout(new BoxLayout(playerCard, BoxLayout.Y_AXIS));
        playerCard.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true));
        playerCard.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(player);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(CONTENT_FONT);

        if (player.equals(playerName)) {
            nameLabel.setForeground(PRIMARY_COLOR);
            nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        }

        JLabel statusLabel = new JLabel(isReady ? "준비 완료" : "대기 중");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        statusLabel.setForeground(isReady ? new Color(40, 167, 69) : new Color(108, 117, 125));

        playerCard.add(Box.createVerticalStrut(10));
        playerCard.add(nameLabel);
        playerCard.add(Box.createVerticalStrut(5));
        playerCard.add(statusLabel);
        playerCard.add(Box.createVerticalStrut(10));

        playersPanel.add(playerCard);
    }

    private void enableAllButtons() {
        Component[] buttons = ((JPanel)getComponent(3)).getComponents();
        for (Component comp : buttons) {
            if (comp instanceof JButton) {
                comp.setEnabled(true);
                comp.setBackground(Color.WHITE);
            }
        }
    }

    private void disableAllButtons() {
        Component[] buttons = ((JPanel)getComponent(3)).getComponents();
        for (Component comp : buttons) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            }
        }
    }

    private ImageIcon loadResizedImageIcon(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(path));
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("이미지 로드 실패: " + path);
            return null;
        }
    }

    public void reset() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        selectedChoice = null;
        enableAllButtons();
        statusLabel.setText("선택해주세요!");
        timerLabel.setText("10초");
    }
}
