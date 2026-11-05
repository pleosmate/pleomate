# 2.5D 캐릭터 생성 프롬프트

2026-10-26 · 내장 image_gen 사용 · 투명 배경 PNG.

## 눈 감은 표정 추가

곰과 토끼의 기존 PNG를 각각 편집 대상으로 사용했다. 결과는 `app/src/main/assets/pets/bear-closed.png`, `rabbit-closed.png`에 저장했다. 각 1254 × 1254 투명 PNG이며 깜빡임·인사·성공 반응에서 사용한다.

Use case: precise-object-edit. Edit target: the attached transparent companion sprite. Change ONLY the two glossy open eyes into gently closed smiling eyelids (two small curved brown lines). Preserve exact canvas size, character scale, position, outline, ears, fur, paws, feet, smile, lighting, transparency, and every other pixel as closely as possible. No movement, no new objects, no text. This is the closed-eye animation frame for an Android pet and must align exactly with the reference. Output transparent PNG.

## 곰

Use case: stylized-concept. Asset type: production transparent Android companion character sprite. Create ONE adorable premium cream plush BEAR companion inspired by the warm character in the reference, isolated full body, front facing symmetric, sitting upright. Very round bean-shaped body/head unified, small rounded ears with peach interiors, tiny brown glossy eyes, tiny smiling mouth, soft blush, tiny feet, small resting paws. Sophisticated soft tactile micro-fleece, beautiful soft studio lighting from upper left, dimensional shading, polished collectible toy quality, calm friendly adult automotive companion, not cheap cartoon or low-poly. Center in square canvas with generous transparent margins, feet at 88% height, ears at 12%, character width about 65%. No pedestal, no cushion, no plant, no ground plane, no cast shadow outside character, no text, no UI. Genuine transparent background. Reference is style only; output only the character.

## 토끼

Use case: stylized-concept. Asset type: production transparent Android companion sprite. Reference image is the matching BEAR asset; create its RABBIT companion in precisely the same premium cream micro-fleece toy style, glossy chocolate eyes, tiny smile and blush, same soft lighting from upper left. One whole rabbit front facing, round unified bean head/body, two long soft ears with muted peach interiors, tiny feet, resting paws. Rabbit has narrower body than bear and distinct upright ears, calm appealing proportions. Match the bear's camera and quality. Square canvas, full character including ears and feet, generous transparent margins. Top ears at 10% image height, feet at 87% height, body width 58%. No floor, pedestal, accessories, text, watermark or external cast shadow. Genuine transparent background.
