package com.example.glucoseguard.report

import java.util.Locale

class ReportAnalysisGenerator {

    fun generateAnalysisText(stats: ReportStats): List<String> {
        val analysis = mutableListOf<String>()

        // 1. 평균 혈당 분석
        analysis.add("📊 평균 혈당 분석: 선택 기간 평균 혈당은 ${stats.avgGlucose}mg/dL입니다.")
        when {
            stats.avgGlucose in 70..140 -> analysis.add("✅ 현재 목표 범위 내에서 매우 안정적으로 유지되고 있습니다. 현재의 식습관과 활동량을 유지하세요.")
            stats.avgGlucose in 141..180 -> analysis.add("⚠️ 평균 혈당이 다소 높습니다. 식사 후 가벼운 산책이나 탄수화물 섭취 조절을 고려해보세요.")
            stats.avgGlucose > 180 -> analysis.add("🚨 평균 혈당이 목표치를 크게 상회하고 있습니다. 의료진과 상담하여 약제 조절이나 식단 교정이 필요할 수 있습니다.")
            stats.avgGlucose > 0 && stats.avgGlucose < 70 -> analysis.add("⚠️ 평균 혈당이 낮아 저혈당 위험이 있습니다. 잦은 저혈당은 위험하므로 간식 섭취 등 대처가 필요합니다.")
        }

        // 2. 변동성 및 극단치 분석
        if (stats.maxGlucose > 250) {
            analysis.add("🚩 고혈당 경고: 최고 혈당이 ${stats.maxGlucose}mg/dL로 매우 높게 나타난 기록이 있습니다. 급격한 혈당 상승 요인을 파악해야 합니다.")
        }
        
        val range = stats.maxGlucose - stats.minGlucose
        if (range > 150) {
            analysis.add("🔄 혈당 변동성: 최저와 최고 차이가 ${range}mg/dL로 큽니다. 혈당 변동성이 크면 혈관 건강에 무리가 갈 수 있으므로 주의가 필요합니다.")
        }

        // 3. 고혈당/저혈당 빈도 분석
        if (stats.highCount > 0) {
            analysis.add("📈 고혈당 패턴: 고혈당 기록이 ${stats.highCount}회 발견되었습니다. 주로 발생하는 시간대를 확인하여 식후 인슐린이나 활동량을 조절해보세요.")
        }

        if (stats.lowCount > 0) {
            analysis.add("📉 저혈당 주의: 저혈당이 ${stats.lowCount}회 발생했습니다. 저혈당 무감지증 예방을 위해 즉각적인 당분 섭취와 원인 파악이 중요합니다.")
        }

        // 4. 인슐린 투여 분석
        if (stats.insulinCount > 0) {
            analysis.add("💉 인슐린 분석: 총 ${stats.insulinCount}회의 투여 기록이 있으며, 1회 평균 투여량은 ${String.format(Locale.getDefault(), "%.1f", stats.avgInsulin)}U입니다.")
            if (stats.avgGlucose > 180 && stats.insulinCount < 3) {
                analysis.add("💡 팁: 혈당이 높은데 투여 횟수가 적다면, 의료진과 상의하여 투여 횟수나 용량 증가를 검토해보세요.")
            }
        }

        // 5. 기록 습관 분석
        analysis.add("📝 기록 습관: 꾸준한 기록은 패턴 파악의 핵심입니다. 현재의 기록 습관을 유지하시기 바랍니다.")

        // 6. 안내 문구
        analysis.add("🔍 안내: 본 리포트는 입력된 데이터를 기반으로 한 참고용이며, 정확한 의학적 판단은 반드시 전문의와 상의하십시오.")

        return analysis
    }
}
