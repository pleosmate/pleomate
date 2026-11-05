# 픽셀 여행자 — 2026-10-31

## v2 최종 생성 프롬프트 (built-in image_gen)

Use case: style-transfer. Asset: transparent game sprite atlas, EXACT 4 columns x 2 rows, landscape 2:1. Image 1 is STYLE REFERENCE ONLY (the RPG screenshot); image 2 is character identity and atlas-layout reference to revise. Redraw the traveler as a genuinely LOW RESOLUTION late-1990s handheld RPG sprite, MUCH simpler and blockier than image 2. Each figure should look drawn on an approximately 32px wide x 48px tall logical pixel grid and enlarged with nearest-neighbor square blocks. Flat 10-12 color palette total, one shadow tone per material, dark stepped 1-logical-pixel outline, absolutely no gradients, no soft highlights, no smoothing, no tiny hair strands. Reference image 1's longer body and legs and small game-world sprite proportions (head about one-third of height), NOT the oversized chibi head of image 2. Keep original auburn short hair, sage jacket, cream scarf, brown boots, tan small satchel, friendly simple two-pixel eyes. Do NOT copy screenshot character's identity, outfit, scene or lettering. Transparent background with no scene, no floor, no shadow, no text, no grid lines. Equal cell boundaries, all sprites same scale, generous transparent padding, aligned baseline per row. Top row: front idle, front smile, front wave with only two arms, front celebrate. Bottom row: four DISTINCT sequential RIGHT-FACING walk-cycle frames: contact right leg forward, passing legs together, contact left leg forward, passing legs together with opposite arm swing. Feet must change pose, exactly two arms and two legs in every frame. Clear chunky visible pixel staircase edges. Deliberately modest game sprite detail, NOT polished high resolution pixel illustration.

## 적용 범위

사용자가 제공한 데스크탑 참고 화면의 레트로 RPG 픽셀 표현을 참고해 만든 독자 캐릭터다. 원본 게임 캐릭터의 얼굴·복장·장면을 복제하지 않는다.

- 앱의 세 번째 선택 동료: 픽셀 여행자. 기존 성장·미션·기억을 공유한다.
- 현재 소스: `app/src/main/assets/pets/pixel-traveler-v2.png`. v1은 이전 시안으로 보관한다.
- v2: 참고 화면처럼 명암·얼굴 디테일을 줄이고 큰 도트로 수정했다. 앱에서는 각 셀을 실제 48×48 격자로 렌더링한 뒤 필터 없이 확대해 도트 크기를 일정하게 유지한다.
- 제작: built-in image_gen, 투명 배경. 4열×2행 스프라이트.
- 윗줄: 기본·웃음·인사·축하. 아랫줄: 오른쪽 걷기 4프레임. 왼쪽은 좌우 반전한다.
- 정면·뒷면·대각선 전용 달리기 프레임은 아직 없다. 해당 방향의 미리보기 요청은 좌우 걷기로 표현한다.
- 픽셀 전용 기본 의상을 사용한다. 곰·토끼의 의상 설정은 보관하되 픽셀 위에 덧그리지 않는다. 공간 소품은 각진 픽셀 표현으로 그린다.
- 렌더링: `PixelPetRenderer.kt`, 비트맵 필터링 비활성. 기존 동물의 몸체·팔다리 리그를 적용하지 않는다.
- 곰·토끼 스카프와 리본에는 목 위치에 맞춘 은은한 명암을 추가했다. 기존 동물 원화 전체를 다시 제작한 것은 아니다.

## v1 생성 프롬프트

Use case stylized-concept. Project asset: transparent sprite atlas for an Android car companion app. Create an ORIGINAL cozy retro RPG pixel traveler character, NOT any existing game character. Small round chibi adventurer with auburn short hair, sage green jacket, cream neck scarf, brown boots, small tan satchel. Friendly face two dark eyes, no text. True crisp pixel art with discrete square blocks, restrained 16-color earthy palette, charcoal outline, no smooth gradients. EXACT sheet layout FOUR equal columns and TWO equal rows, eight separate sprites centered in their equal rectangular cells, consistent character size and ground baseline within every cell, generous transparent padding. No labels no borders no scenery no shadows background genuinely transparent. TOP row left to right: idle front, happy front eyes smiling, front hand wave (two arms only), front celebratory arms raised. BOTTOM row: FOUR sequential walk cycle frames facing right, consistent head and clothes, alternating two arms two legs, readable natural steps. All 8 complete characters fully visible, no overlap across cells. Whole atlas wide landscape aspect 2:1. Output high quality production game sprite sheet. Reference style is classic top-down/side-view RPG pixel characters with chunky outlines and warm muted colors, not rendered 3D.
