package com.shixin.serviceimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixin.entity.ConfigItem;
import com.shixin.service.BaseUrlsManagerService;
import com.shixin.service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.alibaba.fastjson.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BaseUrlsManagerServiceImpl implements BaseUrlsManagerService {
    private static final Logger log = LoggerFactory.getLogger(BaseUrlsManagerServiceImpl.class);

    @Autowired
    private RedisService redisService;

    private final String key_optionalUrls = "optionalUrls";

    @Override
    // 从本地的json文件中，获取数据，同步到redis中，如果redis不存在就获取后放到redis中，避免重复读取
    public List<ConfigItem> loadPotional_Urls() {
        ObjectMapper mapper = new ObjectMapper();
        String redis_data=redisService.getFromRedis(key_optionalUrls);
        // log.debug("从redis中获取的数据为: {}", redis_data);
        if(redis_data == null){
            // 如果redis里面是空，就获取json的文件并加载到redis中
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("Optional_field.json")) {
                if (is == null) {
                    throw new IOException("文件未找到在类路径中: Optional_field.json");
                }
                List<ConfigItem> result=mapper.readValue(is, new TypeReference<List<ConfigItem>>() {
                });
                redisService.saveToRedis(key_optionalUrls, JSON.toJSONString(result), 8640);
                log.info("从json文件中读取数据");
                return result;
            } catch (IOException e) {
                log.error("加载可选URL配置文件失败", e);
                return Collections.emptyList();
            }
        }else{
             log.info("从redis中读取数据");
            return JSON.parseArray(redis_data, ConfigItem.class);
        }
    }
    
    @Override
    public List<ConfigItem> reloadPotional_UrlsFromJson() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("Optional_field.json")) {
            if (is == null) {
                throw new IOException("文件未找到在类路径中: Optional_field.json");
            }
            List<ConfigItem> result = mapper.readValue(is, new TypeReference<List<ConfigItem>>() {});
            log.info("强制从JSON文件重新加载数据，加载了 {} 条记录\n数据为：{}", result.size(), result);
            redisService.saveToRedis(key_optionalUrls, JSON.toJSONString(result), 8640);
            log.info("强制从JSON文件重新加载数据到Redis，加载了 {} 条记录", result.size());
            return result;
        } catch (IOException e) {
            log.error("强制重新加载可选URL配置文件失败", e);
            return Collections.emptyList();
        }
    }
}
