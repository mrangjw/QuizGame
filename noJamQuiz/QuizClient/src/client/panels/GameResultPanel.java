package client.panels;

import client.QuizClient;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;

public class GameResultPanel extends JDialog {
    private final Map<String, Integer> playerScores;
    private final String currentPlayer;
    private final QuizClient client;
    private final JPanel mainPanel;
    private final JPanel scorePanel;
    private RPSPanel rpsPanel;
    private final boolean rpsDecided;

    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255);
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color GOLD_COLOR = new Color(255, 215, 0);
    private static final Color SILVER_COLOR = new Color(192, 192, 192);
    private static final Color BRONZE_COLOR = new Color(205, 127, 50);

    public GameResultPanel(JFrame parent, Map<String, Integer> scores, String currentPlayer,
                           QuizClient client, boolean rpsDecided) {
        super(parent, "게임 종료!", true);
        this.playerScores = new HashMap<>(scores);
        this.currentPlayer = currentPlayer;
        this.client = client;
        this.rpsDecided = rpsDecided;
        this.mainPanel = new JPanel(new BorderLayout(10, 10));
        this.scorePanel = new JPanel(new GridLayout(0, 1, 5, 5));

        initComponents();
        checkForTie();

        setSize(400, 500);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 제목 라벨
        JLabel titleLabel = new JLabel(rpsDecided ?
                "게임 종료! (가위바위보 결과 반영)" : "게임 종료!",
                SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 점수 패널
        scorePanel.setBackground(BACKGROUND_COLOR);
        scorePanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // 결과 정렬 및 표시
        java.util.List<Map.Entry<String, Integer>> sortedScores =
                new ArrayList<>(playerScores.entrySet());
        sortedScores.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedScores) {
            JPanel playerPanel = createPlayerScorePanel(entry.getKey(), entry.getValue(), rank);
            scorePanel.add(playerPanel);
            rank++;
        }

        mainPanel.add(scorePanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        JButton closeButton = new JButton("확인");
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.addActionListener(e -> dispose());

        // 버튼 스타일링
        closeButton.setBackground(PRIMARY_COLOR);
        closeButton.setForeground(Color.WHITE);
        closeButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        closeButton.setFocusPainted(false);

        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JPanel createPlayerScorePanel(String playerName, int score, int rank) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // 순위와 메달 패널
        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rankPanel.setBackground(BACKGROUND_COLOR);

        // 순위 표시
        JLabel rankLabel = new JLabel(rank + "등");
        rankLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        rankPanel.add(rankLabel);

        // 메달 이미지 추가
        if (rank <= 3) {
            try {
                String medalType = rank == 1 ? "gold" : rank == 2 ? "silver" : "bronze";
                ImageIcon originalIcon = new ImageIcon(
                        getClass().getResource("/resources/images/medal_" + medalType + ".png")
                );
                Image scaledImage = originalIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                JLabel medalLabel = new JLabel(new ImageIcon(scaledImage));
                rankPanel.add(medalLabel);
            } catch (Exception e) {
                System.err.println("메달 이미지 로드 실패: " + e.getMessage());
                JLabel medalText = new JLabel("🏅");
                medalText.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                rankPanel.add(medalText);
            }
        }

        panel.add(rankPanel);

        // 플레이어 이름
        JLabel nameLabel = new JLabel(playerName);
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        panel.add(nameLabel);

        // 점수
        JLabel scoreLabel = new JLabel(score + "점");
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        scoreLabel.setForeground(PRIMARY_COLOR);
        panel.add(scoreLabel);

        // 현재 플레이어 표시
        if (playerName.equals(currentPlayer)) {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(PRIMARY_COLOR, 2, true),
                    new EmptyBorder(3, 3, 3, 3)
            ));
            panel.setBackground(new Color(230, 240, 250));
        }

        return panel;
    }

    private void checkForTie() {
        if (rpsDecided) {
            return;  // 이미 가위바위보로 결정된 경우 추가 체크 불필요
        }

        int maxScore = Collections.max(playerScores.values());
        java.util.List<String> tiedPlayers = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() == maxScore) {
                tiedPlayers.add(entry.getKey());
            }
        }

        if (tiedPlayers.size() > 1 && tiedPlayers.contains(currentPlayer)) {
            showRPSPanel(tiedPlayers);
        }
    }

    private void showRPSPanel(java.util.List<String> tiedPlayers) {
        rpsPanel = new RPSPanel(this, tiedPlayers, currentPlayer, client);
        rpsPanel.setVisible(true);
    }
}