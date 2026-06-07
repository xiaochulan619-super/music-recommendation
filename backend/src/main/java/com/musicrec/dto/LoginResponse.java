package com.musicrec.dto;

import com.musicrec.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;

        public static UserInfo from(User user) {
            UserInfo info = new UserInfo();
            info.id = user.getId();
            info.username = user.getUsername();
            info.nickname = user.getNickname();
            return info;
        }
    }
}
