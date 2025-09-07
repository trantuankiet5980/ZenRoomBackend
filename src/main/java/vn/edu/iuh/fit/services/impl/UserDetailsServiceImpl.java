package vn.edu.iuh.fit.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.auths.UserPrincipal;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.repositories.UserRepository;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + username));

        UserResponse userResponse = UserResponse.builder()
                .userId(user.getUserId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .roleName(user.getRole().getRoleName())
                .build();

        return UserPrincipal.builder()
                .username(user.getPhoneNumber())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getRoleName().toUpperCase()))) // Convert Role to String
                .userResponse(userResponse)
                .build();
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
    }
}