# PleosMate 문서 안내

이 문서 묶음은 현재 Android 앱 구현, Pleos Connect AVD에서 확인한 사실, 아직 검증하지 않은 사업 가설을 구분해 관리한다. 아키텍처 구조는 2026-11-01 기준이다.

| 순서 | 문서 | 관리 내용 |
| --- | --- | --- |
| 1 | [제품 기획서](./pleos-vehicle-companion.md) | 제품 방향, 사용자 가치 가설, 디자인 원칙 |
| 2 | [PRD](./pleos-vehicle-companion-prd.md) | 현재 기능 범위, 요구사항, 제약, 다음 제품 검증 |
| 3 | [개발계획서](./pleos-vehicle-companion-development-plan.md) | 구현 현황에서 출시 준비까지의 개발 순서와 완료 기준 |
| 기술 | [Android 아키텍처](./architecture.md) | 현재 feature 트리, UI 상태·SDK·저장 책임, 수명주기와 검증 결과 |
| 4 | [공모전 제안서](./pleos-vehicle-companion-application.md) | 제출용 소개, 구현·검증 근거, 심사 대응 |
| 5 | [사업화·시장 검증 계획](./business-validation-plan.md) | 고객 가설, 수익 모델, 사용자·매출 시나리오와 실험 |
| 별도 | [SDK 검증 기록](./pleos-sdk-validation.md) | 환경과 날짜별 관찰 기록. 과거 상태는 현재 상태로 읽지 않음 |
| 별도 | [SDK 미션 카탈로그](./design/sdk-mission-catalog.md) | 미션 조건·보상·지원 SDK·AVD/실차 검증 경계 |

## 현재 제품 기준

PleosMate는 주차 중 차량 상태 확인과 개인 기억을 펫 성장·재회로 연결하는 Kotlin Android 앱이다. UI는 Jetpack Compose다. 곰·토끼는 Canvas 기반 2.5D, 픽셀 여행자·도트 구름 요정·씨앗 정령·미니 자동차 로봇은 픽셀 아틀라스 방식으로 표현한다. 여섯 동료 모두 Lv.1·5·15 외형을 제공한다.

앱에는 홈·미션·도감·설정의 플로팅 하단 탐색과 펫 선택 버튼이 있다. 개발 도구는 디버그 빌드 전용이다. 차량 미션, 일일·주간 기억, XP·레벨, 꾸미기, 배지 도감, 성장 이야기, 이동 기억의 현재 범위와 제약은 PRD·미션 카탈로그에서 확인한다.

**핵심 경험:** 주차 후 동료와 재회 → 지원되는 차량 신호 또는 사용자가 남긴 기억 확인 → XP·성장·도감 반응 → 다음 재회.

주행 중 앱 이용과 달리기 애니메이션은 MVP 제공 기능이 아니다. 차량을 제어하지 않으며, SDK 지원 여부와 AVD 검증을 전 차종 실차 지원으로 확대 해석하지 않는다. 기록은 현재 로컬 단일 저장소이며 계정·클라우드 동기화가 없다.

## 문서 관리 규칙

- 기능 구현과 실행 검증을 구분한다. AVD·Virtual Console·단위 검사·실차 결과를 구체적으로 쓴다.
- 사업 수치·가격·전환율·사용자 규모는 근거가 생기기 전까지 모두 가정으로 표시한다.
- 현재 사양은 PRD, SDK 미션은 카탈로그, 개발 순서는 개발계획서, 사업 가설은 사업화 계획에서 관리한다.
- SDK 검증 기록은 시간순 증거 보관용이다. 오래된 미완료 상태는 후속 기록으로 정정하며 과거 기록을 지우지 않는다.
- 이전 제안서는 정리 과정에서 제거했다. 현재 기획·요구사항·제출 원고는 위 최신 문서를 기준으로 한다.

## 상세 디자인·구현 기록

- [배지 도감](./design/badge-codex.md)
- [캐릭터 선택](./design/character-selection.md)
- [픽셀 동료](./design/dot-companions.md)
- [성장 이야기](./design/first-memory.md)
- [이동 기억](./design/journey-memory.md)
- [SDK 미션 카탈로그](./design/sdk-mission-catalog.md)
- [펫 모션](./design/pet-motion.md) · [대화 표현](./design/pet-dialogue.md) · [성장 자산](./design/evolution/README.md)
- [디자인 문서 안내](./design/README.md)

## 권장 다음 순서

1. [현재 아키텍처](./architecture.md)를 기준으로 화면→상태→SDK/저장 흐름을 검토하고 릴리스 QA를 이어간다.
2. **제품 실험 1:** 펫 전용 여행 지도와 주차 후 이동 기록/제한 XP를 화면 시안으로 검증한다. 현재 이동 XP는 없고 Odometer 지원·안전 검증이 선행 조건이다.
3. **사업 실험 2:** B2B 파트너와 제휴 혜택·주유 할인 파일럿의 비용·정산·승인 조건을 탐색한다. 계약 전 할인이나 수익을 약속하지 않는다.
4. 인터뷰와 작은 파일럿 결과를 바탕으로 실제 수요·기술 가능성이 확인된 범위만 구현한다. 친구·펫 짝 맺기는 장기 후보로 둔다.

공모전 수상이나 앱 마켓 탑재는 보장되지 않는다. 공식 심사 기준은 [심사 안내](https://appdevchallenge.auto-hmg.io/guide/judging-awards)를 참조한다.
