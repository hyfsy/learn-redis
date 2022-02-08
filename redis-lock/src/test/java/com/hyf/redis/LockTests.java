package com.hyf.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LockTests {

    private final String businessKey = "user:1";

    private final long expireTime = TimeUnit.SECONDS.toMillis(30);

    private Jedis jedis;

    public void doBusiness() {
    }

    @Before
    public void redisBefore() {
        jedis = RedisConfig.getJedis();
    }

    @After
    public void redisAfter() {
        jedis.close();
    }

    @Test
    public void testLock1() {

        Long rtn = jedis.setnx(businessKey, "placeholder");
        if (rtn == 1) {
            try {
                doBusiness();
            } finally {
                jedis.del(businessKey);
            }
        }
        // 重试
    }

    @Test
    public void testLock2() {

        Long rtn = jedis.setnx(businessKey, "placeholder");
        if (rtn == 1) {
            jedis.pexpire(businessKey, expireTime);
            try {
                doBusiness();
            } finally {
                jedis.del(businessKey);
            }
        }
    }

    @Test
    public void testLock3() {

        String rtn = jedis.set(businessKey, "placeholder", "NX", "PX", expireTime);
        if ("OK".equals(rtn)) {
            try {
                doBusiness();
            } finally {
                jedis.del(businessKey);
            }
        }
    }

    @Test
    public void testLock4() {

        String id = UUID.randomUUID().toString();

        String rtn = jedis.set(businessKey, id, "NX", "PX", expireTime);
        if ("OK".equals(rtn)) {
            try {
                doBusiness();
            } finally {
                String val = jedis.get(businessKey);
                if (val.equals(id)) {
                    jedis.del(businessKey);
                }
            }
        }
    }

    @Test
    public void testLock5() {

        String id = UUID.randomUUID().toString();

        String rtn = jedis.set(businessKey, id, "NX", "PX", expireTime);
        if ("OK".equals(rtn)) {
            try {
                doBusiness();
            } finally {
                String script =
                        "if redis.call('get',KEYS[1]) == ARGV[1] " +
                                "then " +
                                "    return redis.call('del',KEYS[1]); " +
                                "else " +
                                "    return 0; " +
                                "end";
                jedis.eval(script, 1, businessKey, id);
            }
        }
    }

    @Test
    public void testLock6() {

        class WatchDog {

            private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

            private final String key;

            public WatchDog(String key) {
                this.key = key;
            }

            public void watch() {
                executorService.scheduleAtFixedRate(() -> {

                    String script =
                            "if (redis.call('get', KEYS[1]) ~= nil) " +
                                    "then " +
                                    "    redis.call('pexpire', KEYS[1], ARGV[1]); " +
                                    "    return 1; " +
                                    "else " +
                                    "    return 0; " +
                                    "end;";

                    jedis.eval(script, 1, key, String.valueOf(expireTime));
                }, 10, 10, TimeUnit.SECONDS);
            }

            public void stopWatch() {
                executorService.shutdown();
            }
        }

        String id = UUID.randomUUID().toString();

        String rtn = jedis.set(businessKey, id, "NX", "PX", expireTime);
        if ("OK".equals(rtn)) {

            try {
                WatchDog watchDog = new WatchDog(businessKey);
                watchDog.watch();

                try {
                    doBusiness();
                } finally {
                    watchDog.stopWatch();
                }
            } finally {
                String script =
                        "if redis.call('get',KEYS[1]) == ARGV[1] " +
                                "then " +
                                "    return redis.call('del',KEYS[1]); " +
                                "else " +
                                "    return 0; " +
                                "end";
                jedis.eval(script, 1, businessKey, id);
            }
        }
    }

    @Test
    public void testLock7() {

        // 重试、公平/非公平、可重入/不可重入、阻塞/自旋、可中断、避免惊群效应...

        // https://github.com/redisson/redisson/wiki/%E7%9B%AE%E5%BD%95

        RedissonClient client = RedissonConfig.getClient();

        RLock lock = client.getLock(businessKey);
        lock.lock();
        try {
            doBusiness();
        } finally {
            lock.unlock();
        }

        client.shutdown();
    }
}
