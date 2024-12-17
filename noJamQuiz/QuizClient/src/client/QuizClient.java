package client;

import model.Room;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class QuizClient extends JFrame {
    private ClientGUI gameGUI;
    private LobbyGUI lobbyGUI;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String playerName;
    private Thread receiveThread;
    private int currentRoomId = -1;

    public QuizClient(String playerName) {
        this.playerName = playerName;
        initComponents();
        initFrame();
    }

    private void initComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        lobbyGUI = new LobbyGUI(this);
        gameGUI = new ClientGUI(this, playerName);

        mainPanel.add(lobbyGUI, "LOBBY");
        mainPanel.add(gameGUI, "GAME");

        cardLayout.show(mainPanel, "LOBBY");
    }

    private void initFrame() {
        setTitle("퀴즈 게임 - " + playerName);
        setSize(800, 600);
        add(mainPanel);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject("ID:" + playerName);

            receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();
        } catch (IOException e) {
            showMessage("서버 연결 실패: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            showMessage("서버와의 연결이 종료되었습니다.");
        } catch (IOException e) {
            showMessage("연결 종료 중 오류 발생: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        try {
            while (!Thread.interrupted() && socket != null && !socket.isClosed()) {
                Object received = in.readObject();
                if (received instanceof String) {
                    String message = (String)received;
                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                showMessage("서버와의 연결이 끊어졌습니다: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            showMessage("메시지 수신 중 오류 발생: " + e.getMessage());
        }
    }

    private void handleMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("ROOM_LIST:")) {
                handleRoomList(message);
            } else if (message.startsWith("JOIN_ROOM:")) {
                currentRoomId = Integer.parseInt(message.substring(10));
                cardLayout.show(mainPanel, "GAME");
                gameGUI.clearChat();
            } else if (message.startsWith("RPS_START:")) {
                String[] players = message.substring(10).split(",");
                List<String> playerList = new ArrayList<>(Arrays.asList(players));
                gameGUI.startRPSGame(playerList);
            } else if (message.startsWith("GAME_END:")) {
                handleGameEnd(message);
            } else if (message.equals("USE_GPT")) {
                handleGPTChoice();
            } else if (message.equals("LOBBY:")) {
                currentRoomId = -1;
                cardLayout.show(mainPanel, "LOBBY");
            } else {
                if (currentRoomId != -1) {
                    gameGUI.displayMessage(message);
                } else {
                    lobbyGUI.displayMessage(message);
                }
            }
        });
    }

    private void handleGameEnd(String message) {
        String[] parts = message.substring(9).split(";");
        Map<String, Integer> scores = new HashMap<>();
        boolean rpsDecided = message.contains("RPS_DECIDED:");

        String scoresStr = rpsDecided ? message.substring(message.indexOf(":") + 1) : message.substring(9);
        String[] scoreEntries = scoresStr.split(";");

        for (String entry : scoreEntries) {
            if (!entry.trim().isEmpty()) {
                String[] scoreParts = entry.split(",");
                scores.put(scoreParts[0], Integer.parseInt(scoreParts[1]));
            }
        }

        gameGUI.showGameResult(scores, rpsDecided);
    }

    private void handleRoomList(String message) {
        try {
            String[] roomDataArray = message.substring(10).split(";");
            ArrayList<Room> rooms = new ArrayList<>();

            for (String roomData : roomDataArray) {
                if (roomData.trim().isEmpty()) continue;

                String[] parts = roomData.split(",");
                int roomId = Integer.parseInt(parts[0]);
                String roomName = parts[1];
                Room.QuizCategory category = Room.QuizCategory.fromKoreanName(parts[2]);
                String hostName = parts[3];
                int currentPlayers = Integer.parseInt(parts[4]);
                int maxPlayers = Integer.parseInt(parts[5]);
                int questionCount = Integer.parseInt(parts[6]);
                int timePerQuestion = Integer.parseInt(parts[7]);

                Room room = new Room(roomId, roomName, hostName, maxPlayers, category, questionCount, timePerQuestion);
                rooms.add(room);
            }

            if (currentRoomId == -1) {
                lobbyGUI.updateRoomList(rooms.toArray(new Room[0]));
            }
        } catch (Exception e) {
            showMessage("방 목록 업데이트 실패: " + e.getMessage());
        }
    }

    private void handleGPTChoice() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "GPT를 통해 퀴즈를 내시겠습니까?",
                "퀴즈 모드 선택",
                JOptionPane.YES_NO_OPTION
        );
        sendMessage("GPT_CHOICE:" + (choice == JOptionPane.YES_OPTION ? "Y" : "N"));
    }

    public void createRoom(String roomName, String category, int maxPlayers, int questionCount, int timePerQuestion) {
        if (socket != null && !socket.isClosed() && out != null) {
            sendMessage(String.format("CREATE_ROOM:%s,%s,%d,%d,%d",
                    roomName, category, maxPlayers, questionCount, timePerQuestion));
        } else {
            showMessage("서버와 연결되어 있지 않습니다.");
        }
    }

    public void startGame() {
        if (currentRoomId != -1) {
            sendMessage("START_GAME:" + currentRoomId);
        }
    }

    public void joinRoom(int roomId) {
        if (socket != null && !socket.isClosed() && out != null) {
            sendMessage("JOIN_ROOM:" + roomId);
        } else {
            showMessage("서버와 연결되어 있지 않습니다.");
        }
    }

    public void leaveRoom() {
        if (currentRoomId != -1) {
            sendMessage("LEAVE_ROOM:" + currentRoomId);
            currentRoomId = -1;
            cardLayout.show(mainPanel, "LOBBY");
            gameGUI.clearChat();
        }
    }

    public void sendMessage(String message) {
        try {
            if (socket != null && !socket.isClosed() && out != null) {
                out.writeObject(message);
                out.flush();
            } else {
                showMessage("서버와 연결되어 있지 않습니다.");
            }
        } catch (IOException e) {
            showMessage("메시지 전송 실패: " + e.getMessage());
        }
    }

    public void sendAnswer(String answer) {
        if (!answer.trim().isEmpty()) {
            sendMessage("ANSWER:" + answer);
        }
    }

    public JFrame getMainFrame() {
        return this;
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (currentRoomId == -1) {
                lobbyGUI.displayMessage(message);
            } else {
                gameGUI.displayMessage(message);
            }
        });
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            disconnect();
        }
        super.processWindowEvent(e);
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 3; i++) {
            final int clientNum = i;
            SwingUtilities.invokeLater(() -> {
                String playerName = JOptionPane.showInputDialog(null,
                        String.format("플레이어 %d 이름을 입력하세요:", clientNum),
                        "퀴즈 게임",
                        JOptionPane.QUESTION_MESSAGE);

                if (playerName != null && !playerName.trim().isEmpty()) {
                    QuizClient client = new QuizClient(playerName.trim());
                    client.setLocation(100 * clientNum, 100 * clientNum);
                    client.setVisible(true);
                    client.connect("localhost", 9999);
                }
            });
        }
    }
}