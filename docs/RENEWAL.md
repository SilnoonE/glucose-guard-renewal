# 리뉴얼 과정

## 1. 기록을 시작하기 쉽게

홈에서 최근 측정값과 측정 시점을 바로 확인하고 혈당 입력으로 이동하도록 정리했습니다. 입력은 값과 측정 구분에 집중하도록 단순화했습니다.

관련 파일: `ui/home/HomeFragment.kt`, `ui/record/GlucoseInputFragment.kt`, `fragment_home.xml`, `fragment_glucose_input.xml`.

## 2. 화면의 기준과 시각 언어 통일

청록색 강조와 밝은 배경으로 홈, 기록, 요약, 관리 화면을 맞췄습니다. 메모 카드는 진한 청록색 테두리를 사용합니다. 목표는 공복·식전과 식후·기타로 나누며 공통 판정 로직을 적용합니다.

관련 파일: `util/GlucosePolicy.kt`, `util/TargetPreferences.kt`, `ui/adapter/RecordAdapter.kt`, `ui/chart/ChartFragment.kt`, `ui/settings/GuideTargetFragment.kt`, `colors.xml`, `themes.xml`, `item_memo.xml`.

## 3. 알림과 백업 동작 보강

알림 설정 변경 시 예약을 갱신하고 기기 재시작 시 설정을 반영하도록 처리했습니다. 백업은 기록 종류를 함께 내보내며 복원 전 형식을 검사합니다. DB 마이그레이션 및 복원 경로를 테스트로 확인했습니다.

관련 파일: `ReminderScheduler.kt`, `NotificationReceiver.kt`, `util/BackupCodec.kt`, `data/database/AppDatabase.kt`, `ui/settings/SettingsFragment.kt`.

## 4. 기록을 진료에 가져갈 수 있게

PDF는 선택 기간의 혈당·인슐린·메모와 사실 중심 요약을 구성합니다. 긴 메모와 다중 페이지 처리를 보강했습니다. 전면광고가 끝나거나 표시되지 않아도 보고서 열기를 진행합니다.

관련 파일: `report/PdfReportGenerator.kt`, `report/ReportDataBuilder.kt`, `report/ReportAnalysisGenerator.kt`, `ads/PdfInterstitial.kt`.

## 5. 광고와 스토어 표현 정리

광고 SDK 초기화 및 로드 상태 처리를 보강했습니다. 공개본은 모든 광고를 공식 테스트 ID로 대체했습니다. 소개 이미지는 기록, 홈, PDF, 목표, 차트·메모 순서입니다. 실제 화면을 참고해 생성 도구로 제작했으며 원본과 완전히 동일한 픽셀 캡처는 아닙니다.

## 검증 범위

운영 소스 리뉴얼 작업에서 debug/release 빌드, lint 및 에뮬레이터 기반 데이터/광고 테스트를 수행했습니다. 공개본은 식별자와 광고 값을 대체했으므로 빌드와 단위 테스트를 별도로 확인합니다. 광고 로드는 네트워크 상태에 따라 달라질 수 있습니다. 기존 lint 경고를 전부 해소한 작업은 아닙니다.

`before-renewal`은 보존된 개선 전 앱 모듈, `after-renewal`은 공개용 현재 코드입니다. 두 커밋 사이의 diff에 주요 변경과 추가된 테스트가 포함됩니다.
