# 공개본 검증

검증일: 2026-10-07

운영 ID를 제거한 이 공개본에서 다음 명령을 실행했습니다.

```sh
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest --console=plain
```

- debug APK 빌드 성공
- release APK 빌드 및 release 필수 lint 검사 성공
- 단위 테스트 9개 통과, 실패/오류/건너뛰기 0개
- 작업 파일 및 전체 Git 커밋의 공개 정보 검사 통과

서명 키를 포함하지 않으므로 release 결과물은 운영 서명 배포물이 아닙니다. 공개본에서 에뮬레이터 계측 테스트를 다시 실행하지는 않았습니다. 원래 리뉴얼 소스에서는 데이터 복원 및 광고 흐름 계측 테스트를 수행했습니다.
