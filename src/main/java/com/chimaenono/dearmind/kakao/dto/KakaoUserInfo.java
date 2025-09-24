package com.chimaenono.dearmind.kakao.dto;



import com.chimaenono.dearmind.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;



@Data

public class KakaoUserInfo {



    @JsonProperty("id")

    private Long kakaoId;



    @JsonProperty("connected_at")

    private String connectedAt;



    @JsonProperty("kakao_account")

    private KakaoAccount kakaoAccount;



    @Data

    public static class KakaoAccount {



        @JsonProperty("profile_nickname_needs_agreement")

        private Boolean profileNicknameNeedsAgreement;



        private Profile profile;



        @JsonProperty("has_gender")

        private Boolean hasGender;



        @JsonProperty("gender_needs_agreement")

        private Boolean genderNeedsAgreement;



        private String gender;



        @Data

        public static class Profile {

            private String nickname;



            @JsonProperty("thumbnail_image_url")

            private String thumbnailImageUrl;



            @JsonProperty("profile_image_url")

            private String profileImageUrl;

            

            @JsonProperty("is_default_image")

            private Boolean isDefaultImage;

        }

    }



    // 편의 메서드 (기존과 동일)

    public String getNickname() {

        return kakaoAccount != null && kakaoAccount.getProfile() != null

                ? kakaoAccount.getProfile().getNickname()

                : null;

    }



    public String getProfileImageUrl() {

        return kakaoAccount != null && kakaoAccount.getProfile() != null

                ? kakaoAccount.getProfile().getProfileImageUrl()

                : null;

    }



    public String getThumbnailImageUrl() {

        return kakaoAccount != null && kakaoAccount.getProfile() != null

                ? kakaoAccount.getProfile().getThumbnailImageUrl()

                : null;

    }



    public String getGender() {

        return kakaoAccount != null ? kakaoAccount.getGender() : null;

    }



    public User.Gender getGenderAsEnum() {

        String gender = getGender();

        if ("male".equalsIgnoreCase(gender)) {

            return User.Gender.MALE;

        } else if ("female".equalsIgnoreCase(gender)) {

            return User.Gender.FEMALE;

        }

        return null;

    }

}

