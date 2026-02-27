package com.shixin.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shixin.entity.User;
public interface UserRepository extends JpaRepository<User, Long> {
    // 根据用户名查询用户
    User findByUsername(String username);
}
