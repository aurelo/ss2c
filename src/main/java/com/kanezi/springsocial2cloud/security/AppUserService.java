package com.kanezi.springsocial2cloud.security;

import com.kanezi.springsocial2cloud.security.data.AuthorityEntity;
import com.kanezi.springsocial2cloud.security.data.AuthorityEntityRepository;
import com.kanezi.springsocial2cloud.security.data.UserEntity;
import com.kanezi.springsocial2cloud.security.data.UserEntityRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Data
@RequiredArgsConstructor
public class AppUserService implements UserDetailsManager {

    private final PasswordEncoder passwordEncoder;

    private final UserEntityRepository userEntityRepository;
    private final AuthorityEntityRepository authorityEntityRepository;

    private final Executor executor;

    //    Map<String, AppUser> users = new HashMap<>();
    private final DefaultOAuth2UserService oauth2Delegate = new DefaultOAuth2UserService();
    private final OidcUserService oidcDelegate = new OidcUserService();


    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        return users.get(username);
        return userEntityRepository
                .findById(username)
                .map(userEntity -> AppUser
                        .builder()
                        .username(userEntity.getUsername())
                        .password(userEntity.getPassword())
                        .imageUrl(userEntity.getImageUrl())
                        .name(userEntity.getName())
                        .provider(userEntity.getProvider())
                        .authorities(userEntity.getUserAuthorities().stream().map(ua -> new SimpleGrantedAuthority(ua.getAuthority().getName())).toList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User %s not found!", username)));
    }

//    @PostConstruct
//    void createHardcodedUsers() {
//        var bob = AppUser.builder()
//                .username("bob")
//                .provider(LoginProvider.APP)
//                .password(passwordEncoder.encode("1234"))
//                .authorities(List.of(new SimpleGrantedAuthority("read")))
//                .build();
//
//        var bil = AppUser.builder()
//                .username("bil")
//                .provider(LoginProvider.APP)
//                .password(passwordEncoder.encode("321"))
//                .authorities(List.of(new SimpleGrantedAuthority("read")))
//                .build();
//
//        createUser(bob);
//        createUser(bil);
//
//    }

    /**
     * Adapts oidc login to return AppUser instead of default OidcUser
     *
     * @return service that returns AppUser from request to the Oidc UserInfo Endpoint
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcLoginHandler() {
        return userRequest -> {
            LoginProvider provider = getProvider(userRequest);
            OidcUser oidcUser = oidcDelegate.loadUser(userRequest);
            AppUser appUser = AppUser
                    .builder()
                    .provider(provider)
                    .username(oidcUser.getEmail())
                    .name(oidcUser.getFullName())
                    .email(oidcUser.getEmail())
                    .userId(oidcUser.getName())
                    .imageUrl(oidcUser.getAttribute("picture"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .attributes(oidcUser.getAttributes())
                    .authorities(oidcUser.getAuthorities())
                    .build();
            saveOauth2User(appUser);
            return appUser;
        };
    }

    /**
     * Adapts oauth2 login to return AppUser instead of default OAauth2User
     *
     * @return service that returns AppUser from request to the Oauth2 user info
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2LoginHandler() {
        return userRequest -> {
            LoginProvider provider = getProvider(userRequest);
            OAuth2User oAuth2User = oauth2Delegate.loadUser(userRequest);
            AppUser appUser = AppUser
                    .builder()
                    .provider(provider)
                    .username(oAuth2User.getAttribute("login"))
                    .name(oAuth2User.getAttribute("login"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .userId(oAuth2User.getName())
                    .imageUrl(oAuth2User.getAttribute("avatar_url"))
                    .attributes(oAuth2User.getAttributes())
                    .authorities(oAuth2User.getAuthorities())
                    .build();
            saveOauth2User(appUser);
            return appUser;
        };
    }

    private LoginProvider getProvider(OAuth2UserRequest userRequest) {
        return LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
    }

    private void saveOauth2User(AppUser appUser) {
        CompletableFuture.runAsync(() -> {
            createUser(appUser);
        }, executor);
    }

    @Transactional
    protected void createUser(AppUser appUser) {

        UserEntity userEntity = saveUserIfNotExists(appUser);

        List<AuthorityEntity> authorityEntities = appUser
                .authorities
                .stream()
                .map(ga -> saveAuthorityIfNotExists(ga.getAuthority(), appUser.getProvider()))
                .toList();

        userEntity.mergeAuthorities(authorityEntities);

        userEntityRepository.save(userEntity);

        //users.putIfAbsent(appUser.getUsername(), appUser);

    }

    private AuthorityEntity saveAuthorityIfNotExists(String grantedAuthority, LoginProvider provider) {

        return authorityEntityRepository
                .findByName(grantedAuthority)
                .orElseGet(() -> authorityEntityRepository.save(new AuthorityEntity(grantedAuthority, provider)));
    }

    private UserEntity saveUserIfNotExists(AppUser user) {
        return userEntityRepository.findById(user.getUsername())
                .orElseGet(() -> userEntityRepository.save(
                        new UserEntity(
                                user.getUsername(),
                                user.getPassword(),
                                user.getEmail(),
                                user.getName(),
                                user.getImageUrl(),
                                user.provider)));
    }

    public void createUser(String username, String password) {
        createUser(User
                .builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .authorities(Collections.emptyList())
                .build());
    }

    @Override
    public void createUser(UserDetails user) {

        if (userExists(user.getUsername())) {
            throw new IllegalArgumentException(String.format("User %s already exists!", user.getUsername()));
        }

        createUser(AppUser
                .builder()
                .provider(LoginProvider.APP)
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .build());
    }

    @Override
    public void updateUser(UserDetails user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String username) {
        if (userExists(username)) {
//            users.remove(username);
            userEntityRepository.deleteById(username);
        }
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AppUser currentUser = (AppUser) authentication.getPrincipal();

        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("Old password is not correct!");
        }

        //users.get(currentUser.getUsername()).setPassword(passwordEncoder.encode(newPassword));

        userEntityRepository.findById(currentUser.getUsername()).ifPresent(ue -> ue.setPassword(passwordEncoder.encode(newPassword)));

    }

    @Override
    public boolean userExists(String username) {
        return userEntityRepository.existsById(username);
//        return users.containsKey(username);
    }


    @Transactional
    public void removeAuthority(AppUser appUser, String authority) {
        appUser.removeAuthority(authority);

        AuthorityEntity authorityEntity = authorityEntityRepository.findByName(authority).orElseThrow(() -> new IllegalArgumentException(authority));

        UserEntity userEntity = userEntityRepository.findById(appUser.getUsername()).orElseThrow(() -> new IllegalArgumentException(appUser.getUsername()));

        userEntity.removeAuthority(authorityEntity);

    }
}
