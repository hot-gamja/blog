package com.example.auth.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.auth.domain.User;
import com.example.auth.mapper.UserMapper;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    public CustomOAuth2UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId   = (String) attributes.get("sub");
        String email        = (String) attributes.get("email");
        String name         = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture");

        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_provider_id"), "Provider id claim is required");
        }
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

        Map<String, Object> displayAttributes = new HashMap<>(attributes);
        displayAttributes.put("displayName", displayName);

        Set<GrantedAuthority> authorities = new HashSet<>(oAuth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new DefaultOAuth2User(
                authorities,
                displayAttributes,
                "displayName"
        );
    }
}
