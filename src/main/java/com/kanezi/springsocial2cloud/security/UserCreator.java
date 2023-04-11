package com.kanezi.springsocial2cloud.security;

import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Value
public class UserCreator {

    AppUserService appUserService;
    PasswordEncoder passwordEncoder;

    @PostConstruct
    void createHardcodedUsers() {
        var bob = AppUser.builder()
                .username("bob")
                .provider(LoginProvider.APP)
                .password(passwordEncoder.encode("1234"))
                .authorities(List.of(new SimpleGrantedAuthority("read")))
                .build();

        var bil = AppUser.builder()
                .username("bil")
                .provider(LoginProvider.APP)
                .password(passwordEncoder.encode("321"))
                .authorities(List.of(new SimpleGrantedAuthority("read")))
                .build();

        appUserService.createUser(bob);
        appUserService.createUser(bil);

    }
}
