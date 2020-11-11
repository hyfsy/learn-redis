package com.hyf.redis;

import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 测试集群部署
 *
 * @author baB_hyf
 * @date 2020/11/08
 */
public class TestCluster {

    public static final String HOST     = "192.168.190.188";
    public static final int    PORT     = 6369;
    public static final String PASSWORD = "11111";

    @Test
    public void testCluster() {

        // 配置一个节点即可，其他插槽的key会自动重定向
        Set<HostAndPort> nodes = new HashSet<>();
        // 注意，redis合并集群时，必须也指定ip为该ip，不能为 127.0.0.1 ！！！
        nodes.add(new HostAndPort(HOST, PORT));


        // 集群配置了密码
        JedisCluster cluster = new JedisCluster(
                nodes,                              // nodes                所有节点
                5000,                               // connectionTimeout    连接超时时间
                5000,                               // socketTimeout        socket超时时间
                3,                                  // maxAttempts          尝试重连次数
                PASSWORD,                           // password             密码
                RedisPoolConfig.getPoolConfig()     // pool                 连接池
        );

        cluster.set("a", "a value");
        String a = cluster.get("a");
        System.out.println(a);

        try {
            cluster.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
