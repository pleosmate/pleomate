# SDK 검증 — 초기 환경 확인

- 최초 확인일: 2026-10-25 · 최신 요약 갱신 2026-10-31
- 상태: 시간순 검증 기록. 오래된 섹션의 ‘다음 구현 대상’ 결론은 뒤 날짜의 미션 검증보다 우선하지 않는다.
- 관련 문서: [PRD](./pleos-vehicle-companion-prd.md) · [개발계획서](./pleos-vehicle-companion-development-plan.md)

## 확인된 사실

| 항목 | 로컬 근거 | 결과 |
| --- | --- | --- |
| Android 프로젝트 | `app/build.gradle.kts` | Kotlin Activity, namespace `com.pleos.mate`, compile/target SDK 37, min SDK 28 |
| Vehicle SDK | `gradle/libs.versions.toml`, `app/build.gradle.kts` | `ai.pleos.playground:Vehicle:2.3.2` 의존성 선언, 로컬 Gradle 캐시에 AAR 존재 |
| 기존 검증 코드 | `MainActivity.kt`, Manifest | Vehicle 초기화·Door 조회·해제 코드, 문 조회 권한 선언. 실행 성공을 확인한 것은 아님 |
| Odometer API | 캐시 AAR의 `classes.jar`를 `javap`로 확인 | 현재 트립 거리·시간 capability, get, register/unregister 메서드 존재 |
| 반환 콜백 형식 | 동일 바이너리 | 거리 `Float`, 시간 `Integer`의 JVM 콜백 시그니처 확인. Kotlin nullability·단위·리셋 의미는 별도 확인 필요 |
| Pleos AVD | 로컬 `.android/avd` 디렉터리 | `Pleos_Connect_v2.1.0` 설정 존재. 실제 기동·서비스 버전은 미확인 |

## 실행 검증 제약

`adb devices -l` 실행 시 ADB 서버가 로컬 TCP 5037 리스너를 생성하지 못했다. 오류는 `Operation not permitted`이며, 이 세션의 실행 제약으로 연결 기기와 에뮬레이터 상태를 확인할 수 없었다. 이를 SDK 또는 앱 오류로 해석하지 않는다.

최초 환경 확인 뒤 Window·Odometer debug 검증 화면을 추가했다. Odometer 제품 흐름은 연결했으나, 이 문서 작성 환경에서 앱을 새로 빌드·실행하거나 AVD의 Odometer 값을 관찰하지 못했다.

## 이어서 확인할 순서

1. ADB 사용 가능한 개발 환경에서 Pleos AVD 기동·장치 연결·Vehicle Service 버전을 확인한다.
2. SDK 2.3.2 소스의 `CAR_DRIVE`, distance m, driving-time min 선언을 확인한다. 실제 차량 서비스가 제공하는 범위는 capability와 실행 값으로 별도 판단한다.
3. debug 빌드의 개발 도구 탭에서 **Odometer SDK 검증**을 연다. (이 기록 작성 당시 경로는 더보기 메뉴였다.)
4. 가상 콘솔 값 변경과 구독 해제를 확인하고, 앱 백그라운드·재실행·트립 초기화·재연결을 비교한다.
5. 주행 제한 전환과 값 유지 여부를 확인한다. 절차와 현재 구현 경계는 [이동 기억 기록](./design/journey-memory.md)을 따른다.

단위와 여정 경계는 아직 미확정이다. 와이어프레임의 거리·시간 표시는 실제 SDK 검증 결과가 아니다.

## Window 검증 화면 준비 결과

대상 AVD는 사용자가 구성한 **Pleos Connect v2.1.0**이다. SDK 2.3.2와 AVD의 이름은 별개 버전이며, 실제 Vehicle Service 버전과 호환성은 기동 후 확인한다.

- `app/src/debug/java/com/pleos/mate/feature/developer/presentation/WindowValidationActivity.kt`: 지원·접근 범위 확인 → 원시값 조회 → 갱신 구독 → 해제.
- `app/src/debug/res/`: 큰 버튼과 스크롤 가능한 결과 로그를 제공하는 네이티브 Android 레이아웃·문자열.
- `app/src/debug/AndroidManifest.xml`: 검증 Activity와 `pleos.car.permission.CAR_WINDOWS` 조회 권한을 debug에만 선언.
- 기존 main Door 테스트는 유지한다. release 빌드에는 새 검증 화면과 debug 권한이 포함되지 않는다.

공식 [권한 문서](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)와 로컬 AAR의 `@RequiresPermission`에서 Window 조회·구독의 `CAR_WINDOWS`를 대조했다. 선언이 실제 권한 승인·차종 지원을 뜻하지 않는다.

배포 바이너리에서 사용한 메서드는 `checkWindowPositionCapabilities`의 CapabilityConfig 반환 오버로드, `getCurrentWindowPosition`, `registerWindowPosition`, `unregisterWindowPosition`이다. 조회 대상은 `ROW_1_LEFT`다. capability의 읽기 접근과 해당 영역이 확인된 후 조회·구독 버튼을 활성화한다.

이 도구는 **에뮬레이터 전용**이다. 원시값을 열림 비율·미션 성공·경험치로 변환하지 않는다. UX Restrictions 연동이 완료된 제품 화면도 아니다. 실제 차량 사용을 전제로 하지 않는다.

### 확인한 것과 확인하지 못한 것

| 확인 항목 | 결과 |
| --- | --- |
| SDK·AndroidX 실제 바이너리 대상 Kotlin 소스 타입 검사 | 통과. 컴파일 검사용 R ID를 별도 임시 생성했으며 전체 앱 빌드가 아님 |
| Android debug XML과 리소스 | XML 파싱 및 Android build-tools 36.0.0 `aapt2 compile` 통과. 리소스 링크·APK 패키징은 별도 |
| 와이어프레임 미션 판정·보상 흐름 | DOM 대역 검사 통과, Safari에서 시연 흐름 확인. 실제 SDK 결과가 아님 |
| `./gradlew --offline :app:assembleDebug` | wrapper 캐시의 `.zip.lck` 쓰기가 `Operation not permitted`로 실패. 컴파일 태스크 진입 전 환경 제약 |
| `adb devices -l` | TCP 5037 서버 리스너 생성 거부. 기기 연결·AVD 기동·SDK 데이터 수신 미확인 |
| 이미 열림·창문 변화·구독 해제 후 콜백·재연결 | Android 실행 환경에서 미검증 |

### Pleos AVD에서 이어 실행할 절차

ADB와 Gradle이 동작하는 개발 환경에서 수행한다. 아래 명령은 실행 안내이며 성공 기록이 아니다.

1. Android Studio Device Manager에서 기존 **Pleos Connect v2.1.0**을 실행한다.
2. `adb devices -l`로 해당 에뮬레이터 serial을 확인한다. 여러 기기가 있으면 모든 ADB 명령에 해당 serial을 지정한다.
3. Android Studio에서 debug APK를 빌드하거나 프로젝트에서 다음을 실행한다.

```sh
./gradlew :app:assembleDebug
```

4. 아래 `EMULATOR_SERIAL`을 확인한 실제 serial로 바꾸어 설치·실행한다.

```sh
adb -s EMULATOR_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s EMULATOR_SERIAL shell am start -n com.pleos.mate/.feature.developer.presentation.WindowValidationActivity
adb -s EMULATOR_SERIAL logcat -s WindowProbe
```

5. 지원·접근 범위 확인 → 현재 원시값 조회 → 구독 순으로 실행한다. 가상 콘솔에서 해당 창문 값을 바꾸어 조회값과 UPDATE 로그를 비교한다. 앱은 창문 쓰기 API를 호출하지 않는다.
6. 닫힘·부분 열림·열림·이미 열림의 원시값과 bounds를 기록한다. 단위·방향·완료 문턱은 이 결과와 API 의미를 대조한 뒤 결정한다.
7. 구독 해제 후 새로운 갱신이 계속 오는지, 백그라운드 복귀 후 지원 재확인이 필요한지, 재실행·연결 복구 결과를 기록한다. 늦게 도착한 콜백과 새 구독 결과를 구분한다.
8. 권한 거부·미지원·조회 실패는 별도 결과로 남긴다. 실행 가능성이 확보되면 제품의 CarUxRestrictions 연동과 미션 판정을 구현한다.

위 빌드·ADB 제약은 에이전트 실행 환경의 결과다. 이후 사용자 환경에서 앱을 실행하여 확인한 결과는 아래에 별도로 기록한다.

## 사용자 환경의 Window 관찰 결과

- 보고일: 2026-10-25
- 환경: 사용자가 설정한 Pleos Connect v2.1.0 AVD, Window 검증 화면, Virtual Console의 Front Left.
- 근거 수준: 사용자의 실행 결과 보고. 에이전트가 로그·capability bounds·설치 APK 버전을 직접 대조한 결과는 아니다.

| 콘솔 설정·조작 | 사용자가 확인한 SDK 원시값 |
| --- | --- |
| Front Left 0% | `0` |
| Front Left 100% | `10` |
| 설정값 증가 과정 | `0 → 2 → 4 → 7 → 9 → 10` 변화 관찰 |
| 설정값 감소 | 원시값도 감소 |

이번 환경에서 창문 설정 변경에 대응하는 값 수신을 확인했다. 콘솔의 0~100%와 SDK 원시값의 끝점 0·10이 대응한다. 모든 중간값의 선형 변환이나 모든 차종의 고정 범위를 증명한 것은 아니다. 원시값을 그대로 퍼센트로 표시하지 않는다.

남은 확인은 실제 열림·닫힘 방향, capability bounds, 이미 충족 상태 처리, 구독 해제·중단·재연결, 주행 제한, 미션 보상 중복 방지다. 특히 콘솔 퍼센트의 끝점 확인과 물리적 열림 방향 확인은 구분한다. 특정 원시값을 미션 완료 문턱으로 아직 확정하지 않는다.

당시 결론은 **사용자 환경의 Window 값 수신·변화 확인, 실제 미션 판정과 성장 연결은 다음 구현·검증 대상**이었다. 이후 확인은 [SDK 미션 카탈로그](./design/sdk-mission-catalog.md)의 날짜별 기록을 참조한다.

## 2026-10-27 Odometer 구현 및 첫 AVD 확인

SDK 2.3.2 소스 JAR에서 현재 트립 주행거리(`Float?`, m), 주행 시간(`Int?`, min)과 `pleos.car.permission.CAR_DRIVE` 선언을 확인했다. 제품은 사용자가 명시적으로 관찰을 시작한 뒤 paired read의 차이만 임시 초안으로 저장한다. 감소/reset, 잘못된 값, paired read 실패, 15초가 넘는 관찰 공백에서 구간을 닫는다. 이동 기억은 성장 기록과 분리했고 이동 XP는 아직 적용하지 않았다.

Android Studio에서 debug APK 빌드·설치가 성공했고, 홈 → 개발 도구 → Odometer SDK 검증 화면을 열었다. 조회·구독 시작 후 **거리 capability 연결 실패**가 화면에 표시됐다. 당시 화면은 실패 메시지만 표시해 SDK 예외 클래스·상세 메시지는 수집하지 못했다. 상세 오류를 검증 로그로 보내도록 코드를 보강해 다시 빌드·설치했으나, 보강된 진단값은 아직 화면에서 확인하지 못했다. 따라서 AVD 지원 여부나 권한 문제라고 단정하지 않는다. 주행 시간 조회, 원시값·Virtual Console 대조, 트립 reset 경계, 앱 종료·재연결, 실차 지원은 계속 미확인이다. 재현 절차는 [이동 기억 기록](./design/journey-memory.md)에 정리했다.

## 2026-10-31 현재 적용 범위와 미완료 검증

앞선 기록은 당시의 관찰로 보존한다. 현재 제품 반영 범위는 [PRD 현재 구현 기준](./pleos-vehicle-companion-prd.md#2026-10-31-현재-구현-기준-정정)과 [SDK 미션 카탈로그](./design/sdk-mission-catalog.md)를 기준으로 읽는다.

- **AVD·Virtual Console에서 제품 흐름까지 확인:** Window 첫 바람 자동 +10 XP, Door 일일 +5 XP 및 주간 진행 표시, Vehicle 조회 10종의 유효값 확인·최초 보상, 한마디 직접 입력·기억 저장, 장소 권한·위치 확인, SDK 미지원/목적지 없음 실패 차단.
- **계산·렌더 검증:** 일일·주간 중복 방지, 주간 서로 다른 3일 계산, 여섯 캐릭터×Lv.1/5/15 외형 렌더 검사. 이는 실제 3일 대기나 사용자 선호 검증이 아니다.
- **추가 AVD 실행 필요:** Door 서로 다른 3일 완료 후 +15 XP, 추억 서로 다른 3일 후 주간 +15 XP, 목적지 150m 이내 성공 및 +10 XP, TTS 후 실제 발화·첫 인사 보상 +5 XP, 배지 13종 획득·공유 전부.
- **실차·상용 검증 미완료:** 차종별 capability·권한, Odometer current-trip 지원·단위, 주행 제한 정책 승인, 앱 마켓 심사·개인별 데이터 분리.

사용자가 Virtual Console에서 관찰한 결과는 사용자 실행 보고로 구분한다. 현재 로컬 저장 구조에는 계정·클라우드 동기화가 없다. 제품 상태와 수익화/사용자 수 가정은 [문서 안내](./README.md) 및 [사업화 계획](./business-validation-plan.md)를 따른다.


## 2026-10-26 첫 미션 연결

MainActivity에 P 기어/UX 제한 확인과 Window 읽기 기반 첫 미션을 연결했다. 현재 에뮬레이터용 raw 2..10을 검증용 완료 범위로 정했으며 다른 차량으로 일반화하지 않는다. UI의 주차 사용 가능 표시와 창문 대기 진입은 관찰했고, 이후 사용자가 가상 콘솔 조작 후 첫 미션 완료·보상 표시가 정상적으로 나온다고 보고했다. 재실행 복원·중복 방지·제한 전환은 별도 실기 확인 항목이다. [구현 및 검사 범위](./design/wind-mission.md)를 따른다. 위 이전 기록의 ‘문턱 미확정’은 이 구현 이전 상태다.
