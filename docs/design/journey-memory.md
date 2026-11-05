# Odometer 이동 기억 구현과 검증

- 작성일: 2026-10-27
- 상태: 구현·APK 설치 완료. 현재 트립 거리 capability 미지원은 확인했고, 누적 거리 대체값은 Pleos Connect Virtual Console에서 수신 확인. 주행 시간과 실차 지원은 미검증
- 기준: [PRD v0.8](../pleos-vehicle-companion-prd.md) · [개발계획서 v0.7](../pleos-vehicle-companion-development-plan.md)

## 제품 흐름

1. 사용자가 홈의 **다음 이동 함께하기**를 누른다. 앱은 주차·UX 상태가 허용될 때만 시작한다.
2. Odometer 현재 트립 거리와 주행 시간을 capability 확인 후 읽고 구독한다.
3. 첫 유효 paired read를 baseline으로 삼는다. 이후 거리 차이는 m, 주행 시간 차이는 SDK가 반환한 정수 min으로 계산한다.
4. 미터·분 값 감소/초기화, 잘못된 값, 짝이 맞지 않는 read, 15초 넘는 확인 공백, 시계 역행을 만나면 그 지점에서 관찰을 끝낸다. 마지막 유효 prefix를 유지하고 알 수 없는 구간을 연결하지 않는다.
5. P 복귀 또는 사용자의 종료 동작에서 마지막 paired read를 요청한다. 확인된 값이 있으면 홈에 초안을 보여주고 저장·건너뛰기·삭제를 선택할 수 있다.
6. 저장한 이동 기억이 다음 홈 재회의 인사에 반영된다. 기록 조작은 Window 미션의 기존 경험치에 영향을 주지 않으며, 이동 경험치도 아직 지급하지 않는다.

관찰은 명시적으로 시작한 현재 앱 프로세스 동안만 지속한다. 프로세스 종료 후 구독·baseline은 복구하지 않고, 디스크에 마지막으로 원자 저장된 관찰 구간만 주차 후 확인할 수 있다. 앱이 꺼져 있던 이동이나 구독 공백을 전체 여정이라고 표시하지 않는다. 백그라운드 서비스나 시동 자동 실행은 구현하지 않았다.

저장 초안·기억·처리 완료 ID는 `files/journeys-v1`에 한 번에 원자 교체 저장한다. 앱 재실행 후에도 미처리 초안은 한 번만 저장 또는 건너뛸 수 있다. 삭제해도 완료 ID는 남아 동일 관찰 구간이 다시 생기는 것을 막는다. `GrowthStore`와 파일을 분리해 이동 저장이 기존 성장 기록을 수정하지 않는다.

## SDK 근거와 제한

[공식 Odometer API 문서](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/odometer/entry)와 캐시된 Vehicle SDK 2.3.2 소스 JAR에서 다음을 확인했다.

- `getTripCurrentDrivingDistance` / `DistanceDrivenListener`: nullable `Float`, 미터.
- `getTripCurrentDrivingTime` / `DrivingTimeListener`: nullable `Int`, 분.
- 두 capability 확인, 조회, 구독 API에 `pleos.car.permission.CAR_DRIVE`가 선언되어 있으며 앱 main manifest에 권한을 선언했다.
- 이 선언은 Pleos Connect v2.1.0 서비스에서 capability·권한 승인·가상 콘솔 값 연동·트립 초기화 시점을 증명하지 않는다. 현재 트립 경계가 차량/서비스에서 언제 초기화되는지도 아직 실측 전이다.

API 두 값을 최대 1초 안에 짝지어 받고 5초 heartbeat로 새 paired read를 요청한다. 각 read 쌍의 응답 시각 차이는 1초 이하여야 한다. 수신 실패·5초 초과 read·지원 불가면 마지막 확정 구간에서 멈춘다. SDK 등록 콜백은 함께 구독하되, 쌍이 맞는 `get` 결과만 기억 계산에 사용한다.

## Android 검증 방법

Pleos Connect v2.1.0 에뮬레이터에서 debug 빌드의 **개발 도구 탭 → Odometer SDK 검증**을 연다. (초기 기록의 더보기 경로는 현재 UI에서 변경되었다.) 이 화면은 capability·listener·paired read 값과 시간 정보를 표시하며 기억이나 경험치는 저장하지 않는다. 수신 중지 또는 화면을 나가면 listener를 해제한다.

1. **조회·구독 시작**을 누르고 두 capability가 읽기 가능하다고 나오는지 확인한다.
2. 현재 거리(m)·주행 시간(min)의 기준값과 update 로그를 기록한다.
3. Virtual Console에서 현재 트립 입력을 바꾸어 두 SDK 값이 각각 어떻게 반응하는지 비교한다. 콘솔 UI에 전체 누적거리만 있다면 현재 트립 API의 매핑으로 단정하지 않는다.
4. 값 증가, 변화 없음, 앱 백그라운드·복귀, 수신 중지 후 재시작, 트립 reset, 에뮬레이터 재시작을 각각 기록한다. 재시작 전후 값이 달라도 실제 트립 초기화 의미는 서비스 동작과 대조한다.
5. 검증 화면에서 나오면 callback이 멈추는지 확인한다. 실차 일반화는 별도다.

제품 흐름을 확인하려면 주차 중 홈에서 **다음 이동 함께하기** → 검증 환경에서 이동 값 변화 → P로 복귀 → 초안 거리·분 확인 → 저장 → 앱 재실행 후 최신 기억·재회 인사를 확인한다. 숫자가 갱신되지 않으면 전체 여정으로 추정하지 말고, 먼저 위 검증 화면에서 capability·원시값·update를 확인한다.

## 구현 파일과 검사 결과

- `Journey.kt`: 유효 sample, 관찰 interval 판정, 처리 완료 ID 규칙.
- `JourneyStore.kt`: pending·saved·handled 원자 저장 및 부팅 복원.
- `OdometerObserver.kt`: SDK capability, paired read, listener, heartbeat, 정지와 실패 처리.
- `JourneyViewModel.kt`: 프로세스 생존 중 관찰·P 복귀·비동기 저장을 연결.
- `JourneyCard.kt`: 홈 이동 카드, draft 확인, 기억 목록·삭제 확인.
- `OdometerValidationActivity.kt`: debug 전용 원시 SDK 검증 화면.

2026-10-26 로컬 검사에서 기존 Window 미션 16개와 이동 추적·저장 23개, 총 39개 JVM 테스트가 통과했다. 캐시된 AndroidX·Vehicle·Filament 바이너리와 SDK 37 android.jar를 사용한 main/debug/Compose 타입 검사와 aapt2 리소스 컴파일이 통과했다. 10월 27일 Android Studio `:app:assembleDebug` 및 Pleos Connect v2.1.0 AVD 설치에 성공했다. 개발 도구에서 검증 화면을 열었고, **현재 트립 거리 capability 연결 실패**가 화면에 나타났다. 예외 원인, 주행 시간 capability, paired values, Virtual Console 매핑, P 전환·종료 경계는 아직 확인하지 못했다. 상세 오류를 진단 로그에 추가했으며 원인 파악 뒤 제품 관찰 흐름의 AVD 검증을 진행한다.


## 2026-10-28 누적거리 대체 측정

현재 트립 거리·시간 capability가 미지원이면 `checkCurrentDistanceDrivenCapabilities` 및 `getCurrentDistanceDriven`으로 전환한다. 권한 오류·일반 연결 오류는 미지원으로 간주하지 않는다. 누적거리 km를 Double m로 변환하고 5초 간격으로 읽으며, 관찰 시작값 대비 증가분만 초안에 남긴다. 시간은 추정하지 않고 null로 저장하여 ‘주행 시간 확인 불가’로 표시한다. 측정 방식은 관찰 중 전환하지 않으며, 감소·수신 공백·측정 방식 변화는 구간을 종료한다.

저장 형식 v4는 측정 출처(TRIP/TOTAL)와 시간 미확인을 보존하며 v1~3도 읽는다. 테마·메모·처리 완료 ID는 유지한다. 이동 XP는 계속 지급하지 않는다.

이번 확인: JVM 테스트 및 디버그 빌드 통과. Pleos v2.1.0 실제 SDK 검증 화면에서 트립 미지원 후 TOTAL 수신을 확인했다. Virtual Console의 Odometer를 733435 → 733436 km로 변경했을 때 앱 원시 측정값이 733435000 → 733436000 m로 갱신됐고 주행 시간은 ‘확인 불가’로 표시됐다. 이 확인은 디버그 원시 수신 화면 기준이며 제품의 초안 저장·수정·삭제 전체 흐름과 실차 확인은 별도다. 공식 [Virtual Console 가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide)는 누적 주행거리만 설명한다.

직접 확인: P 상태에서 도감 ‘함께한 이동’의 ‘다음 이동 함께하기’를 누르고 시작 값 확인을 기다린다. Virtual Console의 누적거리를 증가시키고 약 5초 뒤 앱의 종료 버튼으로 관찰을 끝내면 확인된 증가분을 초안으로 볼 수 있다. 누적거리를 먼저 올린 뒤 관찰을 시작하면 그 이전 증가는 기록되지 않는다. 디버그 SDK 검증 화면에서는 기억을 생성하지 않는다.


## 수동 기억·작성 복원 (2026-10-28)

저장 v5는 `MANUAL` 출처를 지원한다. 내부 0m는 수동 출처의 빈 측정값 표식일 뿐 화면에는 거리·시간을 표시하지 않는다. 측정 초안과 독립적으로 수동 기억을 저장할 수 있으며, `firstSavedAt` 이동 배지 근거는 수동 기억으로 생성하지 않는다. 수동 기록의 재저장·삭제 후 재작성도 같은 ID로 중복 저장되지 않는다.

편집 대상 ID·작성 모드·테마·메모는 앱 전용 `journey_editor`에 보관한다. 제한 상태에서는 편집창을 숨기고 모델에서도 편집·저장을 거부한다. 허용 상태로 돌아오면 이어 쓸 수 있다. 저장이 실패하면 편집 내용을 보존하고, 저장 성공·명시적 작성 취소 때만 정리한다. 앱이 저장 직후 종료돼 초안 정리가 지연됐으면 복원 시 이미 처리된 ID를 확인해 다시 저장하지 않는다.

`JourneyLifecycleTest`는 독립된 Android Application의 파일·설정을 사용해 작성 → 제한 → 모델 재생성 → 저장 → 수정 → 삭제 → 재읽기를 확인한다. 원래 사용자의 펫·성장·기억은 읽거나 수정하지 않는다.


### 이번 화면 확인 결과

Computer Use로 도감의 ‘직접 기억 남기기’ 진입, 메모 없이 ‘나들이’ 저장, ‘함께한 시간’으로 수정, 홈 토끼 인사에 수정한 테마 반영을 확인했다. 확인용 수동 기억 1건은 사용자 앱에 남겨 두었다. 삭제와 작성 복원·제한 차단은 격리된 Android 통합 테스트로 확인했다. 현재 사용자 앱의 전체 삭제는 실행하지 않았다. 설정 진입 좌표가 자동 승인 검토에서 삭제 가능성이 있다는 이유로 차단되어 설정 확인창의 취소 동작은 직접 확인 전이다. 사용자에게 설정 화면을 직접 열어 달라고 요청한 상태다.
