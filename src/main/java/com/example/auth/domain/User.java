package com.example.auth.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class User {

    private Long id;
    private String email;
    private String name;
    private String profileImage;
    private String provider;
    private String providerId;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

    public enum Role {
        USER, ADMIN
    }

    public User(String email, String name, String profileImage, String provider, String providerId) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.provider = provider;
        this.providerId = providerId;
        this.role = Role.USER;
    }
}
