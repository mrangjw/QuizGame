package Server;

import model.Room;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class QuizServer extends JFrame {
    private int port;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Vector<ClientHandler> users;
    private Map<Integer, Room> rooms;
    private int roomIdCounter;

    private JTextArea t_display;
    private JButton b_connect;
    private JButton b_disconnect;
    private JButton b_exit;

    public QuizServer(int port) {
        this.port = port;
        this.users = new Vector<>();
        this.rooms = new HashMap<>();
        this.roomIdCounter = 1;
        buildGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void buildGUI() {
        setTitle("퀴즈 게임 서버");
        setSize(500, 600);
        setLayout(new BorderLayout(5, 5));

        // 디스플레이 패널
        t_display = new JTextArea();
        t_display.setEditable(false);
        t_display.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(t_display);
        scrollPane.setBorder(new CompoundBorder(
                new TitledBorder("서버 로그"),
                new EmptyBorder(5, 5, 5, 5)));
        add(scrollPane, BorderLayout.CENTER);

        // 컨트롤 패널
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        b_connect = new JButton("서버 시작");
        b_disconnect = new JButton("서버 중지");
        b_exit = new JButton("종료");

        b_connect.addActionListener(e -> startServer());
        b_disconnect.addActionListener(e -> stopServer());
        b_exit.addActionListener(e -> System.exit(0));

        controlPanel.add(b_connect);
        controlPanel.add(b_disconnect);
        controlPanel.add(b_exit);
        add(controlPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 포트 " + port + "에서 시작되었습니다.");

            // 클라이언트 수락을 위한 별도 스레드 생성
            acceptThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        users.add(clientHandler);
                        new Thread(clientHandler).start();  // 각 클라이언트를 별도 스레드로 관리
                        printDisplay("새로운 클라이언트가 연결되었습니다.");
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            printDisplay("연결 수락 오류: " + e.getMessage());
                        }
                        break;
                    }
                }
            });
            acceptThread.start();

            b_connect.setEnabled(false);
            b_disconnect.setEnabled(true);

        } catch (IOException e) {
            printDisplay("서버 시작 오류: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (acceptThread != null) {
                acceptThread.interrupt();
            }
            for (ClientHandler client : users) {
                client.disconnect();
            }
            users.clear();
            rooms.clear();
            printDisplay("서버가 중지되었습니다.");

            b_connect.setEnabled(true);
            b_disconnect.setEnabled(false);
        } catch (IOException e) {
            printDisplay("서버 중지 오류: " + e.getMessage());
        }
    }

    public synchronized Room createRoom(String name, String category, int maxPlayers, String hostName) {
        try {
            Room.QuizCategory quizCategory = Room.QuizCategory.fromKoreanName(category);
            Room room = new Room(roomIdCounter++, name, hostName, maxPlayers, quizCategory);
            rooms.put(room.getRoomId(), room);
            printDisplay(hostName + "님이 '" + name + "' 방을 생성했습니다.");
            broadcastRoomList();  // 모든 클라이언트에게 방 목록 업데이트 전송
            return room;
        } catch (IllegalArgumentException e) {
            printDisplay("방 생성 실패: " + e.getMessage());
            return null;
        }
    }

    public synchronized boolean joinRoom(int roomId, String playerName) {
        Room room = rooms.get(roomId);
        if (room != null && !room.isFull() && !room.isGameStarted()) {
            if (room.addPlayer(playerName)) {
                printDisplay(playerName + "님이 " + room.getRoomName() + " 방에 참가했습니다.");
                broadcastToRoom(roomId, playerName + "님이 입장하셨습니다.");
                broadcastRoomList();
                return true;
            }
        }
        return false;
    }

    public synchronized void leaveRoom(int roomId, String playerName) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.removePlayer(playerName);
            printDisplay(playerName + "님이 " + room.getRoomName() + " 방에서 나갔습니다.");
            broadcastToRoom(roomId, playerName + "님이 퇴장하셨습니다.");

            if (room.getPlayers().isEmpty() || playerName.equals(room.getHostName())) {
                rooms.remove(roomId);
                printDisplay(room.getRoomName() + " 방이 삭제되었습니다.");
            }

            broadcastRoomList();
        }
    }

    private void broadcastRoomList() {
        StringBuilder roomList = new StringBuilder("ROOM_LIST:");
        for (Room room : rooms.values()) {
            if (!room.isGameStarted()) {
                roomList.append(room.getRoomId()).append(",")
                        .append(room.getRoomName()).append(",")
                        .append(room.getCategory().getKoreanName()).append(",")
                        .append(room.getHostName()).append(",")
                        .append(room.getPlayers().size()).append(",")
                        .append(room.getMaxPlayers()).append(";");
            }
        }
        broadcastMessage(roomList.toString());
    }

    private void broadcastToRoom(int roomId, String message) {
        Room room = rooms.get(roomId);
        if (room != null) {
            // 서버 로그에 한 번만 출력
            printDisplay(String.format("[방 %d] %s", roomId, message));

            // 각 클라이언트에 한 번씩만 전송
            for (ClientHandler client : users) {
                if (room.getPlayers().contains(client.getPlayerName())) {
                    client.send("[방 " + roomId + "] " + message);
                }
            }
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : users) {
            client.send(message);
        }
    }

    public void removeClient(ClientHandler client) {
        users.remove(client);
        String playerName = client.getPlayerName();
        if (playerName != null) {
            // 클라이언트가 속한 방이 있다면 해당 방에서도 제거
            for (Room room : rooms.values()) {
                if (room.getPlayers().contains(playerName)) {
                    leaveRoom(room.getRoomId(), playerName);
                    break;
                }
            }
            printDisplay(playerName + " 플레이어가 퇴장했습니다. (현재 접속자 수: " + users.size() + "명)");
        }
    }

    public void handleMessage(ClientHandler client, String message) {
        if (message.startsWith("CREATE_ROOM:")) {
            try {
                String[] parts = message.substring(12).split(",");
                Room room = createRoom(parts[0], parts[1], Integer.parseInt(parts[2]), client.getPlayerName());
                if (room != null) {
                    client.send("JOIN_ROOM:" + room.getRoomId());
                } else {
                    client.send("방 생성에 실패했습니다.");
                }
            } catch (Exception e) {
                client.send("방 생성 실패: " + e.getMessage());
            }
        } else if (message.startsWith("JOIN_ROOM:")) {
            try {
                int roomId = Integer.parseInt(message.substring(10));
                if (joinRoom(roomId, client.getPlayerName())) {
                    client.send("JOIN_ROOM:" + roomId);
                } else {
                    client.send("방 참가 실패: 방이 가득 찼거나 게임이 시작되었습니다.");
                }
            } catch (Exception e) {
                client.send("방 참가 실패: " + e.getMessage());
            }
        } else if (message.startsWith("LEAVE_ROOM:")) {
            try {
                int roomId = Integer.parseInt(message.substring(11));
                leaveRoom(roomId, client.getPlayerName());
                client.send("LOBBY:");
            } catch (Exception e) {
                client.send("방 나가기 실패: " + e.getMessage());
            }
        } else {
            // 일반 메시지는 같은 방의 사용자들에게만 전달
            Room room = findPlayerRoom(client.getPlayerName());
            if (room != null) {
                broadcastToRoom(room.getRoomId(), message);
            }
        }
    }

    private Room findPlayerRoom(String playerName) {
        for (Room room : rooms.values()) {
            if (room.getPlayers().contains(playerName)) {
                return room;
            }
        }
        return null;
    }

    public void printDisplay(String message) {
        SwingUtilities.invokeLater(() -> {
            t_display.append(message + "\n");
            t_display.setCaretPosition(t_display.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new QuizServer(9999).setVisible(true);
        });
    }
}