package com.photoday.photoday.security.filter;

import com.photoday.photoday.security.jwt.JwtProvider;
import com.photoday.photoday.security.principaldetails.PrincipalDetailsService;
import com.photoday.photoday.security.utils.CustomAuthorityUtils;
import com.photoday.photoday.user.entity.User;
import com.photoday.photoday.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JwtVerificationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final CustomAuthorityUtils customAuthorityUtils;
    private final PrincipalDetailsService principalDetailsService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        try {
            if (!Objects.isNull(accessToken) && accessToken.startsWith("Bearer") || requestURI.equals("/api/auth/reissue")) {
                if (requestURI.equals("/api/auth/reissue")) {
                    String refreshToken = jwtProvider.getRefreshTokenFromRequest(request);
                    jwtProvider.verifyRefreshToken(refreshToken);
                    setAuthenticationForReissue(refreshToken);
                } else {
                    Map<String, Object> claims = verifyJws(request);
                    String username = (String) claims.get("username");
                    checkUserStatus(username);
                    setAuthenticationToContext(claims);
                }
            }
        } catch (DisabledException ce) {
            request.setAttribute("exception", ce);
        } catch (SignatureException se) {
            request.setAttribute("exception", se);
        } catch (ExpiredJwtException ee) {
            request.setAttribute("exception", ee);
        } catch (Exception e) {
            request.setAttribute("exception", e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        return authorization == null || !authorization.startsWith("Bearer");
    }

    private Map<String, Object> verifyJws(HttpServletRequest request) {
        String jws = request.getHeader("Authorization").replace("Bearer ", "");
        String base64EncodedSecretKey = jwtProvider.encodeBase64SecretKey(jwtProvider.getSecretKey());
        Map<String, Object> claims = jwtProvider.getClaims(jws, base64EncodedSecretKey).getBody();

        return claims;
    }

    private void setAuthenticationToContext(Map<String, Object> claims) {
        String username = (String) claims.get("username");
        List<GrantedAuthority> authorities = customAuthorityUtils.createAuthorities((List) claims.get("roles"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAuthenticationForReissue(String refreshToken) {
        log.info(refreshToken);
        String subject = jwtProvider.getSubject(refreshToken);
        UserDetails userDetails = principalDetailsService.loadUserByUsername(subject);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void checkUserStatus(String email) {
        User user = userService.findUserByEmail(email);
        userService.checkBanTime(user);
        if (user.getStatus().equals(User.UserStatus.USER_BANNED)) {
            throw new DisabledException("유저가 밴 상태입니다." + user.getBanTime() + " 이후에 서비스 이용이 가능합니다.");
        }
    }
}
