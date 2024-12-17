package client;

import model.Room;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class LobbyGUI extends JPanel {
    private QuizClient client;
    private JPanel roomListPanel;
    private JTextArea messageArea;
    private JButton createRoomButton;
    private JLabel playerCountLabel;

    public LobbyGUI(QuizClient client) {
        this.client = client;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initComponents();
    }

    private void initComponents() {
        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        playerCountLabel = new JLabel("참가자: 0/6");
        createRoomButton = new JButton("방 만들기");
        createRoomButton.addActionListener(e -> showCreateRoomDialog());

        topPanel.add(playerCountLabel, BorderLayout.WEST);
        topPanel.add(createRoomButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 중앙 패널 (방 목록과 메시지 영역을 나눔)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // 방 목록 패널
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        JScrollPane roomScrollPane = new JScrollPane(roomListPanel);
        roomScrollPane.setBorder(new TitledBorder("대기실"));
        splitPane.setTopComponent(roomScrollPane);

        // 메시지 영역
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setBorder(new TitledBorder("메시지"));
        splitPane.setBottomComponent(messageScrollPane);

        add(splitPane, BorderLayout.CENTER);
    }

    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "방 만들기", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(300, 250);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField roomNameField = new JTextField();
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"통합", "경제", "사회", "넌센스"});
        JSpinner playerCountSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 6, 1));
        JSpinner problemCountSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        JSpinner timeLimitSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 60, 5));

        inputPanel.add(new JLabel("방 제목:"));
        inputPanel.add(roomNameField);
        inputPanel.add(new JLabel("카테고리:"));
        inputPanel.add(categoryBox);
        inputPanel.add(new JLabel("최대 인원:"));
        inputPanel.add(playerCountSpinner);
        inputPanel.add(new JLabel("문제 수:"));
        inputPanel.add(problemCountSpinner);
        inputPanel.add(new JLabel("제한 시간(초):"));
        inputPanel.add(timeLimitSpinner);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton createButton = new JButton("방 만들기");
        createButton.addActionListener(e -> {
            String roomName = roomNameField.getText().trim();
            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "방 제목을 입력해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String category = (String) categoryBox.getSelectedItem();
            int maxPlayers = (Integer) playerCountSpinner.getValue();
            int problemCount = (Integer) problemCountSpinner.getValue();
            int timeLimit = (Integer) timeLimitSpinner.getValue();

            client.createRoom(roomName, category, maxPlayers, problemCount, timeLimit);
            dialog.dispose();
        });

        JButton cancelButton = new JButton("취소");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    public void updateRoomList(Room[] rooms) {
        SwingUtilities.invokeLater(() -> {
            roomListPanel.removeAll();

            if (rooms == null || rooms.length == 0) {
                JLabel noRoomLabel = new JLabel("생성된 방이 없습니다.");
                noRoomLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                roomListPanel.add(Box.createVerticalStrut(10));
                roomListPanel.add(noRoomLabel);
            } else {
                for (Room room : rooms) {
                    JPanel roomPanel = createRoomPanel(room);
                    roomListPanel.add(roomPanel);
                    roomListPanel.add(Box.createVerticalStrut(5));
                }
            }

            roomListPanel.add(Box.createVerticalGlue());
            roomListPanel.revalidate();
            roomListPanel.repaint();
        });
    }

    private JPanel createRoomPanel(Room room) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(new CompoundBorder(
                new LineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(5, 5, 5, 5)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        String roomInfo = String.format("<html><b>%s</b><br>방장: %s | 카테고리: %s | 참가자: %d/%d</html>",
                room.getRoomName(), room.getHostName(), room.getCategory().getKoreanName(),
                room.getPlayers().size(), room.getMaxPlayers());

        JLabel infoLabel = new JLabel(roomInfo);
        infoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        JButton joinButton = new JButton("참가");
        joinButton.setEnabled(!room.isFull());
        if (room.isFull()) {
            joinButton.setText("만석");
        }

        joinButton.addActionListener(e -> client.joinRoom(room.getRoomId()));

        panel.add(infoLabel, BorderLayout.CENTER);
        panel.add(joinButton, BorderLayout.EAST);

        return panel;
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }

    public void updatePlayerCount(int count) {
        SwingUtilities.invokeLater(() -> {
            playerCountLabel.setText("참가자: " + count + "/6");
        });
    }
}