# 펫 공간 배경 v1

생성 방식: built-in imagegen. 에셋: `app/src/main/assets/pets/rooms/rooms-v1.png`.

프롬프트: 2열 3행 배경 아틀라스. 캐릭터·텍스트·UI 없이 중앙 바닥을 비운 실제 장소. 위 행은 부드러운 3D 독서방·정원방, 중간 행은 픽셀 RPG 여행자 휴게소·하늘 우체국, 아래 행은 픽셀 온실·로봇 정비소. 주변부에 풍부한 소품, 원근감 있는 바닥, 캐릭터는 앱에서 별도 합성.

실제 생성 이미지의 행 경계는 1536px 높이 기준 432px, 960px이다. PetRoomBackground는 해당 영역을 잘라 View에 center crop한다. 원화의 소품 비율을 유지하고 캐릭터 애니메이션은 PetMotionView에서 독립 렌더링한다. 이전 도형 공간은 사용하지 않는다.

## 로봇 배경 v2

Built-in imagegen prompt: standalone 16:9 muted slate/teal pixel repair garage, authentic 320×180 chunky pixel language to match a 48×48 robot sprite, restrained palette, compact workbench left, charger right, back wall window, no characters or UI, clear central floor, feet at 50% x / 82% y. No bloom, noise, busy machinery or cinematic rendering. 원화는 `rover-room-v2.png`. 로봇의 접지 위치를 표시 영역 높이의 82%에 맞춘다.

## 공통 합성 개선

- 펫 발 위치를 영역 높이 82%로 통일하고 도트 쿠션의 사각 받침을 낮은 불투명도의 타원 그림자로 변경.
- 홈 카드의 비어 있던 말풍선 슬롯을 카드 밖으로 옮겨 배경이 하단까지 채워지도록 수정.
- 곰·토끼 쿠션은 민트에서 베이지로 변경. 로봇 외 배경 채도를 75%로 낮춰 펫 가독성을 개선.
- Computer Use로 곰·토끼·여행자·구름·로봇 홈 확인. 씨앗 홈 확인 중 앱 창 연결이 끊겼으며 최종 색 조정 후 전종 재확인은 미완료.

## 2026-11-03 홈 방향 변경

사용자 선택에 따라 홈의 PetStage는 showRoomBackground=false로 배경 이미지를 숨긴다. 홈 카드는 450dp로 줄이고 캐릭터를 중앙에 배치한다. 선택한 의상·공간 소품과 모션은 유지한다. 제작한 배경 에셋은 보관하며 다른 미리보기 화면의 기존 표시에는 영향을 주지 않는다. 별도 펫의 방 화면은 아직 구현하지 않았다.

홈 외 동료 선택, 하단 동료 아이콘, 설정·모션 미리보기도 방 배경을 기본 숨김으로 통일했다. PetStage와 PetMotionView의 showRoomBackground 기본값은 false다. 이미지 에셋은 추후 활용용으로 보관한다.
