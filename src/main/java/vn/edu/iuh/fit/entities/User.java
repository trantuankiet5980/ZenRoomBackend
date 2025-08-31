package vn.edu.iuh.fit.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


import vn.edu.iuh.fit.entities.enums.UserStatus;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Users")
public class User {
    @Id
    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    private String userId;

    @PrePersist
    void prePersist() {
        if (userId == null) userId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @Column(length = 100)
    private String fullName;

    @Column(length = 15, unique = true)
    private String phoneNumber;

    @Column(length = 100, unique = true)
    private String email;

    @JsonIgnore
    @Column(length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    private String avatarUrl;
    private LocalDateTime lastLogin;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* ---------------- Relations ---------------- */

    // Properties (landlord)
    @OneToMany(mappedBy = "landlord", fetch = FetchType.LAZY)
    private List<Property> properties = new ArrayList<>();

    // Bookings (tenant)
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    // Reviews (tenant)
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    // Review replies (landlord)
    @OneToMany(mappedBy = "landlord", fetch = FetchType.LAZY)
    private List<ReviewReply> reviewReplies = new ArrayList<>();

    // Favorites
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<Favorite> favorites = new ArrayList<>();

    // Search history
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<SearchHistory> searches = new ArrayList<>();

    // Reports
    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private List<Report> reportsCreated = new ArrayList<>();

    @OneToMany(mappedBy = "reported", fetch = FetchType.LAZY)
    private List<Report> reportsReceived = new ArrayList<>();

    // User management logs
    @OneToMany(mappedBy = "admin", fetch = FetchType.LAZY)
    private List<UserManagementLog> adminActions = new ArrayList<>();

    @OneToMany(mappedBy = "targetUser", fetch = FetchType.LAZY)
    private List<UserManagementLog> targetedActions = new ArrayList<>();

    // Conversations
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<Conversation> conversationsAsTenant = new ArrayList<>();

    @OneToMany(mappedBy = "landlord", fetch = FetchType.LAZY)
    private List<Conversation> conversationsAsLandlord = new ArrayList<>();

    // Messages
    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    private List<Message> sentMessages = new ArrayList<>();

    // Notifications
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();
}
