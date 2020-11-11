package com.hyf.redis;

import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * 测试基本命令使用
 *
 * @author baB_hyf
 * @date 2020/11/07
 */
public class TestUse {

    public static final String PASSWORD = "11111";

    @Test
    public void testUse() {
        Jedis jedis = RedisPoolConfig.getJedis();
        String auth = jedis.auth(PASSWORD);
        System.out.println(auth);
        String set = jedis.set("test config", "asdf");
        System.out.println(set);

    }
}
