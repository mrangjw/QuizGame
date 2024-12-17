package client;

import client.panels.GamePanel;
import client.panels.GameResultPanel;
import client.panels.RPSPanel;
import model.RPS;
import model.Room;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class QuizClient extends JFrame {
    private GamePanel gamePanel;
    private LobbyGUI lobbyGUI;
    private RPSPanel rpsPanel;
    private GameResultPanel resultPanel;
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

    public String getPlayerName() {
        return this.playerName;
    }

    private void initComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        lobbyGUI = new LobbyGUI(this);
        gamePanel = new GamePanel(this);
        rpsPanel = new RPSPanel(playerName);
        resultPanel = new GameResultPanel();

        // GamePanel 이벤트 처리
        gamePanel.addPropertyChangeListener("gameResult", evt -> {
            @SuppressWarnings("unchecked")
            List<Map.Entry<String, Integer>> sortedScores =
                    (List<Map.Entry<String, Integer>>) evt.getNewValue();
            cardLayout.show(mainPanel, "RESULT");
            resultPanel.displayResults(sortedScores);
        });

        gamePanel.addPropertyChangeListener("gameRPS", evt -> {
            String message = (String) evt.getNewValue();
            String playerList = message.substring(10);
            cardLayout.show(mainPanel, "RPS");
            rpsPanel.updatePlayersStatus(Arrays.asList(playerList.split(",")), new ArrayList<>());
            rpsPanel.startTimer(10);
        });

        // RPS 패널 이벤트 처리
        rpsPanel.addPropertyChangeListener("choiceMade", evt -> {
            RPS.Choice choice = (RPS.Choice) evt.getNewValue();
            sendMessage("RPS_CHOICE:" + choice.name());
        });

        // 결과 패널 이벤트 처리
        resultPanel.addPropertyChangeListener("exitToLobby", evt -> {
            if ((Boolean) evt.getNewValue()) {
                leaveRoom();
                cardLayout.show(mainPanel, "LOBBY");
            }
        });

        resultPanel.addPropertyChangeListener("exitGame", evt -> {
            if ((Boolean) evt.getNewValue()) {
                leaveRoom();
                disconnect();
                dispose();
                System.exit(0);
            }
        });

        mainPanel.add(lobbyGUI, "LOBBY");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(rpsPanel, "RPS");
        mainPanel.add(resultPanel, "RESULT");

        cardLayout.show(mainPanel, "LOBBY");
    }

    private void initFrame() {
        setTitle("퀴즈 게임 - " + playerName);
        setSize(800, 600);
        add(mainPanel);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leaveRoom();
                disconnect();
                dispose();
                System.exit(0);
            }
        });
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
                gamePanel.clearChat();
            } else if (message.equals("USE_GPT")) {
                handleGPTChoice();
            } else if (message.equals("LOBBY:")) {
                currentRoomId = -1;
                cardLayout.show(mainPanel, "LOBBY");
            } else if (message.startsWith("START_RPS:")) {
                String playerList = message.substring(10);
                cardLayout.show(mainPanel, "RPS");
                rpsPanel.startTimer(10);
                rpsPanel.updatePlayersStatus(Arrays.asList(playerList.split(",")), new ArrayList<>());
            } else if (message.startsWith("RPS_STATUS:")) {
                handleRPSStatus(message);
            } else if (message.startsWith("RPS_RESULT:")) {
                handleRPSResult(message);
            } else if (message.startsWith("GAME_RESULT:")) {
                handleGameResult(message);
            } else {
                if (currentRoomId != -1) {
                    gamePanel.displayMessage(message);
                } else {
                    lobbyGUI.displayMessage(message);
                }
            }
        });
    }

    private void handleRPSStart(String message) {
        String playerList = message.substring(10);
        cardLayout.show(mainPanel, "RPS");
        rpsPanel.startTimer(10);
        rpsPanel.updatePlayersStatus(Arrays.asList(playerList.split(",")), new ArrayList<>());
    }

    private void handleRPSStatus(String message) {
        String[] parts = message.substring(11).split(";");
        List<String> readyPlayers = new ArrayList<>();
        List<String> waitingPlayers = new ArrayList<>();

        for (String player : parts) {
            if (!player.isEmpty()) {
                String[] playerInfo = player.split(":");
                if (playerInfo[1].equals("READY")) {
                    readyPlayers.add(playerInfo[0]);
                } else {
                    waitingPlayers.add(playerInfo[0]);
                }
            }
        }

        rpsPanel.updatePlayersStatus(readyPlayers, waitingPlayers);
    }

    private void handleRPSResult(String message) {
        String result = message.substring(11);
        JOptionPane.showMessageDialog(this, result, "가위바위보 결과", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleGameResult(String message) {
        String resultData = message.substring(12);
        String[] entries = resultData.split(";");
        List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>();

        for (String entry : entries) {
            if (!entry.isEmpty()) {
                String[] parts = entry.split(":");
                sortedScores.add(new AbstractMap.SimpleEntry<>(parts[0], Integer.parseInt(parts[1])));
            }
        }

        cardLayout.show(mainPanel, "RESULT");
        resultPanel.displayResults(sortedScores);
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

                Room room = new Room(roomId, roomName, hostName, maxPlayers, category, 5, 30);

                for (int i = 1; i < currentPlayers; i++) {
                    room.addPlayer("Player " + i);
                }

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

    public void createRoom(String roomName, String category, int maxPlayers, int problemCount, int timeLimit) {
        if (socket != null && !socket.isClosed() && out != null) {
            sendMessage(String.format("CREATE_ROOM:%s,%s,%d,%d,%d",
                    roomName, category, maxPlayers, problemCount, timeLimit));
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
            gamePanel.clearChat();
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

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (currentRoomId == -1) {
                lobbyGUI.displayMessage(message);
            } else {
                gamePanel.displayMessage(message);
            }
        });
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