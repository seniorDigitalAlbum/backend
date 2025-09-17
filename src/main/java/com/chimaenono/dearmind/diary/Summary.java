package com.chimaenono.dearmind.diary;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Summary {
    
    private String situation;
    private List<String> events;
    private Anchors anchors;
    private Highlights highlights;
    private List<String> quotes;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Anchors {
        private List<String> people;
        private List<String> place;
        private String era;
        private List<String> objects;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Highlights {
        private String bestMoment;
        private String hardMoment;
        private String insight;
    }
}
