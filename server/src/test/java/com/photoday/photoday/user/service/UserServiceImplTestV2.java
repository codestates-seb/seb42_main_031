package com.photoday.photoday.user.service;

import com.photoday.photoday.excpetion.CustomException;
import com.photoday.photoday.image.entity.Image;
import com.photoday.photoday.image.entity.Report;
import com.photoday.photoday.image.service.S3Service;
import com.photoday.photoday.security.service.AuthUserService;
import com.photoday.photoday.security.utils.CustomAuthorityUtils;
import com.photoday.photoday.user.dto.UserDto;
import com.photoday.photoday.user.entity.User;
import com.photoday.photoday.user.mapper.UserMapper;
import com.photoday.photoday.user.repository.UserRepository;
import com.photoday.photoday.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@ExtendWith(SpringExtension.class)
public class UserServiceImplTestV2 {
    @InjectMocks
    UserServiceImpl userService;
    @Spy
    PasswordEncoder passwordEncoder;
    @Spy
    UserMapper userMapper;
    @Spy
    CustomAuthorityUtils customAuthorityUtils;
    @Mock
    UserRepository userRepository;
    @Mock
    AuthUserService authUserService;
    @Mock
    S3Service s3Service;

    private Long userId = 0L;

    @Test
    @DisplayName("createUser: 정상 입력")
    void createUserTest() {
        // given
        String email = "test@test.com";
        String password = "123456a!";
        User user = getUser(email);

        UserDto.Post post = new UserDto.Post(email, password);

        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        UserDto.Response response = userService.createUser(post);

        // then
        assertEquals(user.getUserId(), response.getUserId());
        assertEquals(user.getName(), response.getName());
        assertEquals(user.getDescription(), response.getDescription());
    }

    @Test
    @DisplayName("createUser: 이미 존재")
    void createUserAlreadyExistsTest() {
        // given
        String email = "test@test.com";
        String password = "123456a!";
        User user = getUser(email);

        UserDto.Post post = new UserDto.Post(email, password);

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.createUser(post));
        assertEquals(HttpStatus.CONFLICT, exception.getExceptionCode().getHttpStatus());
        assertEquals("이미 존재하는 이메일입니다.", exception.getExceptionCode().getMessage());
    }

    @Test
    @DisplayName("registerUserOAuth2: 정상 입력")
    void registerUserOAuth2Test() {
        // given
        User user = getUser("test@test.com");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        User response = userService.registerUserOAuth2(user);

        // then
        assertEquals(user.getUserId(), response.getUserId());
    }

    @Test
    @DisplayName("registerUserOAuth2: 중복 입력")
    void registerUserOAuth2LoginTest() {
        // given
        User user = getUser("test@test.com");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // when
        User response = userService.registerUserOAuth2(user);

        // then
        assertEquals(user.getUserId(), response.getUserId());
    }

    @Test
    @DisplayName("getUser: 정상 입력")
    void getUserTest() {
        // given
        User user = getUser("test@email.com");
        User loginUser = getUser("loginUser");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(authUserService.getLoginUser()).willReturn(Optional.of(loginUser));

        // when
        UserDto.Response response = userService.getUser(user.getUserId());

        // then
        assertNotEquals(loginUser.getUserId(), response.getUserId());
        assertEquals(user.getUserId(), response.getUserId());
    }

    @Test
    @DisplayName("getUser: 존재하지 않는 유저 조회")
    void getUserUserNotFoundTest() {
        // given
        User user = getUser("test@email.com");

        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUser(user.getUserId()));
        assertEquals(HttpStatus.NOT_FOUND, exception.getExceptionCode().getHttpStatus());
        assertEquals("회원 정보가 없습니다.", exception.getExceptionCode().getMessage());
    }

    @Test
    @DisplayName("getUser: AnonymousUser로 조회")
    void getUserAnonymousTest() {
        // given
        User user = getUser("test@email.com");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(authUserService.getLoginUser()).willReturn(Optional.empty());

        // when
        UserDto.Response response = userService.getUser(user.getUserId());

        // then
        assertEquals(user.getUserId(), response.getUserId());
        assertFalse(response.isCheckAdmin());
        assertFalse(response.isMyPage());
        assertFalse(response.isCheckFollow());
    }

    @Test
    @DisplayName("updateUser: 이미지, description 변경")
    void updateUserTest() throws IOException, NoSuchAlgorithmException {
        // given
        User user = getUser("test@email.com");
        UserDto.Update update = new UserDto.Update("유저 설명");
        MultipartFile multipartFile = new MockMultipartFile("image.jpg", "".getBytes());
        String imageUrl = "imageUrl";

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));
        given(s3Service.saveImage(any(MultipartFile.class))).willReturn(imageUrl);

        // when
        UserDto.Response response = userService.updateUser(update, multipartFile);

        // then
        assertEquals(update.getDescription(), response.getDescription());
        assertEquals(imageUrl, response.getProfileImageUrl());
    }

    @Test
    @DisplayName("updateUser: 이미지만 변경")
    void updateUserUpdateDtoNullTest() throws IOException, NoSuchAlgorithmException {
        // given
        User user = getUser("test@email.com");
        MultipartFile multipartFile = new MockMultipartFile("image.jpg", "".getBytes());
        String imageUrl = "imageUrl";

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));
        given(s3Service.saveImage(any(MultipartFile.class))).willReturn(imageUrl);

        // when
        UserDto.Response response = userService.updateUser(null, multipartFile);

        // then
        assertEquals(user.getDescription(), response.getDescription());
        assertEquals(imageUrl, response.getProfileImageUrl());
    }

    @Test
    @DisplayName("updateUser: description만 변경")
    void updateUserMultipartFileNullTest() throws IOException, NoSuchAlgorithmException {
        // given
        User user = getUser("test@email.com");
        UserDto.Update update = new UserDto.Update("유저 설명");

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));

        // when
        UserDto.Response response = userService.updateUser(update, null);

        // then
        assertEquals(user.getDescription(), response.getDescription());
    }

    @Test
    @DisplayName("updateUser: IOException")
    void updateUserIOExceptionTest() throws IOException, NoSuchAlgorithmException {
        // given
        User user = getUser("test@email.com");
        MultipartFile multipartFile = new MockMultipartFile("image.jpg", "".getBytes());

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));
        given(s3Service.saveImage(any(MultipartFile.class))).willThrow(IOException.class);

        // when & then
        assertThrows(RuntimeException.class, () -> userService.updateUser(null, multipartFile));
    }

    @Test
    @DisplayName("updateUser: NoSuchAlgorithmException")
    void updateUserNoSuchAlgorithmExceptionTest() throws IOException, NoSuchAlgorithmException {
        // given
        User user = getUser("test@email.com");
        MultipartFile multipartFile = new MockMultipartFile("image.jpg", "".getBytes());

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));
        given(s3Service.saveImage(any(MultipartFile.class))).willThrow(NoSuchAlgorithmException.class);

        // when & then
        assertThrows(RuntimeException.class, () -> userService.updateUser(null, multipartFile));
    }

    @Test
    @DisplayName("updateUserPassword: 정상 입력")
    void updateUserPasswordTest() {
        // given
        String password = "123456a!";
        UserDto.UpdateUserPassword update = new UserDto.UpdateUserPassword(password, password);
        User user = getUser("test@test.com");

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));

        // when
        userService.updateUserPassword(update);

        // then
        assertNotEquals(password, user.getPassword());
    }

    @Test
    @DisplayName("updateUserPassword: 비밀번호 확인 불일치")
    void updateUserPasswordPasswordNotMatchTest() {
        // given
        String password = "123456a!";
        UserDto.UpdateUserPassword update = new UserDto.UpdateUserPassword(password, password + "a");

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.updateUserPassword(update));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getExceptionCode().getHttpStatus());
        assertEquals("비밀번호가 일치하지 않습니다.", exception.getExceptionCode().getMessage());
    }

    @Test
    @DisplayName("deleteUser: 정상 입력: 유저 본인")
    void deleteUserTest() {
        // given
        User user = getUser("test@email.com");

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(anyLong());

        // when
        userService.deleteUser(user.getUserId());
    }

    @Test
    @DisplayName("deleteUser: 정상 입력: 관리자 삭제")
    void deleteUserAdminTest() {
        // given
        User admin = getAdmin();
        User user = getUser("test@mail.com");

        given(authUserService.getLoginUser()).willReturn(Optional.of(admin));
        doNothing().when(userRepository).deleteById(anyLong());

        // when
        userService.deleteUser(user.getUserId());
    }

    @Test
    @DisplayName("deleteUser: 정상 입력: 다른 유저")
    void deleteUserUserInfoNotMatchTest() {
        // given
        User user = getUser("test@mail.com");
        User anotherUser = getUser("wrongUser@mail.com");

        given(authUserService.getLoginUser()).willReturn(Optional.of(anotherUser));

        // when
        assertThrows(CustomException.class, () -> userService.deleteUser(user.getUserId()));
    }

    @Test
    @DisplayName("deleteProfileImage: 정상 입력(액세스 토큰)")
    void deleteProfileImageTest() {
        // given
        String defaultProfileUrl = "https://ifh.cc/g/zPrPfv.png";
        User user = getUser("test@mail.com");

        given(authUserService.getLoginUser()).willReturn(Optional.of(user));

        // when
        UserDto.Response response = userService.deleteProfileImage();

        // then
        assertEquals(defaultProfileUrl, response.getProfileImageUrl());
    }

    @Test
    @DisplayName("deleteProfileImage: 잘못된 입력(Anonymous)")
    void deleteProfileImageAnonymousTest() {
        // given
        given(authUserService.getLoginUser()).willReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.deleteProfileImage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getExceptionCode().getHttpStatus());
        assertEquals("회원 정보가 없습니다.", exception.getExceptionCode().getMessage());
    }

    @Test
    @DisplayName("checkUserReportCount: report 5 미만")
    void checkUserReportCountTest() {
        // given
        User user = getUser("test@mail.com");

        // when
        userService.checkUserReportCount(user);
    }

    @Test
    @DisplayName("checkUserReportCount: report 5 이상")
    void checkUserReportCountReportCountExceedsLimitTest() {
        // given
        User user = getUser("test@mail.com");

        Report report1 = new Report();
        report1.setUser(user);
        Report report2 = new Report();
        report2.setUser(user);
        Report report3 = new Report();
        report3.setUser(user);
        Report report4 = new Report();
        report4.setUser(user);
        Report report5 = new Report();
        report5.setUser(user);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> userService.checkUserReportCount(user));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getExceptionCode().getHttpStatus());
        assertEquals("신고 개수가 5개 이상입니다.", exception.getExceptionCode().getMessage());
    }

    @Test
    @DisplayName("findUserByEmail: 정상 입력")
    void findUserByEmailTest() {
        // given
        User user = getUser("test@email.com");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // when
        userService.findUserByEmail(user.getEmail());
    }

    @Test
    @DisplayName("findUserByEmail: 존재하지 않는 유저")
    void findUserByEmailUserNotFoundTest() {
        // given
        String email = "test@email.com";

        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when
        assertThrows(CustomException.class, () -> userService.findUserByEmail(email));
    }

    @Test
    @DisplayName("banUser: reportCount 10 미만")
    void banUserTest() {
        // given
        User user = getUser("test@email.com");
        Image image = Image.builder().user(user).build();

        // when
        userService.banUser(image);
    }

    @Test
    @DisplayName("banUser: reportCount 10 이상")
    void banUserBanTest() {
        // given
        User user = getUser("test@email.com");
        user.setReportedCount(10);
        Image image = Image.builder().user(user).build();

        // when
        userService.banUser(image);

        // then
        assertEquals(0, user.getReportedCount());
        assertEquals(User.UserStatus.USER_BANNED, user.getStatus());
        assertTrue(LocalDateTime.now().plusWeeks(1).isAfter(user.getBanTime()));
    }

    @Test
    @DisplayName("checkBanTime: active 유저")
    void checkBanTimeTest() {
        // given
        User user = getUser("test@email.com");

        // when
        userService.checkBanTime(user);
    }

    @Test
    @DisplayName("checkBanTime: banned 유저 -> 밴 해제")
    void checkBanTimeBannedUserTest() {
        // given
        User user = getUser("test@email.com");
        user.setStatus(User.UserStatus.USER_BANNED);
        user.setBanTime(LocalDateTime.now());


        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        userService.checkBanTime(user);

        // then
        assertNull(user.getBanTime());
        assertEquals(User.UserStatus.USER_ACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("checkBanTime: banned 유저 -> 밴 유지")
    void checkBanTimeBannedUserV2Test() {
        // given
        User user = getUser("test@email.com");
        user.setStatus(User.UserStatus.USER_BANNED);
        user.setBanTime(LocalDateTime.now().plusMinutes(5));


        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        userService.checkBanTime(user);

        // then
        assertNotNull(user.getBanTime());
        assertEquals(User.UserStatus.USER_BANNED, user.getStatus());
    }

    @Test
    @DisplayName("checkAdmin: 일반 유저 접속")
    void checkAdminTest() {
        // given
        User user = getUser("test@email.com");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

        // when
        boolean response = userService.checkAdmin(user.getUserId());

        // then
        assertFalse(response);
    }

    @Test
    @DisplayName("checkAdmin: Anonymous")
    void checkAdminAnonymousTest() {
        // given

        // when
        boolean response = userService.checkAdmin(null);

        // then
        assertFalse(response);
    }

    @Test
    @DisplayName("checkAdmin: 관리자")
    void checkAdminAdminTest() {
        // given
        User admin = getAdmin();

        given(userRepository.findById(anyLong())).willReturn(Optional.of(admin));

        // when
        boolean response = userService.checkAdmin(admin.getUserId());

        // then
        assertTrue(response);
    }

    private User getUser(String email) {
        return User.builder()
                .userId(getId())
                .email(email)
                .name("test")
                .roles(List.of("USER"))
                .build();
    }

    private User getAdmin() {
        return User.builder()
                .userId(getId())
                .email("admin@email.com")
                .name("test")
                .roles(List.of("USER", "ADMIN"))
                .build();
    }

    private Long getId() {
        userId += 1;
        return userId;
    }

}
