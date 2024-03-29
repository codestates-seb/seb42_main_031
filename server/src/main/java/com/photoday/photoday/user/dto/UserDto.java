package com.photoday.photoday.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class UserDto {
    //TODO: test할때 builder 적용으로 수정하기
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Post {
        @Pattern(regexp = "[0-9a-zA-Z]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$", message = "유효한 이메일 주소가 아닙니다.")
        @NotNull
        private String email;

        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*\\W).{8,20}$", message = "비밀번호는 영문과 특수문자, 숫자를 포함하여 8자 이상이고 20자 이하여야 합니다.")
        @NotNull
        private String password;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update {
        @Pattern(regexp = "^.{1,200}$", message = "자기소개란은 1자 이상 200자 이하여야 합니다.")
        private String description;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class UpdateUserPassword {
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*\\W).{8,20}$", message = "비밀번호는 영문과 특수문자, 숫자를 포함하여 8자 이상이고 20자 이하여야 합니다.")
        @NotNull
        private String password;

        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*\\W).{8,20}$", message = "비밀번호는 영문과 특수문자, 숫자를 포함하여 8자 이상이고 20자 이하여야 합니다.")
        @NotNull
        private String checkPassword;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Response {
        private Long userId;
        private String name;
        private String profileImageUrl;
        private String description;
        private int likeCount;
        private int reportCount; //필드 이름을 reportedCount로 했어야 덜 헷갈렸을 듯.
        private int followerCount;
        private int followingCount;
        private boolean checkFollow;
        private boolean myPage;
        private boolean checkAdmin;
    }
}
