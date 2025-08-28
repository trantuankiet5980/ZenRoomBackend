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
        if (this.userId == null) this.userId = UUID.randomUUID().toString();
    }

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 15, unique = true)
    private String phoneNumber;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    private Role role;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UserStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Collections
    @OneToMany(mappedBy = "landlord", fetch = FetchType.LAZY)
    @Builder.Default private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @Builder.Default private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    @Builder.Default private List<Report> reportsCreated = new ArrayList<>();

    @OneToMany(mappedBy = "reported", fetch = FetchType.LAZY)
    @Builder.Default private List<Report> reportsReceived = new ArrayList<>();

    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    @Builder.Default private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @Builder.Default private List<Favorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @Builder.Default private List<SearchHistory> searches = new ArrayList<>();

    @OneToMany(mappedBy = "admin", fetch = FetchType.LAZY)
    @Builder.Default private List<UserManagementLog> adminActions = new ArrayList<>();

    @OneToMany(mappedBy = "targetUser", fetch = FetchType.LAZY)
    @Builder.Default private List<UserManagementLog> targetedActions = new ArrayList<>();

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @Builder.Default private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "landlord", fetch = FetchType.LAZY)
    @Builder.Default private List<ReviewReply> reviewReplies = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default private List<DiscountCodeUsage> couponUsages = new ArrayList<>();
}
