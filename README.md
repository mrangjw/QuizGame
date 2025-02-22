# 🎮 MultiQuizBattle

> GPT API를 활용한 실시간 멀티플레이어 퀴즈 게임 플랫폼

## 📝 프로젝트 소개

닌텐도 DS의 '한국인의 상식력 DS'를 모티브로 제작된 멀티플레이어 퀴즈 게임입니다. OpenAI의 GPT API를 연동하여 실시간으로 다양한 카테고리의 문제를 생성할 수 있도록 개선하였으며, 동점자 발생 시 자동으로 가위바위보 모드로 전환되는 독특한 승부 결정 시스템을 구현했습니다.

## ✨ 주요 기능

### 🎲 실시간 멀티플레이어
* 최대 6명 동시 게임 참여
* 실시간 점수판 및 순위 업데이트
* Socket 통신 기반 실시간 상호작용

### 🤖 동적 퀴즈 생성
* GPT API 활용한 자동 문제 생성
* 다양한 카테고리별 퀴즈 선택
* 로컬 퀴즈 데이터베이스 지원

### 🎯 게임 시스템
* 실시간 타이머 및 점수 관리
* 남은 시간 보너스 점수 시스템
* ✌️ 가위바위보 동점자 처리 시스템

## 🛠 기술 스택

### 💻 Backend
* Java
* Socket 통신
* OpenAI GPT API
* DTO/DAO 패턴

### 🎨 Frontend
* Java Swing GUI
* 멀티스레드 처리
* 소켓 클라이언트

## 🏗 시스템 구조

### 🖥 클라이언트 (QuizClient)
* 사용자 인터페이스
* 패널 컴포넌트
  * GamePanel: 게임 진행
  * LobbyGUI: 대기실 화면
  * RPSPanel: 가위바위보 화면
  * GameResultPanel: 결과 화면

### ⚙️ 서버 (QuizServer)
* Socket 통신 관리
* 게임 매니저 (GameManager)
* 데이터 처리 (QuizDataDAO)
* 게임 모델 관리 (Room, Quiz, RPS)

## 👥 팀 구성 및 역할

### 👨‍💻 양정우 (55%)
* GUI 시스템 구현
* Java Swing 기반 UI 디자인
* 실시간 게임 진행 화면 개발
* 멀티플레이어 동기화 처리

### 👨‍💻 성규현 (45%)
* GPT API 연동
* 동적 퀴즈 생성 시스템 구현
* DTO/DAO 설계 및 구현
* 서버-클라이언트 데이터 통신 구현

## 🚀 실행 방법

```bash
# 서버 실행
java -jar QuizServer.jar

# 클라이언트 실행
java -jar QuizClient.jar
