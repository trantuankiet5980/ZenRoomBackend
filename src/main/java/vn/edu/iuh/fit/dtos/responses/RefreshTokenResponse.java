package vn.edu.iuh.fit.dtos.responses;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RefreshTokenResponse {
    private String accessToken;
    private String type;
    private long expiry;

    // Make the constructor public
    public RefreshTokenResponse(String accessToken, String type, long expiry) {
        this.accessToken = accessToken;
        this.type = type;
        this.expiry = expiry;
    }

}