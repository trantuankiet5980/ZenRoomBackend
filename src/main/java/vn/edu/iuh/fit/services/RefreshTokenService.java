package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.entities.RefreshToken;

import java.util.List;

public interface RefreshTokenService {
    void saveRefreshToken(RefreshToken refreshToken);
    RefreshToken findByToken(String token);
    boolean isTokenRevoke(String token);
    String getRefreshTokenByUser(String user);
    List<RefreshToken> getValidTokensByUserId(String userId);
}
