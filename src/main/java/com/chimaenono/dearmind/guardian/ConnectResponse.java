package com.chimaenono.dearmind.guardian;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectResponse {
    private boolean success;
    private String message;
}
