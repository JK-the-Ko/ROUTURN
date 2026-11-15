# ROUTURN

방문 계획이 예상치 못하게 틀어졌을 때, 원래 목적을 최대한 살리면서 대안을 골라 차량 내비게이션으로 바로 넘겨주는 차량용 앱입니다.

## 시나리오

- **01-A 주차장만 바꾸기**: 식당은 그대로 두고, 가까운 다른 주차장을 찾습니다.
- **01-B 식당 바꾸기**: 파스타 식사는 그대로 두고, 주차가 되는 다른 식당을 찾습니다.
- **02 먼저 들를 곳**: 식당은 최종 목적지로 두고, 화장실·카페·편의점·충전소 중 한 곳을 먼저 들릅니다.

## 흐름

1. 상황에 맞는 항목을 고릅니다.
2. 후보 두 곳 안에서 고릅니다. 확인되지 않은 정보(잔여면 등)는 '미확인'으로 표시합니다.
3. 바꿀 내용을 확인합니다.
4. 차량 내비게이션(NaviHelper)에 경로를 요청합니다.
5. 안내가 시작되면 남은 시간과 거리를 보여주고, 경유지 도착 같은 이벤트를 반영합니다.

## 실행 방법

1. 저장소를 받습니다.
   ```bash
   git clone https://github.com/JK-the-Ko/ROUTURN.git
   ```
2. Android Studio에서 프로젝트를 열고 Gradle 동기화를 끝냅니다.
3. Pleos Connect 에뮬레이터(API 34)를 실행합니다.
4. 실행 파일을 설치합니다. 아래 **APK** 섹션의 파일을 사용하거나, 직접 빌드할 수 있습니다.
   ```bash
   # 제출 APK 설치
   adb install -r "apk/KWAK&KO_ROUTURN_release.apk"

   # 직접 빌드 (release)
   ./gradlew :app:assembleRelease
   ```
5. 에뮬레이터에서 `ROUTURN` 앱을 실행합니다.

## 테스트 환경 및 필요한 설정

- **Android Studio** (Android Studio에 포함된 JDK 25 `jbr`로 빌드를 확인했습니다)
- **`minSdk` 34**, `targetSdk` 37, `compileSdk` 37
- **NaviHelper 3.0.5**: 의존성을 `https://nexus-playground.pleos.ai/repository/maven-releases/` 에서 받습니다. 이 저장소에 접근할 수 있어야 Gradle 동기화가 됩니다.
- **`local.properties`**: Android SDK 경로(`sdk.dir`)를 로컬에 설정해야 합니다. 이 파일은 저장소에 포함하지 않습니다.
- **에뮬레이터**: 아래 Pleos Connect 에뮬레이터 설정을 사용합니다.
- **서명**: `apk/` 의 APK는 개발용 디버그 키로 서명한 release 빌드입니다. 배포용 서명 키는 포함하지 않습니다.

```bash
./gradlew :app:assembleDebug          # 디버그 APK 빌드
./gradlew :app:testDebugUnitTest      # 단위 테스트
```

## 개발에 사용한 Pleos Connect 에뮬레이터

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | Pleos Connect (`Pleos_Connect_API34`) |
| Android 버전 | API 34 (Android Automotive, `pleos_car_emulator` x86_64) |
| 화면 해상도 | 1280 × 1024 px |
| 화면 밀도 | 160 dpi |
| 방향 | 가로(landscape) |
| 에뮬레이터 버전 | Android Emulator 37.2.12 |
| RAM | 4096 MB |

## 현재 상태

- 주차장, 식당, 화장실 데이터는 시연용 샘플입니다. 실제 데이터 소스는 아직 정하지 않았습니다.
- 주행 제한(CarUxRestrictions)은 아직 연결하지 않았습니다.
- 경로 시작 신호와 도착 이벤트는 에뮬레이터에서 일부만 확인했습니다.
- 디자인 색상과 크기는 임시 값입니다. 공식 디자인 가이드 값으로 바꿀 예정입니다.

## 폴더

- `app/src/main/java/com/example/drive_plan_b_service/routurn/` : 화면, 상태 전이, 후보 정렬, 내비게이션 연동
- `app/src/test/` : 상태 전이와 후보 정렬 단위 테스트
- `apk/` : 제출용 release APK
- `docs/기획안.md` : 기획 문서 (이전 README)
- `docs/Drive_Plan_B_Scenario.pdf` : 개발 핸드오프 문서

## APK

- 파일: [`apk/KWAK&KO_ROUTURN_release.apk`](apk/KWAK%26KO_ROUTURN_release.apk)
- 빌드: release (`:app:assembleRelease`, 최적화 및 축소 적용)
- SHA-256: `9055C055262A006CEEF0E0D3DD88213DD4B62800E282AC9966FFE330C36EAD1D`

무결성 확인:

```bash
sha256sum "apk/KWAK&KO_ROUTURN_release.apk"
```
