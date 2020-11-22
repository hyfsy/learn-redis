package com.hyf.redis;

import com.hyf.redis.pojo.Person;
import com.hyf.redis.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringbootRedisApplicationTests {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 测试存入汉字
     */
    @Test
    void contextLoads() {
        Person zs = new Person("zhangsan", 18, true);
        redisUtil.set("zs", zs);
        System.out.println(redisUtil.get("zs"));

        Person ls = new Person("李四", 20, false);
        redisUtil.set("ls", ls);
        System.out.println(redisUtil.get("ls"));

        Person ww = new Person("王五", 22, true);
        redisUtil.set("王五", ww);
        System.out.println(redisUtil.get("王五"));
    }

}
