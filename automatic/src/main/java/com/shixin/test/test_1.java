package com.shixin.test;

import com.shixin.serviceimpl.BaseUrlsManagerServiceImpl;
import com.shixin.entity.ConfigItem;
import java.util.List;

/**
 * Simple test class for BaseUrlsManagerServiceImpl.
 * 
 * Note: This test creates the service directly without Spring dependency injection,
 * so the RedisService autowired field will not be initialized. For proper testing,
 * use the unit test in src/test/java/com/shixin/serviceimpl/BaseUrlsManagerServiceImplTest.java
 * which uses Mockito to mock dependencies.
 */
public class test_1 {
    public static void main(String[] args) {
        BaseUrlsManagerServiceImpl base = new BaseUrlsManagerServiceImpl();
        // This will likely throw a NullPointerException because RedisService is not initialized
        // For proper testing, use the unit test with mocked dependencies
        List<ConfigItem> list = base.loadPotional_Urls();
        System.out.print(list);
    }
}
