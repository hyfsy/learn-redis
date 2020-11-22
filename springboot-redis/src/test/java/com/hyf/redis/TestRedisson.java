package com.hyf.redis;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author baB_hyf
 * @date 2020/11/21
 */
@SpringBootTest
public class TestRedisson {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 分布式锁简单实现
     */
    @Test
    public void testA() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.190.188:6369").setPassword("11111");
        RedissonClient client = Redisson.create(config);

        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();

        String key = "jvm_process_id:thread_num";
        String packetId = "1";

        String value = hashOperations.get(key, packetId);

        if (StringUtils.isEmpty(value)) {
            RLock lock = client.getLock("key");

            try {
                lock.lock(10, TimeUnit.MINUTES);

                value = hashOperations.get(key, packetId);
                if (StringUtils.isEmpty(value)) {
                    hashOperations.put(key, packetId, "1");

                    System.out.println("从数据库中读取");
                }

            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("从缓存中读取");
        }



    }
}
