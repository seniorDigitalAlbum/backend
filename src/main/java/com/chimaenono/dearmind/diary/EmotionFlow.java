package com.chimaenono.dearmind.diary;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionFlow {
    
    private List<Segment> segments;
    private Metrics metrics;
    private Map<String, Object> params;
    private String inputHash;
    private String generatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private int startTurn;
        private int endTurn;
        private String dominant;
        private double meanConf;
        private double valenceMean;
        private double arousalMean;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private int flips;
        private double positiveRatio;
        private int longestNegativeRun;
        private int peakArousalTurn;
        private String pattern;
    }
}
