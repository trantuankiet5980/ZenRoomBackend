    package vn.edu.iuh.fit.services;

    import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
    import vn.edu.iuh.fit.entities.User;

    public interface AuthService {
        User getCurrentUser();
        String getCurrentUserId();
        boolean signUp(SignUpRequest signUpRequest);
//        LoginResponse login(LoginRequest loginRequest);
        void logout(String token);
        void resetPassword(String phoneNumber, String newPassword);
        void changePassword(String currentPassword, String newPassword);
    }
