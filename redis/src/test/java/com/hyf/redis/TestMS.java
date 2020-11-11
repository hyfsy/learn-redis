package com.hyf.redis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * 测试主从复制
 *
 * @author baB_hyf
 * @date 2020/11/08
 */
public class TestMS {

    public static final String HOST     = "192.168.190.188";
    public static final String PASSWORD = "11111";
    public static final int    PORT_M   = 6379;
    public static final int    PORT_S   = 6380;
    private             Jedis  master   = null;
    private             Jedis  slave    = null;

    @Before
    public void before() {
        master = new Jedis(HOST, PORT_M); // 主机设置密码的情况下，从机配置文件中需指定masterauth
        slave = new Jedis(HOST, PORT_S);
        master.auth(PASSWORD);
        slave.auth(PASSWORD);
    }

    @Test
    public void testMS() {
        String slaveof = slave.slaveof(HOST, PORT_M);
        System.out.println(slaveof);

        master.set("info", "aaa");
        String info = slave.get("info");
        System.out.println(info); // 可能拿不到值，内存操作速度太快

    }
}
