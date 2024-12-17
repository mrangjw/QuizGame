package client.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class GameResultPanel extends JPanel {
    private static final Color GOLD_COLOR = new Color(255, 215, 0);
    private static final Color SILVER_COLOR = new Color(192, 192, 192);
    private static final Color BRONZE_COLOR = new Color(205, 127, 50);
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color DANGER_COLOR = new Color(220, 53, 69);

    private final ImageIcon goldMedal;
    private final ImageIcon silverMedal;
    private final ImageIcon bronzeMedal;

    public GameResultPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // 메달 이미지 로드
        goldMedal = loadResizedImageIcon("/resources/images/medal_gold.png", 60, 60);
        silverMedal = loadResizedImageIcon("/resources/images/medal_silver.png", 60, 60);
        bronzeMedal = loadResizedImageIcon("/resources/images/medal_bronze.png", 60, 60);
    }

    public void displayResults(List<Map.Entry<String, Integer>> sortedScores) {
        removeAll();

        // 상단 제목 패널
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("게임 결과", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        titlePanel.add(titleLabel);

        add(titlePanel, BorderLayout.NORTH);

        // 결과 표시 패널
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Color.WHITE);

        // 각 플레이어의 결과를 표시
        for (int i = 0; i < sortedScores.size(); i++) {
            Map.Entry<String, Integer> entry = sortedScores.get(i);
            JPanel playerPanel = createPlayerResultPanel(entry.getKey(), entry.getValue(), i);
            resultsPanel.add(playerPanel);
            if (i < sortedScores.size() - 1) {
                resultsPanel.add(Box.createVerticalStrut(15));
            }
        }

        // 스크롤 패널에 결과 패널 추가
        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // 다시하기 버튼
        JButton restartButton = createStyledButton("다시하기", PRIMARY_COLOR);
        restartButton.addActionListener(e -> {
            firePropertyChange("restartGame", false, true);
        });

        // 그만하기 버튼
        JButton exitButton = createStyledButton("그만하기", DANGER_COLOR);
        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "정말로 게임을 종료하시겠습니까?",
                    "게임 종료",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                firePropertyChange("exitGame", false, true);
            }
        });

        // 로비로 돌아가기 버튼
        JButton lobbyButton = createStyledButton("로비로 돌아가기", PRIMARY_COLOR);
        lobbyButton.addActionListener(e -> {
            firePropertyChange("exitToLobby", false, true);
        });

        buttonPanel.add(restartButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(lobbyButton);
        add(buttonPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    private JPanel createPlayerResultPanel(String playerName, int score, int rank) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(15, 0));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(600, 100));
        panel.setBorder(new CompoundBorder(
                new LineBorder(getRankColor(rank), 3, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // 왼쪽: 메달 아이콘
        JLabel medalLabel = new JLabel();
        medalLabel.setPreferredSize(new Dimension(70, 70));
        setMedalIcon(medalLabel, rank);
        medalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(medalLabel, BorderLayout.WEST);

        // 중앙: 플레이어 정보
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        infoPanel.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(playerName);
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        JLabel scoreLabel = new JLabel(score + "점");
        scoreLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));

        infoPanel.add(nameLabel);
        infoPanel.add(scoreLabel);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private void setMedalIcon(JLabel label, int rank) {
        switch (rank) {
            case 0 -> label.setIcon(goldMedal);
            case 1 -> label.setIcon(silverMedal);
            case 2 -> label.setIcon(bronzeMedal);
            default -> label.setText((rank + 1) + "등");
        }
    }

    private Color getRankColor(int rank) {
        return switch (rank) {
            case 0 -> GOLD_COLOR;
            case 1 -> SILVER_COLOR;
            case 2 -> BRONZE_COLOR;
            default -> Color.GRAY;
        };
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(baseColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(baseColor.brighter());
                } else {
                    g2.setColor(baseColor);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(text)) / 2;
                int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
                g2.drawString(text, x, y);
            }
        };

        button.setPreferredSize(new Dimension(150, 40));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
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
}