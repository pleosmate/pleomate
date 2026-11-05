# 달리기 이미지 제작 기록

현재 앱은 [몸·팔·발을 분리한 연속 2.5D 리그](./pet-rig.md)를 사용한다. 아래 시트와 프롬프트는 이전 제작 기록이며 현재 렌더러는 사용하지 않는다. [최신 모션 미리보기](./pet-running-preview.html).

2026-10-29 · 내장 image_gen 사용 · 기존 곰·토끼 PNG를 캐릭터 참고 이미지로 사용.

기본 시트는 1536 × 1024 RGBA PNG이며, 왼쪽부터 6프레임, 위부터 정면·왼쪽·오른쪽·뒷면 순서다. 대각선 시트에는 앞 왼쪽·앞 오른쪽을 각각 6프레임씩 추가했다. 캐릭터별 36포즈, 총 72포즈로 12개의 달리기 루프를 구성한다. 원본 앉은 이미지와 눈 감은 이미지는 유지했다. 생성된 시트의 간격이 완전히 균등하지 않아, `PetRunDirection.frameRect`와 HTML 미리보기에서 측정한 행 경계와 프레임별 중심을 사용한다. 단순 균등 분할을 하면 옆 프레임의 귀·발이 섞이므로 사용하지 않는다.

- [곰 달리기](../../app/src/main/assets/pets/bear-run.png)
- [토끼 달리기](../../app/src/main/assets/pets/rabbit-run.png)
- [곰 대각선 달리기 · 손 윤곽 보정](../../app/src/main/assets/pets/bear-run-diagonal-v2.png)
- [토끼 대각선 달리기 · 손 윤곽 보정](../../app/src/main/assets/pets/rabbit-run-diagonal-v2.png)
- [12개 달리기 루프 미리보기](./pet-running-preview.html)

앱의 debug ‘동료 움직임 미리보기’에서 곰·토끼와 방향을 선택할 수 있다. 곰은 720ms, 토끼는 600ms 주기로 재생한다. 정지·백그라운드·수동 제한 시 애니메이션을 중단한다. 실제 차량 속도와 자동 연결하는 작업은 아직 포함하지 않았다.

## 곰 생성 프롬프트

Use case: stylized-concept. Asset type: production 2.5D Android pet RUN animation sprite atlas. Reference image: existing cream plush BEAR companion; preserve its exact identity, round unified bean head/body, short limbs, round peach inner ears, glossy brown eyes, small smile, blush and premium micro-fleece texture. Create ONE transparent PNG sprite sheet, landscape aspect 3:2, precisely 6 equal columns by 4 equal rows (24 square cells), no grid lines. Row 1: FRONT view running gently toward camera in place. Row 2: strict LEFT-facing profile running in place toward screen left. Row 3: strict RIGHT-facing profile running in place toward screen right. Row 4: BACK view running away in place, back of ears, tiny round tail, NO face visible. Each row is a seamless six-frame run cycle left-to-right: left contact, left recoil, flight, right contact, right recoil, flight. Alternate left/right feet and opposite paws visibly, not six identical poses. Same character size, same orthographic camera, same lighting upper left throughout; fix body center at 50% of cell width, baseline at 86% of cell height, head top near 15%, generous clear margins in every cell, every ear/paw fully inside its cell. Only subtle 2-3% vertical bounce. Friendly gentle jogging, two legs, no shoes, clothes, accessories, floor, pedestal, shadows outside character, scenery, text, labels, numbers, watermark, border or background color. Actual transparent alpha outside all 24 characters. This is ONE precisely aligned game animation atlas, NOT a collage or mockup.

## 토끼 생성 프롬프트

Use case: stylized-concept. Asset type: production 2.5D Android pet RUN animation sprite atlas. Reference image: existing cream plush RABBIT companion; preserve exact identity, unified rounded bean body, short limbs, two long upright peach-inner ears, glossy brown eyes, tiny smile, blush and premium micro-fleece texture. Create ONE transparent PNG sprite sheet, landscape aspect 3:2, precisely 6 equal columns by 4 equal rows (24 square cells), no grid lines. Row 1: FRONT view running gently toward camera in place. Row 2: strict LEFT-facing profile running toward screen left in place. Row 3: strict RIGHT-facing profile running toward screen right in place. Row 4: BACK view running away in place, back of long ears and small cotton tail, NO face visible. Each row is a seamless six-frame run cycle left-to-right: left contact, left recoil, flight, right contact, right recoil, flight. Alternate left/right feet and opposite paws visibly; ears respond gently to each stride, not six identical poses. Same character size, orthographic camera, lighting upper left throughout. Body centered at 50% cell width, ground baseline 86% cell height, ear tips near 10%, generous clear margin inside every cell. Entire ears and paws fully inside each cell. Only subtle 2-3% vertical bounce. Friendly two-legged gentle jogging. No shoes, clothes, accessories, floor, pedestal, ground shadow, scenery, text, labels, numbers, watermark, border or background color. Genuine transparent alpha outside all 24 characters. ONE precisely aligned game animation atlas, NOT a collage or mockup.

## 투명 배경 보정 프롬프트

Use case: background-extraction. Edit target: this exact 6-column 4-row character animation sprite sheet. Remove ONLY the entire beige studio background and all external cast shadows, replacing every background pixel with genuine fully transparent alpha=0. Keep all 24 plush characters, all poses, layout, size and exact positions UNCHANGED. Preserve the character texture, ears, feet and soft anti-aliased edges. Do not redraw or rearrange, do not add any checkerboard texture or white rectangle. The PNG must have REAL transparency throughout the space between the 24 separate characters and all canvas edges. No floor, no ground shadow, no scenery or color behind characters. Preserve 1536x1024 image dimensions and 6x4 equal grid alignment.

## 앞쪽 대각선 추가 제작

내장 image_gen으로 기본 캐릭터와 기존 달리기 시트를 함께 참고했다. 두 대각선 시트도 1536 × 1024 RGBA PNG이며 첫째 행은 앞 왼쪽, 둘째 행은 앞 오른쪽이다. 투명도와 캐릭터별 12개 독립 포즈를 확인했다. 곰의 원본 포즈 높이는 약 250px, 토끼는 귀를 포함해 약 320px이므로 앱과 HTML에서 서로 다른 표시 배율로 기존 방향과 크기를 맞춘다.

### 곰 대각선 프롬프트

Use case: stylized-concept. Asset type: production Android 2.5D companion running sprite atlas, 12 poses. Input image 1: character identity reference. Input image 2: existing running animation style reference. Create additional FRONT DIAGONAL views only for the EXACT SAME cream plush BEAR with small round ears. Preserve rounded unified bean head/body, very short arms/legs, peach inner ears, tiny glossy brown eyes, tiny smiling mouth, blush, premium micro-fleece texture, upper-left soft lighting and collectible toy quality.
Composition: ONE landscape 1536x1024 transparent PNG, exactly SIX COLUMNS by TWO ROWS. Each cell is 256x512. Exactly 12 separate full characters, no grid lines. Row 1: front-left three-quarter view, running toward camera and screen LEFT at 45 degrees; BOTH eyes and smile must remain visible, nearer left side of body visible. Row 2: front-right three-quarter view, running toward camera and screen RIGHT at 45 degrees; BOTH eyes and smile visible. This is neither straight front nor side profile. Keep this precise camera angle fixed across all six frames of each row.
Animation: six sequential frames per row, left contact, recoil, flight, right contact, recoil, flight, seamless loop. Clearly alternate the short feet and opposite paws. Only gentle jogging bounce. Maintain identity and body size throughout; do not turn during the cycle.
Alignment: full character centered at x=128+256*column and y=256 in row 1, y=768 in row 2. All characters about 200 pixels tall (RABBIT ears included about 230), body width 155-175px. Generous transparent space around all feet and ears. No overlap between cells.
Background: TRUE transparent alpha=0 outside each character. NO beige background, floor, external cast shadow, pedestal, decoration, text, numbering, labels, watermark, border, clothing or accessories. Isolated RGBA sprites, not a studio scene or contact-sheet mockup.

### 토끼 대각선 프롬프트

Use case: stylized-concept. Asset type: production Android 2.5D companion running sprite atlas, 12 poses. Input image 1: character identity reference. Input image 2: existing running animation style reference. Create additional FRONT DIAGONAL views only for the EXACT SAME cream plush RABBIT with long upright peach-inner ears that gently follow each stride. Preserve rounded unified bean head/body, very short arms/legs, peach inner ears, tiny glossy brown eyes, tiny smiling mouth, blush, premium micro-fleece texture, upper-left soft lighting and collectible toy quality.
Composition: ONE landscape 1536x1024 transparent PNG, exactly SIX COLUMNS by TWO ROWS. Each cell is 256x512. Exactly 12 separate full characters, no grid lines. Row 1: front-left three-quarter view, running toward camera and screen LEFT at 45 degrees; BOTH eyes and smile must remain visible, nearer left side of body visible. Row 2: front-right three-quarter view, running toward camera and screen RIGHT at 45 degrees; BOTH eyes and smile visible. This is neither straight front nor side profile. Keep this precise camera angle fixed across all six frames of each row.
Animation: six sequential frames per row, left contact, recoil, flight, right contact, recoil, flight, seamless loop. Clearly alternate the short feet and opposite paws. Only gentle jogging bounce. Maintain identity and body size throughout; do not turn during the cycle.
Alignment: full character centered at x=128+256*column and y=256 in row 1, y=768 in row 2. All characters about 200 pixels tall (RABBIT ears included about 230), body width 155-175px. Generous transparent space around all feet and ears. No overlap between cells.
Background: TRUE transparent alpha=0 outside each character. NO beige background, floor, external cast shadow, pedestal, decoration, text, numbering, labels, watermark, border, clothing or accessories. Isolated RGBA sprites, not a studio scene or contact-sheet mockup.

### 대각선 투명 배경 보정

Use case: background-extraction. Edit target: this exact twelve-pose 6-column 2-row plush bear running sprite sheet. Remove ONLY the entire studio background, beige halos and all cast shadows outside the plush characters. Replace the background with genuine fully transparent alpha=0, including every gap and canvas edge. Keep all 12 characters, poses, textures, lighting, scale and exact positions unchanged. Preserve the 1536x1024 dimensions. Do not redraw, rearrange, add text, floor, backdrop or checkerboard pixels. The output must be a real transparent RGBA PNG with only the 12 clean plush cutouts.

토끼는 위 프롬프트의 bear를 rabbit으로 바꿔 동일하게 보정했다.

## 대각선 v2 · 손 윤곽 보정

일부 포즈에서 손 옆 돌출부와 반대 손이 함께 나타나, 재생 중 손이 세 개처럼 보이는 문제가 있었다. 내장 image_gen 편집으로 손가락 없는 둥근 앞쪽 손 하나를 유지하고, 반대쪽 팔과 꼬리는 몸에 가려지도록 정리했다. 두 발은 몸 아래에 유지했다. 원본 대각선 시트는 보존하고 `bear-run-diagonal-v2.png`, `rabbit-run-diagonal-v2.png`를 앱 자산에 추가했다. 표시 영역의 폭은 244px로 맞추고 수정된 포즈 중심을 다시 측정해 옆 프레임의 작은 조각이 섞이지 않게 했다. 브라우저에서 프레임을 한 장씩 넘겨 검토할 수 있다.

### 곰 보정 프롬프트

Use case: precise-object-edit. Edit target: the attached transparent 1536x1024 six-column two-row BEAR diagonal-running animation sprite sheet. Fix ONLY limb anatomy and silhouette consistency in all twelve existing poses. There are ambiguous extra side bumps/tail-like appendages beside the hands which read as a THIRD HAND during playback.
STRICT ANATOMY: The character has exactly TWO arms and TWO legs. For this front three-quarter camera view show only ONE near-side arm with ONE rounded mitten-like oval paw clearly in front of the chest; keep the far-side arm completely occluded behind the torso in EVERY frame. Row 1 is facing front-left: keep the near paw on the front-left of the torso. Row 2 is facing front-right: keep the near paw on the front-right of the torso. Remove the other visible hand and ALL rear-side nubs/protrusions at arm height. Hide the tail entirely behind the body in these front diagonal views. There must be no extra hand, no finger, thumb, toe, claw, little nub, double contour, detached blob, third arm, third leg or ghost limb. The ONE visible paw must be a smooth simple rounded plush oval, connected to one short arm at the same shoulder throughout. Exactly TWO simple rounded feet may appear only BELOW the hips; preserve the existing six-frame alternating jogging foot poses and bounce.
PRESERVE: exact existing character identity, cream fleece texture, face, expression, ears, camera direction, light, body shape, pose centers, scale, six columns/two rows order and all positions. Do not move, resize or rearrange any character. Do not change the ears or facial features. Keep the same 1536x1024 canvas. Clean coherent silhouettes across frames, not twelve unrelated designs. Genuine alpha=0 transparency outside the plush characters, no studio background, floor or external shadow.

### 토끼 보정 프롬프트

Use case: precise-object-edit. Edit target: the attached transparent 1536x1024 six-column two-row RABBIT diagonal-running animation sprite sheet. Fix ONLY limb anatomy and silhouette consistency in all twelve existing poses. There are ambiguous extra side bumps/tail-like appendages beside the hands which read as a THIRD HAND during playback.
STRICT ANATOMY: The character has exactly TWO arms and TWO legs. For this front three-quarter camera view show only ONE near-side arm with ONE rounded mitten-like oval paw clearly in front of the chest; keep the far-side arm completely occluded behind the torso in EVERY frame. Row 1 is facing front-left: keep the near paw on the front-left of the torso. Row 2 is facing front-right: keep the near paw on the front-right of the torso. Remove the other visible hand and ALL rear-side nubs/protrusions at arm height. Hide the tail entirely behind the body in these front diagonal views. There must be no extra hand, no finger, thumb, toe, claw, little nub, double contour, detached blob, third arm, third leg or ghost limb. The ONE visible paw must be a smooth simple rounded plush oval, connected to one short arm at the same shoulder throughout. Exactly TWO simple rounded feet may appear only BELOW the hips; preserve the existing six-frame alternating jogging foot poses and bounce.
PRESERVE: exact existing character identity, cream fleece texture, face, expression, ears, camera direction, light, body shape, pose centers, scale, six columns/two rows order and all positions. Do not move, resize or rearrange any character. Do not change the ears or facial features. Keep the same 1536x1024 canvas. Clean coherent silhouettes across frames, not twelve unrelated designs. Genuine alpha=0 transparency outside the plush characters, no studio background, floor or external shadow.



