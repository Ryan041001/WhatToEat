package com.zjgsu.whattoeat.repository;

import com.zjgsu.whattoeat.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByOpenid(String openid);
}
