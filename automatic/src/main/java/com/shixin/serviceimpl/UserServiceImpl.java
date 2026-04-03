package com.shixin.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.shixin.entity.User;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.repository.UserRepository;
import com.shixin.service.CacheService;
import com.shixin.service.RedisService;
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
     * 避免因缓存过期导致的权限问题。
     * 1.先从redis中查询用户完整信息，如果存在则提取用户基本信息返回；
     * 如果不存在，则从MySQL数据库中查询用户信息，
     * 并将查询结果存入redis缓存中，以便后续快速访问。
     */
    @Autowired
    private RedisService redisService;

    @Autowired
    private CacheService cacheService;

    @Override
    public User JWTFindUserForRedisOrMysql(Long user_id) {
        // 先从新的JSON存储中获取用户完整信息
        UserAllInfoDTO userAllInfo = redisService.getUserAllInfo(user_id);
        if (userAllInfo != null && userAllInfo.getBase() != null) {
            log.info("从Redis JSON存储中获取用户数据，用户ID: {}", user_id);
            // 从完整信息中提取用户基本信息
            UserAllInfoDTO.UserBaseInfo baseInfo = userAllInfo.getBase();
            User user = new User();
            user.setId(baseInfo.getId());
            user.setUsername(baseInfo.getUsername());
            user.setEmail(baseInfo.getEmail());
            user.setPhone(baseInfo.getPhone());
            user.setEnabled(baseInfo.getEnabled());
            return user;
        } else {
            log.info("从MySQL数据库中获取用户数据，用户ID: {}", user_id);
            User user = userRepository.findById(user_id).orElse(null);
            if (user != null) {
                log.info("将用户数据存入Redis缓存，用户ID: {}", user);
                // 同时缓存用户的完整信息
                cacheService.cacheUserAllInfoToRedis(user_id);
                return user;
            } else {
                return null;
            }
        }
    }
}
