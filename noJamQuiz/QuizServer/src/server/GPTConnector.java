package server;

import model.Quiz;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class GPTConnector {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "";  // OpenAI API 키 입력 필요

    public String generateQuiz(String category) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String requestBody = String.format("{" +
                    "\"model\": \"ft:gpt-4o-2024-08-06:whalewhale:quizmodel:Adg96qWt\"," +
                    "\"messages\": [" +
                    "{\"role\": \"system\", \"content\": \"당신은 퀴즈 출제자입니다. '질문: [문제] 답변: [답]' 형식으로 퀴즈를 출제해주세요.\"}," +
                    "{\"role\": \"user\", \"content\": \"%s 카테고리의 퀴즈를 하나만 출제해주세요.\"}" +
                    "]," +
                    "\"temperature\": 0.7" +
                    "}", category);

            System.out.println("Request Body: " + requestBody);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes());
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.out.println("Error Response: " + errorResponse.toString());
                }
                return null;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("GPT Response: " + response.toString());

                String jsonResponse = response.toString();
                int contentStart = jsonResponse.indexOf("\"content\": \"") + 11;
                int contentEnd = jsonResponse.indexOf("\",", contentStart);
                String content = jsonResponse.substring(contentStart, contentEnd)
                        .replace("\\n", "\n")
                        .replace("\"", "");  // 따옴표 제거

                System.out.println("Extracted Content: " + content);
                return content;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("GPT API Error: " + e.getMessage());
            return null;
        }
    }

    public Quiz parseQuizResponse(String response) {
        try {
            System.out.println("Parsing Quiz Response: " + response);

            String[] parts = response.split("\n");
            String question = "";
            String answer = "";

            for (String part : parts) {
                part = part.trim();  // 앞뒤 공백 제거
                if (part.startsWith("질문:")) {
                    question = part.substring("질문:".length()).trim();
                    System.out.println("Found Question: " + question);
                } else if (part.startsWith("답변:")) {
                    answer = part.substring("답변:".length()).trim();
                    System.out.println("Found Answer: " + answer);
                }
            }

            System.out.println("Final Question: " + question);
            System.out.println("Final Answer: " + answer);

            if (!question.isEmpty() && !answer.isEmpty()) {
                Quiz quiz = new Quiz(
                        question,
                        answer,
                        Quiz.QuizType.SHORT_ANSWER,
                        "AI 생성 퀴즈"
                );
                quiz.setTimeLimit(20);
                quiz.setPoints(10);
                return quiz;
            }

            return null;
        } catch (Exception e) {
            System.out.println("Quiz Parsing Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
