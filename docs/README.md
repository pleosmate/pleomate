# PleosMate 문서

이 폴더에는 현재 코드 구조를 설명하는 [Android 아키텍처](./architecture.md)와 과거 설계·검증 기록이 있습니다. 날짜가 붙은 실험 문서는 당시 상태의 기록이며 현재 기능 명세로 사용하지 않습니다.

현재 구현 범위는 코드와 테스트를 기준으로 확인합니다. 주요 패키지는 `app/src/main/java/com/pleos/mate/feature/` 아래 `home`, `missions`, `growth`, `collection`, `journey`, `exploration`, `settings`입니다. 특히 탐험 탭은 지역 지도 선택과 방문 확인·기념품 수집 흐름을 포함합니다.

## 문서 판단

- [Android 아키텍처](./architecture.md): 현재 패키지 경계와 앱 데이터 흐름을 설명합니다.
- `design/`의 시안, 프롬프트, QA 캡처, 미리보기: 제작·검토 이력 보존용입니다. 코드와 다르면 현재 앱 설명에 사용하지 않습니다.
- 이전 제품 제안서, PRD, 개발·사업 계획 및 SDK 검증 보고서는 현재 코드와 시점 차이가 있어 복구하지 않았습니다. 최신 사양이 필요하면 현재 구현을 확인해 새로 작성해야 합니다.
