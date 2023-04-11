package com.kanezi.springsocial2cloud.security.data;

import com.kanezi.springsocial2cloud.security.LoginProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log4j2
public class UserEntity {
    @Id
    String username;
    String password;
    String email;
    String name;
    String imageUrl;

    @Enumerated(value = EnumType.STRING)
    LoginProvider provider;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    List<UserAuthorityEntity> userAuthorities = new ArrayList<>();

    public UserEntity(String username, String password, String email, String name, String imageUrl, LoginProvider provider) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.name = name;
        this.imageUrl = imageUrl;
        this.provider = provider;
    }

    public void addAuthority(AuthorityEntity authority) {
        if (userAuthorities.stream().anyMatch(uae -> uae.user.equals(this) && uae.authority.equals(authority))) {
            return;
        }

        UserAuthorityEntity userAuthorityEntity = new UserAuthorityEntity(this, authority);
        this.userAuthorities.add(userAuthorityEntity);
        authority.getAssignedTo().add(userAuthorityEntity);
    }

    public void removeAuthority(AuthorityEntity authority) {
        log.info("remove authority: {}", authority);
        Iterator<UserAuthorityEntity> iterator = this.userAuthorities.iterator();

        while (iterator.hasNext()) {
            UserAuthorityEntity next = iterator.next();

//            if (next.user.equals(this) && next.authority.equals(authority)) {
            if (next.authority.equals(authority)) {

                iterator.remove();
                authority.getAssignedTo().remove(next);

                next.setAuthority(null);
                next.setUser(null);

            }
        }

//        for (Iterator<UserAuthorityEntity> iterator = userAuthorities.iterator(); iterator.hasNext(); ) {
//            UserAuthorityEntity uae = iterator.next();
//
//            if (uae.user.equals(this) && uae.authority.equals(authority)) {
//                uae.getAuthority().getAssignedTo().remove(uae);
//
//                uae.setUser(null);
//                uae.setAuthority(null);
//
//                iterator.remove();
//
//            }
//        }

    }


    public void mergeAuthorities(List<AuthorityEntity> authorityEntities) {
        var toRemove = this.userAuthorities
                .stream()
                .filter(uae -> !authorityEntities.contains(uae.getAuthority()))
                .toList();

        toRemove.forEach(uae -> this.removeAuthority(uae.getAuthority()));

        authorityEntities.forEach(this::addAuthority);

    }
}
