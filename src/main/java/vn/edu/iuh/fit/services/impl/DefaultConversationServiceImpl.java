package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entities.Conversation;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.repositories.ConversationRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.DefaultConversationService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultConversationServiceImpl implements DefaultConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createConversationWithAdmin(User user) {
        if (user == null) {
            return;
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        if (roleName != null && "ADMIN".equalsIgnoreCase(roleName)) {
            return;
        }

        Optional<User> adminOpt = userRepository.findByRole_RoleName("ADMIN").stream()
                .filter(admin -> !Objects.equals(admin.getUserId(), user.getUserId()))
                .filter(admin -> admin.getStatus() == null || admin.getStatus() == UserStatus.ACTIVE)
                .findFirst();

        if (adminOpt.isEmpty()) {
            return;
        }

        User admin = adminOpt.get();
        if (conversationRepository.findByUsersAnyOrder(user.getUserId(), admin.getUserId()).isPresent()) {
            return;
        }

        User tenant = user;
        User landlord = admin;
        if (roleName != null && "LANDLORD".equalsIgnoreCase(roleName)) {
            tenant = admin;
            landlord = user;
        }

        Conversation conversation = Conversation.builder()
                .tenant(tenant)
                .landlord(landlord)
                .createdAt(LocalDateTime.now())
                .build();

        conversationRepository.save(conversation);
    }
}
