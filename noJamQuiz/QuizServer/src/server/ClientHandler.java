package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String playerName;
    private QuizServer server;

    public ClientHandler(Socket socket, QuizServer server) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Object received = in.readObject();
                if (received instanceof String) {
                    String message = (String) received;
                    handleMessage(message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!clientSocket.isClosed()) {
                server.printDisplay(playerName + " 플레이어와의 연결이 끊어졌습니다.");
            }
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String message) {
        if (message.startsWith("ID:")) {
            playerName = message.substring(3);
            server.printDisplay(playerName + " 플레이어가 접속했습니다.");
        } else {
            // 모든 메시지를 QuizServer의 handleMessage로 위임
            server.handleMessage(this, message);
        }
    }

    public void send(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            server.printDisplay(playerName + " 플레이어에게 메시지 전송 실패: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            server.removeClient(this);
        } catch (IOException e) {
            server.printDisplay(playerName + " 플레이어 연결 종료 중 오류 발생: " + e.getMessage());
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}