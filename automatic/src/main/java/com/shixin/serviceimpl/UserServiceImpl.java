package com.shixin.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.shixin.entity.User;
import com.shixin.repository.UserRepository;
import com.shixin.service.RedisService;
import com.shixin.service.UserService;

@Service
public class UserServiceImpl implements UserService {
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
    每次鉴权都需要从Redis或MySQL中查询用户信息，确保数据的实时性和准确性。虽然这可能会增加一些查询开销，但可以保证用户信息的最新状态，避免因缓存过期导致的权限问题。
    1.先从redis中查询用户信息，如果存在则直接返回；
    如果不存在，则从MySQL数据库中查询用户信息，
    并将查询结果存入redis缓存中，以便后续快速访问。
    */
    @Autowired
    private RedisService redisService;
    @Override
    public User JWTFindUserForRedisOrMysql(Long user_id) {
        User userInfo = redisService.getUserinfoByUser_id(user_id);
        if (userInfo != null) {
            System.out.println("从Redis中获取用户数据，用户ID: " + user_id);
            // 如果Redis中存在用户信息，则直接返回
            return userInfo;
        } else {
            // 如果Redis中不存在用户信息，则从MySQL数据库中查询，并将结果存入Redis缓存
            System.out.println("从MySQL数据库中获取用户数据，用户ID: " + user_id);
            User user = userRepository.findById(user_id).orElse(null);
            if (user != null) {
                System.out.println("将用户数据存入Redis缓存，用户ID: " + user);
                // 将用户对象转换为JSON字符串存储到Redis中
                User user_1=new User();
                user_1.setId(user.getId());
                user_1.setUsername(user.getUsername());
                user_1.setEmail(user.getEmail());
                user_1.setPhone(user.getPhone());
                redisService.saveToRedis("user:" + user_id, JSON.toJSONString(user_1), 600); // 设置过期时间为1小时
                
            }
            return user;
        }
    }
}
