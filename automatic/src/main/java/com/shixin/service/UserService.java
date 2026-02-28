package com.shixin.service;
import com.shixin.entity.User;
public interface UserService {
    User addorUpdateUser(User user);

    User findByUsername(String username);

    User findById(Long id);

    User JWTFindUserForRedisOrMysql(Long user_id);
}
