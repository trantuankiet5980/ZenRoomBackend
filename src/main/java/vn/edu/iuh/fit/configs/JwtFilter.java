package vn.edu.iuh.fit.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.edu.iuh.fit.auths.UserPrincipal;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.services.RefreshTokenService;
import vn.edu.iuh.fit.services.impl.UserDetailsServiceImpl;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final JwtDecoder jwtDecoder;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenService refreshTokenService;

    public JwtFilter(JwtUtil jwtUtil, JwtDecoder jwtDecoder, UserDetailsServiceImpl userDetailsService, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.jwtDecoder = jwtDecoder;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String userId = jwt.getSubject();

                if (userId != null && !userId.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(userId);

                    // Lấy user entity từ DB (nếu cần)
                    User user = userDetailsService.getUserById(userId); // sửa lại lấy đúng userId, tránh toString()

                    // Kiểm tra refresh token có bị thu hồi không
                    String refreshToken = refreshTokenService.getRefreshTokenByUser(userId);
                    if (refreshToken != null && refreshTokenService.isTokenRevoke(refreshToken)) {
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Your session has expired. Please log in again.");
                        return;
                    }

                    if (jwtUtil.isTokenValid(jwt, userPrincipal)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException ex) {
                // Khi token không hợp lệ hoặc decode lỗi
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}