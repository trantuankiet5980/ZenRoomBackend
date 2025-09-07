    package vn.edu.iuh.fit.services;

    import vn.edu.iuh.fit.dtos.requests.LoginRequest;
    import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
    import vn.edu.iuh.fit.dtos.responses.LoginResponse;
    import vn.edu.iuh.fit.dtos.responses.RefreshTokenResponse;

    public interface AuthService {
        boolean signUp(SignUpRequest signUpRequest);
//        LoginResponse login(LoginRequest loginRequest);
        void logout(String token);
        RefreshTokenResponse refreshToken(String refreshToken);
    }
