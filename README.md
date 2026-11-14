# 달리는 오락관

화면을 보지 않고 목소리만으로 운전자와 동승자가 함께 노는 차량용 게임 사회자(MC) 앱

앱이 사회자가 되어 인원 등록, 순서 정하기, 타이머, 정답 판정, 규칙 변주, 시상식까지 음성으로 진행합니다.
주행 중에는 음성 전용으로 동작하도록 설계했습니다 (Music/Audio 카테고리, Distraction Optimized 선언).

## 주요 기능

앱을 켜면 MC가 먼저 "몇 명이 게임하나요?"라고 묻습니다. 2명 이상이면 함께 하는 게임(끝말잇기, 초성 퀴즈, 지나는 동네 퀴즈)을, 1명이면 "혼자서 운전하시는군요! 지루하지 않으시도록 저랑 같이 게임을 해 봐요"와 함께 1인 게임(스무고개, 초성 퀴즈, 지나는 동네 퀴즈, 아재개그 퀴즈)을 화면과 음성으로 안내하고, 번호나 이름을 말해 고릅니다. 게임이 끝나면 "한 판 더 / 다른 게임 / 그만"을 묻습니다.

| 게임 | 내용 |
| --- | --- |
| 끝말잇기 | 차례 호명, 제한 시간, 두음법칙 판정, 중복 단어 판정, 4연속 성공 시 새 규칙 제안 (3글자만, '가'로 시작, 스피드 라운드) |
| 초성 퀴즈 | 분야와 초성을 음성으로 출제, 오답이면 힌트 후 한 번 더 기회 |
| 지나는 동네 퀴즈 | 현재 주소(NaviHelper) 또는 좌표(FusedLocation)로 지역을 판정해 객관식(3지선다)·OX 문제 출제, 위치가 없으면 전국 퀴즈 |
| 혼자 드라이브 (1인) | 동승자가 없을 때 MC가 문제를 내는 음성 스무고개(예/아니요 질문, 정답 이름을 말하면 판정, 10번째 질문에 힌트). 끝나면 "한 판 더 할까요, 다른 게임 할까요?"를 묻고, 다른 게임을 원하면 초성 퀴즈·지나는 동네 퀴즈·아재개그 퀴즈를 화면과 음성으로 안내해 음성으로 고르게 함. 마지막에 졸음 예방 휴식 안내 |

공통 MC 기능
- 운전자 차례에는 주행 중일 때 제한 시간 +4초
- 목적지까지 10분 이내로 남으면 마지막 라운드로 정리하고 시상식 진행
- 길 안내 음성 등으로 오디오 포커스를 잃으면 자동 일시정지
- "그만" 음성 명령으로 즉시 종료
- 화면 문구와 읽는 문장을 분리해 OX는 "오엑스", 369는 "삼육구", 3글자는 "세 글자"로 읽음

## Pleos Connect SDK 사용

| SDK | 버전 | 용도 |
| --- | --- | --- |
| TextToSpeech | 3.3.1 | MC 진행 멘트, 문제 낭독 (ON_DEVICE 모드) |
| SpeechToText | 3.3.1 | 탑승자 답변, 이름, 인원 수, 명령어 인식 (ON_DEVICE 모드) |
| NaviHelper | 3.0.5 | 목적지까지 남은 시간(onDrivingInfo), 현재 주소(onCurrentLocationInfo) |
| FusedLocation | 1.1.3 | 차량 좌표로 지역 판정 |
| AAOS CarUxRestrictions | - | 주행 규제 상태 확인, 주행 중 화면 정보 축소 |

음성 인식은 Pleos SpeechToText SDK만 사용합니다. 차량(AAOS)에서는 외부 음성 인식 서비스로 대체하지 않으며, Pleos 음성 서비스가 없는 일반 휴대폰에서만 Android 기본 TTS/SpeechRecognizer로 대체 실행됩니다.

## 실행 방법

1. Android Studio에서 Pleos Connect Emulator(v2.1.0, API 34)를 설치하고 AVD를 만듭니다.
   설치 방법: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
2. 에뮬레이터를 켠 뒤 저장소의 `apk/` 폴더(또는 Releases)에 있는 APK를 설치합니다.
   ```
   adb install -r HwangHyunsang_DallineunOrakgwan_release.apk
   ```
3. 앱 목록에서 "달리는 오락관"을 실행하고 마이크 권한을 허용합니다.
4. 앱을 켜면 MC가 인원부터 물어보고 음성으로 진행합니다 (홈 화면의 '시작하기'로 다시 시작).
5. (자동 시연) `adb shell am start -n kr.unseen.dallineun/.MainActivity --ez demo true --es scenario family` (혼자: `--es scenario solo`, 가족 게임 지정: `--es game WORD_CHAIN|CHOSUNG|REGION`)

### 시연 모드 (마이크 없이 확인)
홈 화면 오른쪽 아래 "시연 모드"를 켜고 가족 시연 또는 혼자 시연을 누르면, 탑승자 답변을 내장 대본으로 자동 입력합니다.
주행 중 상태, 도착까지 80분, 천안 부근 위치를 시뮬레이션하므로 에뮬레이터에서 전체 흐름을 바로 확인할 수 있습니다.

## 소스에서 빌드

- JDK 17, Android SDK 35, Gradle 8.10.2 (wrapper 포함), Android Gradle Plugin 8.7.3, Kotlin 1.9.25
- minSdk 28, targetSdk 34
- Pleos Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (settings.gradle.kts에 등록됨)

```
chmod +x gradlew   # 웹 업로드로 실행 권한이 빠졌을 수 있음
./gradlew assembleRelease
./gradlew testReleaseUnitTest   # 게임 4종 전체 흐름 시뮬레이션(GameFlowSimulationTest) + 한글 처리(두음법칙, 초성, 조사), 스무고개 질문 해석, 읽기용 문장 변환, 객관식·OX 답 인식, 지역 판정 단위 테스트
```

## 테스트 환경 및 화면 규격

- 개발에 사용한 에뮬레이터: Pleos Connect Emulator v2.1.0 (system image: Pleos Connect v2.1.0 APIs arm64, Android API 34)
- 화면 규격: 2560×1440, 160dpi (Pleos Connect v2.1.0 AVD 기본 규격), landscape
- 게임 흐름 검증: `./gradlew testReleaseUnitTest` 의 GameFlowSimulationTest가 게임 4종을 시연 대본으로 끝까지 실행
- 화면: Pleos Connect Display Layout 가이드에 따라 landscape(2개 window 확장)와 portrait(1개 window) 레이아웃을 모두 지원
- 디자인: Pleos Connect UI Design Guide 기준 다크 테마, Asta Sans 서체, 대비 4.5:1 이상, 76dp 이상 큰 버튼, 주행 중 애니메이션 없음

## APK 위치

APK는 저장소 안에 함께 들어 있습니다.

- 저장소 경로: [`apk/HwangHyunsang_DallineunOrakgwan_release.apk`](apk/HwangHyunsang_DallineunOrakgwan_release.apk) (release 서명, 약 21MB)
- 원래 파일명은 `황현상_달리는오락관_release.apk`이며, GitHub 한글 파일명 제한 때문에 영문으로 표기했습니다.
- 같은 파일을 Release v0.1.0에도 올려 두었습니다: https://github.com/unseen1127/dallineun-orakgwan/releases/tag/v0.1.0
- 포함 ABI: arm64-v8a, x86_64 (Pleos Connect 차량·에뮬레이터와 64비트 휴대폰용)

## 매니페스트 주요 선언

- `android:appCategory="audio"`
- `<meta-data android:name="distractionOptimized" android:value="true" />`
- 권한: `pleos.car.permission.TTS_SERVICE`, `pleos.car.permission.STT_SERVICE`, FusedLocation 바인딩, 마이크, 위치

## 콘텐츠 출처

- 지역 퀴즈와 초성 퀴즈는 시제품용 내장 문항입니다. 서비스 단계에서 공공데이터 기반 문항 생성과 검수 절차로 교체합니다.
- 서체: Asta Sans (SIL Open Font License)
