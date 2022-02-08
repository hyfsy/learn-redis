package com.hyf.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConfig {

    public static final String HOST = "192.168.190.188";
    public static final int    PORT = 6379;

    private static final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), HOST, PORT);

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
}
