package com.example.auth.mapper;

import com.example.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<User> findByProviderAndProviderId(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

    void save(User user);

    void updateLastLoginAt(@Param("id") Long id);
}
