package com.kanezi.springsocial2cloud.security.data;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_authorities")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class UserAuthorityEntity {
    @Id
    @GeneratedValue
    Long id;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "username", foreignKey = @ForeignKey(name = "user_authority_user_fk"))
    @ToString.Exclude
    UserEntity user;

    @ManyToOne
    @JoinColumn(name = "authority_id", foreignKey = @ForeignKey(name = "user_authority_authority_fk"))
    @ToString.Exclude
    AuthorityEntity authority;


    @PrePersist
    void assignCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    public UserAuthorityEntity(UserEntity user, AuthorityEntity authority) {
        this.user = user;
        this.authority = authority;
    }
}
