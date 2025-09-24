package com.chimaenono.dearmind.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class KakaoFriendsResponse {

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("elements")
    private List<KakaoFriend> elements;

    @Data
    public static class KakaoFriend {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("profile_nickname")
        private String profileNickname;

        @JsonProperty("profile_thumbnail_image")
        private String profileThumbnailImage;

        @JsonProperty("favorite")
        private Boolean favorite;
    }
}