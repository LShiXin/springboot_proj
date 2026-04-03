package com.shixin.serviceimpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.shixin.entity.User;
import com.shixin.repository.UserRepository;
import com.shixin.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @Autowired
    private UserRepository userRepository;

    @Override
    public User addorUpdateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /*
     * 每次鉴权都需要从Redis或MySQL中查询用户信息，确保数据的实时性和准确性。虽然这可能会增加一些查询开销，但可以保证用户信息的最新状态，
     */

    @Override
    public User JWTFindUserForRedisOrMysql(Long user_id) {

        User user = userRepository.findById(user_id).orElse(null);
        if (user != null) {
            return user;
        } else {
            return null;
        }
    }
}
