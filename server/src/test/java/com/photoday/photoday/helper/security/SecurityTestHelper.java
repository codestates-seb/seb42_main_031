package com.photoday.photoday.helper.security;

import com.photoday.photoday.security.jwt.JwtProvider;
import com.photoday.photoday.user.entity.User;
import com.photoday.photoday.user.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@Component
public class SecurityTestHelper {
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private JwtProvider jwtProvider;

    public String getAccessToken(String email, List<String> roles) {
        User user = new User();
        user.setEmail(email);
        user.setRoles(roles);
        String accessToken = jwtProvider.delegateAccessToken(user);
        given(userServiceImpl.findUserByEmail(anyString())).willReturn(user);
        return accessToken;
    }

}
