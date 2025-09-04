package vn.edu.iuh.fit.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entities.RefreshToken;
import vn.edu.iuh.fit.exceptions.TokenNotFoundException;
import vn.edu.iuh.fit.repositories.RefreshTokenRepository;
import vn.edu.iuh.fit.services.RefreshTokenService;

import java.util.List;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public void saveRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByRefreshToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Không tìm thấy refresh token: " + token));
    }

    @Override
    public boolean isTokenRevoke(String token) {
        return refreshTokenRepository.findByRefreshToken(token)
                .map(RefreshToken::isRevoked)
                .orElse(true);
    }

    @Override
    public String getRefreshTokenByUser(String userId) {
        RefreshToken refreshToken = refreshTokenRepository.findTopByUser_UserIdAndRevokedFalseOrderByExpiresDateDesc(userId);
        return refreshToken != null ? refreshToken.getRefreshToken() : null;
    }


    @Override
    public List<RefreshToken> getValidTokensByUserId(String userId) {
        return refreshTokenRepository.findAllByUser_UserIdAndRevokedFalse(userId);
    }
}