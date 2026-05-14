package com.example.auth.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.stereotype.Service;

import com.example.auth.domain.User;
import com.example.auth.mapper.UserMapper;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserMapper userMapper;

    public CustomOidcUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider    = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerId  = oidcUser.getSubject();
        String email       = oidcUser.getEmail();
        String name        = oidcUser.getFullName();
        String profileImage = (String) oidcUser.getAttributes().get("picture");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"), "Email claim is required");
        }
        String displayName = (name != null && !name.isBlank()) ? name : email;

        Optional<User> found = userMapper.findByProviderAndProviderId(provider, providerId);
        User user;
        if (found.isEmpty()) {
            user = new User(email, displayName, profileImage, provider, providerId);
            userMapper.save(user);
        } else {
            user = found.get();
            userMapper.updateLastLoginAt(user.getId());
        }

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        authorities.add(new OidcUserAuthority("ROLE_" + user.getRole().name(), oidcUser.getIdToken(), oidcUser.getUserInfo()));

        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }
}
