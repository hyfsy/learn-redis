package com.hyf.redis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * 测试事务乐观锁
 *
 * @author baB_hyf
 * @date 2020/11/08
 */
public class TestTx {

    public static final String PASSWORD = "11111";
    public static final int   modifyValue = 50;
    private static      int   a           = 100;
    private static      int   b           = 0;
    private             Jedis jedis       = null;

    @Before
    public void before() {
        jedis = RedisPoolConfig.getJedis();
        jedis.auth(PASSWORD);
        jedis.set("a", String.valueOf(a));
        jedis.set("b", String.valueOf(b));
    }

    @Test
    public void testTx() {
        System.out.println("开始执行事务");

        int i = 3;

        // 自旋
        while (i-- > 0 && !execute()) {
            if (i == 1) {
                TestTx.a = modifyValue;
            }
            System.out.println("自旋");
        }

        System.out.println("提交成功");
    }

    public boolean execute() {
        jedis.watch("a");

        int modify = modifyValue; // = jedis.get("a"); // 模拟修改

        // 开启事务后，不能调用jedis的方法，也不能调用watch方法，会报错
        // jedis.set("a", "50");

        Transaction transaction = jedis.multi();
        transaction.decrBy("a", 20);
        transaction.incrBy("b", 20);

        if (modify != TestTx.a) {
            transaction.discard(); // 取消事务
            jedis.unwatch(); // 取消观察
            System.out.println("watch a has been modified");
            return false;
        }
        else {
            transaction.exec();
            System.out.println("tx commit success");
            System.out.println("a: " + jedis.get("a"));
            System.out.println("b: " + jedis.get("b"));
            return true;
        }
    }

    @Test
    public void testExtra() {
        jedis.watch("a");
        Transaction transaction = jedis.multi();
        transaction.decrBy("a", 20);
        transaction.incrBy("b", 20);
        List<Object> resultList = transaction.exec();
        if (resultList == null || resultList.isEmpty()) {
            System.out.println("失败");
            return;
        }
        System.out.println("成功");

    }
}
