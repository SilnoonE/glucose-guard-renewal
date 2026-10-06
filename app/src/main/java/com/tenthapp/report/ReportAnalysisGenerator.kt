package com.example.glucoseguard.report
class ReportAnalysisGenerator {
    fun generateAnalysisText(stats: ReportStats): List<String> = buildList {
        if(stats.avgGlucose > 0) {
            add("입력 기록의 평균은 ${stats.avgGlucose} mg/dL, 최저 ${stats.minGlucose}, 최고 ${stats.maxGlucose}입니다. 평균만으로 개별 수치나 현재 상태를 판단할 수 없습니다.")
            add("측정 구분별 목표 범위 안에 있는 기록의 비율은 ${stats.normalRangePercentage}%입니다. 이 비율은 연속 측정의 목표 범위 내 시간 비율이 아닙니다.")
        }
        if(stats.lowCount > 0) add("70 mg/dL 미만 기록 ${stats.lowCount}회가 있습니다. 발생 시각과 맥락을 의료진과 검토하세요. 과거 기록만으로 지금 당분을 섭취해야 한다고 판단할 수 없습니다.")
        if(stats.highCount > 0) add("설정한 측정 구분별 상한을 넘은 기록 ${stats.highCount}회가 있습니다. 식사·활동 메모와 함께 확인하세요.")
        if(stats.insulinCount > 0) add("입력한 인슐린 기록은 ${stats.insulinCount}회입니다. 기록 누락이 있을 수 있어 이 정보만으로 투여 횟수나 용량을 평가할 수 없습니다.")
        add("기록하지 않은 시간은 분석에 포함되지 않습니다. 약물 및 인슐린 변경은 의료진과 정한 계획에 따라야 합니다.")
        add("이 문서는 직접 입력한 기록을 정리한 참고 자료이며 진단이나 치료 권고가 아닙니다.")
    }
}
