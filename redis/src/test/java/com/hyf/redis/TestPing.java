package com.hyf.redis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * 测试连接
 *
 * @author baB_hyf
 * @date 2020/11/07
 */
public class TestPing {

    public static final String HOST     = "192.168.190.188";
    public static final int    PORT     = 6379;
    public static final String PASSWORD = "11111";
    private             Jedis  jedis;

    @Before
    public void before() {
        jedis = new Jedis(HOST, PORT);
        // jedis.auth(PASSWORD);
    }

    @Test
    public void testPing() {
        String ping = jedis.ping();
        jedis.set("test:ping", "hello world");
        System.out.println(ping);
    }
}
