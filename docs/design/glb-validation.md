# Compose 3D 홈 검증

- 작성일: 2026-10-26
- 상태: 임시 GLB와 debug 검증 화면 구현 / Android 전체 빌드·실제 GPU 렌더링 미검증
- 대상: 사용자가 설정한 Pleos Connect v2.1.0 AVD.

## 구현 범위

홈의 **‘3D 홈 검증 열기’**에서 Compose 기반 검증 화면으로 진입한다. 기존 캐릭터 선택·홈은 유지하며, 이 단계에서는 별도 화면으로 GLB 렌더링 가능성을 확인한다.

- Filament 1.75.1의 core·gltfio·utils를 동일 버전의 debug 의존성으로 추가했다. release에는 이 검증 화면과 네이티브 라이브러리를 포함하지 않는다.
- `app/src/debug/assets/models/companion-probe.glb`는 직접 생성한 약 240KB의 공용 모델이다. 외부 모델·텍스처·실행 중 다운로드가 없다.
- 실제 3차원 메시·PBR 재질·노드 애니메이션을 사용한다. 기존 Canvas 시안이나 이미지 확대·축소가 아니다.
- `Idle`은 숨쉬기·눈 깜빡임, `Greeting`은 손 인사다. 현재는 노드 변환 애니메이션이며 최종 캐릭터의 스켈레톤·표정 리깅 검증은 별도다.
- 곰·토끼 선택과 관계없이 같은 임시 모델을 표시한다. 두 최종 캐릭터의 3D 제작을 완료한 것이 아니다.
- Compose UI 안에 TextureView를 포함하고 ModelViewer로 표시한다. 자동 재생 대신 앱에서 두 클립의 시간을 제어한다.
- 일시정지·백그라운드에서는 프레임 요청을 중단한다. 복귀 시 숨겨진 동안의 시간을 건너뛰어 애니메이션을 이어간다. 화면을 닫으면 자식 View의 detach를 통해 ModelViewer가 GPU 자원을 해제한다.
- 상수 주변광(1-band SH)과 방향광으로 부드러운 명암을 구성한다. MSAA 4x·FXAA·접촉 음영을 적용하고 동적 해상도 축소는 끈다. 별도 반사 환경맵·텍스처는 아직 사용하지 않는다. 최종 주야간 조명과 성능은 후속 기기 검증 대상이다.
- 실제 주행 제한·Window·미션·경험치와는 연결하지 않는다. 에뮬레이터용 개발 화면이다.

## 실행 순서

1. Android Studio에서 **Gradle Sync**를 실행해 Filament 의존성을 받는다. 이번에 새 라이브러리를 추가했으므로 최초 Sync에는 네트워크 연결이 필요하다.
2. debug 앱을 빌드하고 Pleos Connect v2.1.0에서 실행한다.
3. 캐릭터 홈의 **‘3D 홈 검증 열기’**를 누른다.
4. ‘리소스 로딩 완료’ 상태와 실제 모델 표시를 모두 확인한다. 상태 문구만으로 시각 렌더링 성공을 판단하지 않는다.
5. 숨쉬기·눈 깜빡임을 관찰하고 **‘3D 인사 재생’**을 누른다.
6. 일시정지 → 재개, 다른 앱으로 전환 → 복귀, 기존 홈으로 돌아가기 → 재진입을 확인한다.
7. 화면 방향·창 크기 변경과 반복 재진입에서 오류·검은 화면·정지·자원 증가가 있는지 확인한다.

‘렌더 제출’은 최근 보고 구간에서 renderer가 프레임을 제출한 횟수다. 실제 표시 FPS·GPU 시간·발열 측정값이 아니다. 성능 통과 판단에는 별도 프로파일링이 필요하다.

실행 오류는 화면의 메시지 또는 `adb logcat -s GlbProbe`로 확인한다. 라이브러리 Sync 실패와 네이티브 그래픽 오류를 구분한다.

## 검증 상태

| 항목 | 결과 |
| --- | --- |
| GLB 생성 | 완료. 표준 라이브러리로 재현 가능 |
| GLB 구조 | 헤더·청크·버퍼 범위·인덱스·삼각형 방향·법선·애니메이션 시간·타깃·쿼터니언 검사 통과 |
| XML·버전 카탈로그 | 파싱과 debug 의존성 범위 확인 |
| Android 리소스 | main/debug `aapt2 compile` 통과. 리소스 링크·패키징은 미확인 |
| Filament 바이너리 | 사용자 Sync 이후 로컬에 1.75.1 AAR 확인. 품질 개선 코드를 포함한 main/debug Kotlin을 실제 Filament·Compose 바이너리와 컴파일러로 타입 검사하여 통과. 검사용 임시 R 상수 사용으로 APK 빌드와 구분 |
| 전체 debug 빌드 | wrapper 캐시 `.zip.lck` 쓰기 제한으로 Gradle 태스크 진입 전 실패 |
| Pleos GPU 렌더링·기기 성능 | 아직 확인하지 못함 |

구조 검사는 Khronos의 전체 glTF Validator나 기기 실행을 대신하지 않는다. 기존 Compose 홈의 사용자 실행 확인을 이번 3D 화면의 검증 결과로 확대하지 않는다.

## 파일과 재현

- `app/src/debug/java/com/pleos/mate/feature/developer/presentation/GlbValidationActivity.kt`: Compose 화면과 lifecycle 연결.
- `app/src/debug/java/com/pleos/mate/feature/developer/presentation/GlbProbeView.kt`: GLB·애니메이션·렌더 요청·정지·자원 수명 관리.
- `tools/generate_probe_glb.py`: 임시 모델 생성.
- `tools/validate_probe_glb.py`: 독립적인 구조 검사.

```sh
python3 tools/generate_probe_glb.py
python3 tools/validate_probe_glb.py
./gradlew :app:assembleDebug
```

## 2026-10-26 품질 개선

사용자가 기존 3D 화면의 낮은 품질을 보고하여 공용 곰형 모델과 렌더 설정을 개선했다. 이전 화면이 표시됐다는 사용자 보고와, 이번 수정본의 실행 검증은 구분한다.

- 구체의 분할을 위도 16·경도 24에서 위도 48·경도 64로 늘리고, 별도 몸통 곡면과 그에 맞는 법선을 생성했다.
- 입체 주둥이·코·곡선 미소·눈 하이라이트·볼·양손을 추가했다. 눈 깜빡임에는 하이라이트도 함께 움직인다.
- 눈·몸·귀·주둥이·바닥 재질의 거칠기와 색상을 구분하고, 기존 재질 발광은 제거했다.
- 간접광·접촉 음영·MSAA 4x·FXAA·고품질 HDR 버퍼로 명암과 윤곽을 개선했다. 뷰포트의 동적 축소를 비활성화했다.
- 검증 영역을 320dp에서 380dp로 늘리고 카메라 초점거리를 조정했다.
- 추가한 IndirectLight는 TextureView detach에서 ModelViewer 엔진 해제 전에 명시적으로 제거한다.
- 모델 약 19KB → 240KB. 기기에서 GPU 비용과 프레임 안정성을 다시 측정해야 하며, 이번 설정이 모든 차량에서 성능 목표를 만족한다고 판단하지 않는다.

[모델 형태 미리보기](./probe-model-quality-preview.png)는 생성한 GLB 메시를 별도 소프트웨어 렌더러로 확인한 이미지다. 실제 Filament 화면·조명·해상도나 최종 디자인 품질을 증명하는 스크린샷이 아니다.

검증 결과: 실제 1.75.1 바이너리의 옵션 API를 대조하고, Kotlin·Compose 소스 타입 검사, main/debug 리소스 컴파일, GLB 구조·법선·삼각형 방향 검사를 통과했다. 새 버전의 에뮬레이터 화면은 미확인이다.

## 다음 판단

실제 AVD에서 모델·움직임·중단·재진입을 확인한 뒤 곰·토끼 최종 모델 제작과 홈 렌더 영역 교체를 진행한다. 현재 단계에서는 디자인 시안 수준의 질감이나 차종 공통 성능을 약속하지 않는다.

기준 버전은 Maven Central에서 배포 정보를 확인한 [Filament 1.75.1](https://central.sonatype.com/artifact/com.google.android.filament/filament-android/1.75.1)이다. [utils POM](https://central.sonatype.com/artifact/com.google.android.filament/filament-utils-android)은 같은 버전의 core와 gltfio를 참조한다. [ModelViewer v1.75.1 소스](https://github.com/google/filament/blob/v1.75.1/android/filament-utils-android/src/main/java/com/google/android/filament/utils/ModelViewer.kt)의 자동 재생 제어·로딩 진행·Boolean render 반환·detach 해제 API를 대조했다.

초기에는 GitHub 릴리스만 확인하여 1.78.1을 지정했으나, 사용자 빌드에서 Maven 의존성을 찾지 못했다. 2026-10-26에 1.75.1로 수정했다. Configuration Cache 오류는 의존성 해석 실패를 감싼 메시지이므로 캐시 설정이나 저장소를 추가하지 않았다.
