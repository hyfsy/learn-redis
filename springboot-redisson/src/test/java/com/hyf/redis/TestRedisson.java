package com.hyf.redis;

import org.junit.jupiter.api.Test;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 任何分布式锁场景都可以参考 ConcurrentHashMap
 * <p>
 * 分布式锁相关
 * <p>
 * Redis -> AP (不保证数据一致性) （不确定）
 * ZooKeeper -> CP ZAB协议：写入子节点，当超过一般节点写成功后，才算成功，并且当主节点挂了，会选择最新数据的从节点变为主节点
 *
 * @author baB_hyf
 * @date 2020/11/21
 */
@SpringBootTest
public class TestRedisson {

    @Autowired
    private RedissonClient                client;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 普通锁只是在JVM级别的
     * <p>
     * 分布式锁简单实现
     */
    @Test
    public void testStart() {
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
        }
        else {
            System.out.println("从缓存中读取");
        }


    }

    /**
     * 不使用分布式锁的复杂实现
     */
    @Test
    public void commonLock() {
        String lockKey = "product_001"; // 分布式锁key（可考虑分段锁提高性能，将库存分布在不同的key上：001_1、001_2）
        String clientId = UUID.randomUUID().toString(); // 保证只释放当前客户端请求对应的锁

        try {
            // 原子操作，保证同一时间，只有一个请求操作redis中对应的 lockKey
            // 设置超时时间，防止业务系统挂掉，锁未释放
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, clientId, 30, TimeUnit.MINUTES);
            if (!result) {
                System.out.println("获取锁失败！");
            }

            int stock = Integer.parseInt((String) redisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                redisTemplate.opsForValue().set("stock", realStock);
                System.out.println("扣减成功，库存剩余：" + realStock);
            }
            else {
                System.out.println("扣减失败，库存不足");
            }
        }
        // 不管业务是否处理完毕，都要释放锁（此处未执行，锁也会过期）
        finally {
            // 校验是当前客户端加的锁，防止释放其他客户端的锁（发生情况：业务没处理完，锁过期）
            if (clientId.equals(redisTemplate.opsForValue().get(lockKey))) {

                // FIXME 此处存在还问题，如果进入if时，锁刚好过期（考虑使用看门狗/原子操作）
                //  看门狗：开启一个监听，定时（expire/3）重置过期时间

                redisTemplate.delete(lockKey);
            }
        }

    }

    /**
     * 使用分布式锁的简单实现-仅在单机redis中有效
     */
    @Test
    public void redissonLock() {
        String lockKey = "product_001";

        /**
         * 加锁实现：{@link org.redisson.RedissonLock#tryAcquireAsync(long, long, TimeUnit, long)}
         * 看门狗实现：{@link org.redisson.RedissonLock#renewExpirationAsync(long)}
         * 释放锁实现：{@link org.redisson.RedissonLock#unlockInnerAsync(long)}
         */
        RLock redissonLock = client.getLock(lockKey);
        try {
            redissonLock.lock(); // 未获取到锁的线程会持续等待，默认30秒超时等待

            int stock = Integer.parseInt((String) redisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                redisTemplate.opsForValue().set("stock", realStock);
                System.out.println("扣减成功，库存剩余：" + realStock);
            }
            else {
                System.out.println("扣减失败，库存不足");
            }
        } finally {
            redissonLock.unlock();
        }
    }

    /**
     * 主从架构问题：主节点设置分布式锁时，复制到子节点时，如果子节点并没有都复制成功，则客户端可能访问到没有分布式锁的子节点
     * <p>
     * 分布式锁-红锁（RedLock）
     * 当一半以上的节点加锁成功才算加锁成功
     */
    @Test
    public void redLock() {
        String lockKey = "product_001";

        RLock lock = client.getLock(lockKey);
        RLock lock1 = client.getLock(lockKey);
        RLock lock2 = client.getLock(lockKey);

        // 根据多个 RLock 构建一个 RedissonRedLock
        RedissonRedLock redissonRedLock = new RedissonRedLock(lock, lock1, lock2);
        try {
            // waitTime:  尝试获取锁的最大等待时间,超过这个值,则认为获取锁失败
            // leaseTime: 锁的持有时间，超过这个时间锁会自动失效(值应设置为大于业务处理的时间,确保在锁有效期内业务能处理完)
            redissonRedLock.tryLock(10, 30, TimeUnit.SECONDS);

            // business

        } catch (InterruptedException e) {
            System.out.println("lock fail!");
        } finally {
            redissonRedLock.unlock();
        }

    }

    /**
     * 缓存双写一致性问题
     * <p>
     * t1 写库->10，删除缓存
     * t2 查缓存空，查库->10
     * t3 写库->6，删除缓存
     * t2 写缓存->10
     * <p>
     * 不推荐方案：
     * 1、双删：t3第一次删除后，延迟一段时间，再删除一次（降低产生几率，写性能低(每次写要延迟)）
     * 2、内存队列：将数据库操作放到一个队列中（解决，但性能低）
     * <p>
     * 推荐
     * 1、使用分布式锁解决，优化使用分布式读写锁（读多写少场景）
     * 2、canal解决（需要加额外的中间件）
     */
    @Test
    public void doubleWrite() {
        readLock();
        writeLock();
    }

    public void readLock() {
        String lockKey = "product_001";
        RReadWriteLock readWriteLock = client.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();

        try {
            readLock.lock();
            System.out.println("读取缓存");
        } finally {
            readLock.unlock();
        }
    }

    public void writeLock() {
        String lockKey = "product_001";
        RReadWriteLock readWriteLock = client.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();

        try {
            writeLock.lock();
            System.out.println("写入缓存");
        } finally {
            writeLock.unlock();
        }
    }
}
