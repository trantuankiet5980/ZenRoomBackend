    package vn.edu.iuh.fit.services;

    import vn.edu.iuh.fit.dtos.requests.LoginRequest;
    import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
    import vn.edu.iuh.fit.dtos.responses.LoginResponse;
    import vn.edu.iuh.fit.dtos.responses.RefreshTokenResponse;
    import vn.edu.iuh.fit.entities.User;

    public interface AuthService {
        User getCurrentUser();
        String getCurrentUserId();
        boolean signUp(SignUpRequest signUpRequest);
//        LoginResponse login(LoginRequest loginRequest);
        void logout(String token);
        RefreshTokenResponse refreshToken(String refreshToken);
        void resetPassword(String phoneNumber, String newPassword);
    }
