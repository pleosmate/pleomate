# 펫 연속 모션 원화와 제작 기록

2026-10-29 · 내장 image_gen 사용. 기존 곰·토끼를 참고해 몸·팔·발을 분리한 원화를 새로 만들었다.

## 해결하려는 문제

여러 장의 달리기 포즈를 생성해 교체하면 손·발 위치, 얼굴 비율, 외곽선이 포즈마다 달라졌다. 대각선에서 손 옆 돌출부가 추가 손처럼 보였고, 먼쪽 손을 가리는 보정만으로는 움직임의 일관성을 확보하기 어려웠다.

최신 구현은 같은 몸을 유지하면서 같은 팔 두 개와 발 두 개를 연속해서 움직인다. 어깨와 앞뒤 순서가 고정되고, 발은 접지와 되돌아오는 궤적을 반 주기 차이로 따른다. 양팔이 몸에 가려지는 것은 시점에 따른 가림이며 팔다리 원화가 프레임마다 새로 생성되지는 않는다.

## 저장한 자산

- [곰 원화](../../app/src/main/assets/pets/rig/bear.png), [표시 영역](../../app/src/main/assets/pets/rig/bear.json)
- [토끼 원화](../../app/src/main/assets/pets/rig/rabbit.png), [표시 영역](../../app/src/main/assets/pets/rig/rabbit.json)
- [모션 검토 화면](./pet-running-preview.html)

PNG는 각 1254×1254, RGBA이며 두 장 합계 약 2.4MB다. 각 3×3 시트는 다음 순서다.

| 행 | 왼쪽 | 가운데 | 오른쪽 |
| --- | --- | --- | --- |
| 1 | 정면 몸 | 왼쪽 몸 | 오른쪽 몸 |
| 2 | 뒤쪽 몸 | 앞 왼쪽 몸 | 앞 오른쪽 몸 |
| 3 | 눈 감은 정면 몸 | 팔 원화 하나 | 발 원화 하나 |

위 몸 시트 하단의 팔·발은 이전 버전이며, 현재는 아래 ‘짧고 둥근 손발 보정’의 별도 손발 PNG를 사용한다.

몸 원화에는 손·발·꼬리가 없다. JSON은 시트에서 실제 부위가 차지하는 범위를 측정한 좌표이며, 단순 균등 분할을 사용하지 않는다. 같은 부위가 자세마다 다르게 변하지 않도록 앱과 미리보기에서 이 좌표를 사용한다. 깜빡임은 눈 부위만 교체해 몸 윤곽이 바뀌지 않게 한다.

새 표현은 홈·선택 화면·debug 미리보기 모두에 적용된다. 경험치·레벨·해금·기존 저장 데이터는 변경하지 않는다. 실제 주행 상태 연결은 별도 작업이다. ‘누구나 자연스럽게 느낀다’고 검증된 결과는 아니며, 실제 사용자 반응 확인은 남아 있다.

검증: 전체 단위 테스트 74개 및 디버그 빌드 통과. 에뮬레이터에 설치해 홈·곰 앞 왼쪽·토끼 앞 오른쪽 화면을 기록했고 AndroidRuntime 오류 로그는 없었다. [검토 기준과 화면 기록](./pet-motion.md).

## 짧고 둥근 손발 보정

사용자가 제안한 참고 대상에서는 짧은 팔다리와 통통한 실루엣이라는 일반적인 비율만 참고했다. 생성 도구에는 기존 곰·토끼 원화만 참고 이미지로 전달했으며, 얼굴·귀·표정·색상·몸 원화는 변경하지 않았다.

- 최신 손발: [곰](../../app/src/main/assets/pets/rig/bear-paws.png), [토끼](../../app/src/main/assets/pets/rig/rabbit-paws.png). 각각 1254×1254 투명 PNG에 앞발·뒷발 두 부위가 들어 있다.
- JSON의 `parts`는 몸 6방향과 눈 감은 정면 몸 7개를 가리킨다. `limbsImage`와 `limbs`는 새 손발 PNG와 두 부위 좌표를 가리킨다. 기존 몸 시트 하단의 긴 팔·납작한 발은 사용하지 않는다.
- 앞발은 약 31×30(곰), 28×28(토끼)로 짧고 둥글게 표시한다. 시작 부분의 투명도를 부드럽게 이어 배와의 연결선이 두드러지지 않게 했다.
- 발은 약 34×29(곰), 31×28(토끼)로 줄이고 몸 아래에 약 14만큼 겹쳐 작은 발 끝이 드러나게 했다. 수치는 360×300 렌더 좌표 기준이다.
- 달리기의 팔 흔들림은 ±20도에서 ±12도, 발 들림은 최대 12에서 8, 몸의 상하 이동은 최대 3.6에서 2.4로 줄였다. 속도·해금·저장 규칙은 유지한다.

이 보정 후에도 전체 테스트 74개와 디버그 빌드가 통과했다. Pleos 에뮬레이터에 최신 APK를 설치하고 [새 손발이 적용된 홈](./qa/pet-rig/home-short-paws.png)을 확인했다.

### 곰 손발 원화 프롬프트

Use case: stylized-concept. Asset type: TWO isolated replacement plush limb textures for an Android mascot rig. Input image: the current cream bear atlas is ONLY a material/color/lighting reference. Create ONLY TWO separate objects on a square transparent canvas: on the LEFT, one SHORT PUFFY ROUND PAW; on the RIGHT, one SMALL ROUNDED FOOT. NO character bodies, faces or ears anywhere.
Paw: a soft little mochi-like rounded bean, approximately as wide as it is tall (about 210x190px), NO long forearm, no capsule, no tube. Rounded chunky mitten end, subtly narrower flatter attachment edge at the top which blends softly into transparency. No fingers, thumb, claws or toe details. It must look like a tiny integrated stubby paw, not a glove or separate ball.
Foot: a softly rounded chunky little bean approximately 220px wide x 190px tall, plump volume, gently flattened bottom for weight-bearing. NO long flat oval, no shoe, sole, toes, claws or pads. Slightly narrower soft attachment area at the upper edge so it can overlap naturally under the belly.
Match the EXACT existing premium short cream micro-fleece texture, warm ivory color and soft upper-left lighting of the provided mascot. Soft diffuse shading, no harsh seams or dark outline. Original generic plush anatomy, no recognizable existing franchise character features.
Composition: square 1024x1024 PNG. Paw centered at (256,512), foot centered at (768,512). Generous separate transparent margins; only these two cutouts. GENUINE alpha=0 background. No floor, drop shadow outside object, beige backdrop, contact-sheet border, labels, text, watermark, UI, accessories or decorations.

### 토끼 손발 원화 프롬프트

Use case: stylized-concept. Asset type: TWO isolated replacement plush limb textures for an Android mascot rig. Input image: the current cream rabbit atlas is ONLY a material/color/lighting reference. Create ONLY TWO separate objects on a square transparent canvas: on the LEFT, one SHORT PUFFY ROUND PAW; on the RIGHT, one SMALL ROUNDED FOOT. NO character bodies, faces or ears anywhere.
Paw: a soft little mochi-like rounded bean, approximately as wide as it is tall (about 210x190px), NO long forearm, no capsule, no tube. Rounded chunky mitten end, subtly narrower flatter attachment edge at the top which blends softly into transparency. No fingers, thumb, claws or toe details. It must look like a tiny integrated stubby paw, not a glove or separate ball.
Foot: a softly rounded chunky little bean approximately 220px wide x 190px tall, plump volume, gently flattened bottom for weight-bearing. NO long flat oval, no shoe, sole, toes, claws or pads. Slightly narrower soft attachment area at the upper edge so it can overlap naturally under the belly.
Match the EXACT existing premium short cream micro-fleece texture, warm ivory color and soft upper-left lighting of the provided mascot. Soft diffuse shading, no harsh seams or dark outline. Original generic plush anatomy, no recognizable existing franchise character features.
Composition: square 1024x1024 PNG. Paw centered at (256,512), foot centered at (768,512). Generous separate transparent margins; only these two cutouts. GENUINE alpha=0 background. No floor, drop shadow outside object, beige backdrop, contact-sheet border, labels, text, watermark, UI, accessories or decorations.


## 곰 원화 프롬프트

Use case: stylized-concept. Asset type: separated 2.5D puppet rig texture atlas for an Android companion. Reference is the existing cream plush BEAR with small round ears; preserve exactly its identity, unified very round bean head/torso, chocolate glossy eyes, tiny smile, blush, peach ear interiors, premium fine short micro-fleece, NOT curly wool, warm upper-left light. This is a reusable body-part atlas, NOT an animation sheet, NOT running poses.
Create ONE SQUARE 1536x1536 transparent RGBA PNG with a precise THREE COLUMNS x THREE ROWS layout, nine isolated objects. Each cell 512x512, clear transparent margins, no lines, labels or typography.
ROW 1: cell 1 FRONT-FACING bare body with face and ears; cell 2 strict LEFT profile bare body facing screen left; cell 3 strict RIGHT profile bare body facing screen right.
ROW 2: cell 1 BACK-FACING bare body, no face or tail; cell 2 FRONT-LEFT 45-degree three-quarter bare body with both eyes visible; cell 3 FRONT-RIGHT 45-degree three-quarter bare body with both eyes visible.
The SIX bodies have NO ARMS, NO HANDS, NO LEGS, NO FEET, NO TAIL, NO LIMB NUBS whatsoever. Only the single smooth round body/head with ears and face. Same body volume/proportions across views. Body centered in each cell, bottom at 440px within its cell. Body height including ears approximately 350px, width approximately 310px. All six are neutral upright, feet/arms will be attached by code.
ROW 3: cell 1 exact duplicate of row1-cell1 body in exactly the same size and placement but with both eyes gently closed as two curved brown eyelids; everything else identical. Cell 2: ONE separate cream plush ARM, a simple short vertical rounded capsule, narrow rounded shoulder end at top and one softly rounded mitten end at bottom, no fingers or thumb, about 120px wide x 190px tall, centered in cell. Cell 3: ONE separate cream plush FOOT, simple plump horizontal oval, no toes, no claws, no pads, about 160px wide x 105px tall, centered in cell.
Every part uses precisely the same fleece material, color, lighting and softness as the reference. No stumps on bodies. Rig parts must never be combined or overlapping. Only nine distinct isolated cutouts, not 12 or 24. TRUE alpha=0 empty space everywhere around parts. No background, beige studio glow, floor, cast shadows, pedestal, UI, clothing, accessories, watermark, text or grid.

## 토끼 원화 프롬프트

Use case: stylized-concept. Asset type: separated 2.5D puppet rig texture atlas for an Android companion. Reference is the existing cream plush RABBIT with long upright soft ears; preserve exactly its identity, unified very round bean head/torso, chocolate glossy eyes, tiny smile, blush, peach ear interiors, premium fine short micro-fleece, NOT curly wool, warm upper-left light. This is a reusable body-part atlas, NOT an animation sheet, NOT running poses.
Create ONE SQUARE 1536x1536 transparent RGBA PNG with a precise THREE COLUMNS x THREE ROWS layout, nine isolated objects. Each cell 512x512, clear transparent margins, no lines, labels or typography.
ROW 1: cell 1 FRONT-FACING bare body with face and ears; cell 2 strict LEFT profile bare body facing screen left; cell 3 strict RIGHT profile bare body facing screen right.
ROW 2: cell 1 BACK-FACING bare body, no face or tail; cell 2 FRONT-LEFT 45-degree three-quarter bare body with both eyes visible; cell 3 FRONT-RIGHT 45-degree three-quarter bare body with both eyes visible.
The SIX bodies have NO ARMS, NO HANDS, NO LEGS, NO FEET, NO TAIL, NO LIMB NUBS whatsoever. Only the single smooth round body/head with ears and face. Same body volume/proportions across views. Body centered in each cell, bottom at 440px within its cell. Body height including ears approximately 425px, width approximately 270px. All six are neutral upright, feet/arms will be attached by code.
ROW 3: cell 1 exact duplicate of row1-cell1 body in exactly the same size and placement but with both eyes gently closed as two curved brown eyelids; everything else identical. Cell 2: ONE separate cream plush ARM, a simple short vertical rounded capsule, narrow rounded shoulder end at top and one softly rounded mitten end at bottom, no fingers or thumb, about 120px wide x 190px tall, centered in cell. Cell 3: ONE separate cream plush FOOT, simple plump horizontal oval, no toes, no claws, no pads, about 160px wide x 105px tall, centered in cell.
Every part uses precisely the same fleece material, color, lighting and softness as the reference. No stumps on bodies. Rig parts must never be combined or overlapping. Only nine distinct isolated cutouts, not 12 or 24. TRUE alpha=0 empty space everywhere around parts. No background, beige studio glow, floor, cast shadows, pedestal, UI, clothing, accessories, watermark, text or grid.

## 곰 정리·방향 보정 프롬프트

Use case: background-extraction. Edit target: the attached 1254x1254 3x3 plush puppet-parts atlas. Remove ALL stray white fragments, colored specks, background scraps, external halos and shadows. Keep ONLY the nine plush objects (six bare body views, one closed-eye bare front body, one arm capsule, one oval foot). Every space between and around these nine cutouts must be genuinely fully transparent alpha=0. Preserve exact body geometry, material, colors, shading, dimensions and positions; no new limbs, no tail, no reordering, no resizing, no text. No checkerboard pixels or white outlines. Clean soft antialiased edges. Preserve 1254x1254 dimensions. Also correct ONLY row2-column3 (middle row rightmost bare body) to face FRONT-RIGHT at 45 degrees: mouth and both eyes are visibly on the RIGHT side of the body's center. It must be the opposite camera angle of row2-column2. Keep this object's same center, size and material.

## 토끼 투명 배경 정리 프롬프트

Use case: background-extraction. Edit target: the attached 1254x1254 3x3 plush puppet-parts atlas. Remove ALL stray white fragments, colored specks, background scraps, external halos and shadows. Keep ONLY the nine plush objects (six bare body views, one closed-eye bare front body, one arm capsule, one oval foot). Every space between and around these nine cutouts must be genuinely fully transparent alpha=0. Preserve exact body geometry, material, colors, shading, dimensions and positions; no new limbs, no tail, no reordering, no resizing, no text. No checkerboard pixels or white outlines. Clean soft antialiased edges. Preserve 1254x1254 dimensions.
