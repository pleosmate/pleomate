# PleosMate

PleosMate는 Pleos 차량의 주차 후 경험과 개인 기억을 동료 펫의 성장으로 연결하는 Android 앱입니다.

## 실행 방법

1. Android Studio에서 이 저장소를 열고 Gradle Sync를 완료합니다.
2. 에뮬레이터 또는 연결된 기기를 선택하고 `app` 구성을 실행합니다. 일반 개발에는 `debug` 변형을 사용합니다.

명령줄에서 빌드·설치할 수도 있습니다.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## 테스트 환경 및 필요한 설정

- Gradle Wrapper: Gradle 9.6.0
- Android Gradle Plugin: 9.4.1
- Gradle JDK: 25 (Gradle daemon JVM 설정 기준)
- Android SDK Platform 37 및 `local.properties`의 `sdk.dir` 설정이 필요합니다. Android Studio가 SDK 경로를 설정할 수 있습니다.
- 단위 테스트: `./gradlew :app:testDebugUnitTest`
- 기기/에뮬레이터 테스트: `./gradlew :app:connectedDebugAndroidTest`
- 차량 SDK 연동 기능을 확인하려면 Pleos Connect를 지원하는 차량 환경 또는 AVD와 필요한 차량 권한이 필요합니다. 일반 Android 에뮬레이터에서는 차량 SDK 기능을 검증할 수 없습니다.
- 서명된 release를 빌드하려면 프로젝트 루트의 Git 제외 파일 `local.properties`에 keystore 정보를 설정합니다. `storeFile`은 keystore 파일 경로입니다.

```properties
pleosmate.release.storeFile=/절대/경로/pleosmate.jks
pleosmate.release.storePassword=키저장소비밀번호
pleosmate.release.keyAlias=키별칭
pleosmate.release.keyPassword=키비밀번호
```

키 파일과 비밀번호는 저장소에 추가하지 마세요.

## APK 위치

현재 release APK는 저장소 루트의 [`apk/zumfriend_pleosmate_release.apk`](apk/zumfriend_pleosmate_release.apk)에 둡니다. Gradle release 빌드 산출물은 기본적으로 `app/build/outputs/apk/release/app-release.apk`에 생성되며, 보관용 파일을 갱신할 때 다음처럼 복사합니다.

```bash
./gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk apk/zumfriend_pleosmate_release.apk
```
