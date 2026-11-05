# Compose 동료 선택과 성장 외형

- 최초 작성: 2026-10-26 · 현황 갱신: 2026-10-31
- 대상: Kotlin Android 앱의 실제 MainActivity. 화면 UI는 Jetpack Compose로 구성한다.
- 범위: 첫 캐릭터 선택 → 홈 → 재실행 시 선택 복원 → 캐릭터 변경.

## 현재 여섯 동료

| 선택 | 저장 ID | 시각 방식 |
| --- | --- | --- |
| 곰 동료 | `bear` | Canvas 기반 2.5D |
| 토끼 동료 | `rabbit` | Canvas 기반 2.5D |
| 픽셀 여행자 | `pixel` | 픽셀 아틀라스 |
| 도트 구름 요정 | `cloud` | 픽셀 아틀라스 |
| 씨앗 정령 | `seed` | 픽셀 아틀라스 |
| 미니 자동차 로봇 | `rover` | 픽셀 아틀라스 |

여섯 캐릭터는 같은 미션·레벨 규칙을 공유한다. 모두 Lv.1·5·15 성장 외형을 갖는다. 달리기·걷기 등의 동작은 주차 중 표현이며 주행 중 애니메이션으로 제공하지 않는다. 자산별 렌더 근거는 [성장 자산 기록](./evolution/README.md)을 참조한다.

최신 [첫 미션·성장 구현 기록](./wind-mission.md)을 함께 읽는다. 캐릭터 선택과 변경은 P/UX 사용 허용 확인 후 가능하다.

## 사용 흐름

1. 첫 실행에서 여섯 캐릭터의 모습·설명을 확인한다.
2. 원하는 캐릭터를 선택하고 ‘이 동료와 시작하기’를 누른다.
3. 홈에서 선택한 동료를 보고 ‘인사하기’로 움직임을 확인한다.
4. 앱을 종료·재실행하면 확정한 동료로 홈에 진입한다.
5. 홈 오른쪽 펫 선택 버튼에서 다른 캐릭터를 고른다. 성장·기억은 유지된다.

선택 중인 ID와 화면 상태는 `rememberSaveable`로 유지하고, 확정한 ID는 앱 전용 SharedPreferences의 `companion/character_id`에 저장한다. 변경 작업은 캐릭터 ID만 갱신하며 기억·성장 데이터를 삭제하지 않는다. 첫 미션 완료와 경험치·레벨 저장을 추가했으며, 캐릭터를 바꾸어도 유지된다. 기억 저장은 후속이다.

## 현재 구현 상태

- Compose 홈·선택·확인·취소·인사 버튼과 주야간 테마.
- 캐릭터 선택은 여섯 동료 중 하나를 고르며, 홈의 선택한 동료가 움직인다.
- 동작 영역은 기존 `PetMotionView`를 `AndroidView`로 재사용한다. lifecycle에 따라 중단·재개하고, MVP는 2.5D 방식으로 유지한다.
- 개발 도구 탭은 `BuildConfig.DEBUG`에서만 노출한다. release 제품 탐색에서는 보이지 않아야 한다.
- 성장·기억·꾸미기 상태는 로컬 저장한다. 서버/계정 동기화는 없다.

## 검증

2026-10-26 사용자 실행 확인: 위에서 안내한 Compose 선택·홈 흐름이 잘 동작한다는 보고를 받았다. 에이전트 환경에서의 실행 결과와 구분하며, 세부 시나리오별 로그·프레임 성능·실제 3D 검증까지 완료한 것으로 확대하지 않는다.

- Compose compiler 2.4.20과 실제 Android·AndroidX 바이너리로 main/debug Kotlin 소스 검사 통과. 임시 R 상수를 사용한 검사이므로 APK 빌드와 구분한다.
- main/debug `aapt2 compile` 통과. 에이전트 환경에서는 리소스 링크·APK 패키징·실제 AVD 화면을 확인하지 못했으며, 이후 사용자 환경에서 앱 실행 성공을 보고받았다.
- `CharacterSelectionTest`에 토끼 선택 → Activity 재생성 후 복원 → 곰 변경과 저장 ID 확인을 추가했다. 기기 테스트 실행은 아직 하지 못했다. 테스트는 테스트 에뮬레이터의 캐릭터 선택을 변경하므로 테스트 기기에서 실행한다.
- `./gradlew --offline :app:assembleDebug`는 wrapper 캐시 잠금 파일 쓰기 제한으로 태스크 진입 전에 실패했다. 이는 에이전트 환경의 제약이며, 이후 사용자 환경의 실행 성공 보고와 구분한다.

## 다음 단계

이 문서는 선택 화면의 초기 구현 이력이다. 3D/GLB 실험은 제품 핵심에서 제외되었다. 현재는 여섯 캐릭터와 Lv.1·5·15 외형을 사용하며, 다음 작업은 [개발계획서](../pleos-vehicle-companion-development-plan.md)의 Release·미션 검증·사용자 조사 순서를 따른다.

## 설정 근거

- [Compose compiler 설정](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler): Compose BOM 2026.09.00, compiler 2.4.20을 적용했다. AGP의 built-in Kotlin을 사용하며 별도 kotlin-android 플러그인을 추가하지 않는다.
- [Compose 상태 복원](https://developer.android.com/develop/ui/compose/state-saving): 임시 UI 선택 복원과 확정한 로컬 저장을 구분한다.
- [View 상호 운용](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose): 기존 펫 동작 View만 한정해 재사용한다.

Context7 CLI 문서 조회는 네트워크 DNS 제한으로 실패해 Android 공식 문서를 사용했다.
