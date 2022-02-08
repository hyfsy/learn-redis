package com.hyf.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonConfig {

    private static final RedissonClient client;

    static {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + RedisConfig.HOST + ":" + RedisConfig.PORT);
        client = Redisson.create(config);
    }

    public static RedissonClient getClient() {
        return client;
    }
}
