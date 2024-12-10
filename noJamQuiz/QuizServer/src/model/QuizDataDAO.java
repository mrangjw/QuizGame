package model;

// QuizDataDAO.java
// 데이터베이스와의 상호작용을 담당하는 객체
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

// 문제들을 저장하고 관리하는 클래스
public class QuizDataDAO extends ArrayList<QuestionDTO> {
    /**
     * 지정된 키에 해당하는 퀴즈 데이터를 파일에서 읽어와 리스트에 저장하고,
     * 문제들을 랜덤하게 섞는다.
     * @param key 퀴즈 파일을 선택하기 위한 키
     * @return 파일을 성공적으로 로드하면 false, 실패하면 true를 반환
     */
    public boolean loadQuiz(int key) {
        String quizDataPath = "src/Data/Quiz" + key + ".dat"; // 퀴즈 데이터 파일 경로 설정
        File file = new File(quizDataPath);

        try {
            // 파일 존재 여부 확인
            if (!file.exists()) {
                System.out.println("예외 : 파일이 존재하지 않습니다: " + file.getAbsolutePath());
                return true;
            }

            // FileInputStream에 InputStreamReader를 통해 인코딩 지정
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            Scanner scn = new Scanner(isr);

            while (scn.hasNext()) {
                // 문제와 답을 읽어와서 리스트에 추가
                //짝수줄 문제, 홀수줄 정답.
                //문제와 정답을 읽어와서 Arraylist에 추가
                if (!scn.hasNext()) {
                    System.out.println("예외 : 파일 형식 오류 - 답이 없는 문제가 있습니다.");
                    scn.close();
                    return true;
                }
                QuestionDTO qDTO = new QuestionDTO(scn.nextLine(), scn.nextLine());
                //System.out.println(qDTO);
                this.add(qDTO);
            }
            scn.close();
            System.out.println("총 " + this.size() + "개의 문제를 로드했습니다.");
            System.out.println();
            Collections.shuffle(this); // 문제들을 랜덤하게 섞음
        } catch (FileNotFoundException e) {
            System.out.println("예외 : " + e);
            System.out.println("현재 디렉토리: " + System.getProperty("user.dir"));
            return true; // 파일 로드 실패 시 true 반환
        } catch (Exception e) {
            System.out.println("예외 : " + e);
            e.printStackTrace();
            return true; // 파일 로드 실패 시 true 반환
        }
        return false; // 파일 로드 성공 시 false 반환
    }
}