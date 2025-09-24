package com.chimaenono.dearmind.guardian;

import lombok.Data;
import java.util.List;

@Data
public class SearchSeniorsRequest {
    private List<String> phoneNumbers;
}
