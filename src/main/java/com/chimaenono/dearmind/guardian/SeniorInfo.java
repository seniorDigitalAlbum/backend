package com.chimaenono.dearmind.guardian;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeniorInfo {
    private Long id;
    private String name;
    private String profileImage;
    private String kakaoId;
    private String kakaoNickname;
    private String kakaoProfileImage;
}
