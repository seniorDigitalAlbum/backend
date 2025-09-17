package com.chimaenono.dearmind.diary;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryPlan {
    
    // 1) 문단 계획 (문단 번호 -> 세그먼트 인덱스)
    private Map<String, Integer> paragraphPlan;
    
    // 2) 스타일 힌트
    private Map<String, String> styleHints;
    
    // 3) 세그먼트 정보 리스트
    private List<DPSeg> segments = new ArrayList<>();
    
    // 4) 감정 흐름 정보
    private String flowPattern;
    private Map<String, Object> turningPoint;
    private Map<String, Object> opening;
    private Map<String, Object> closing;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DPSeg {
        private int index;
        private int start;
        private int end;
        private String dominant;
        private String micro;
        private Map<String, Object> anchors;
        private String quote;
    }
}
