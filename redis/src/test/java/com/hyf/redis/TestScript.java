package com.hyf.redis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * 测试 lua脚本
 *
 * @author baB_hyf
 * @date 2020/11/08
 */
public class TestScript {

    public static final String HOST     = "192.168.190.188";
    public static final int    PORT     = 6369;
    public static final String PASSWORD = "11111";
    private             Jedis  jedis    = null;

    @Before
    public void before() {
        jedis = new Jedis(HOST, PORT);
        jedis.auth(PASSWORD);
    }

    @Test
    public void testScript() {

        // 加载lua脚本，lua脚本能保证多个redis操作的原子性
        String sha = jedis.scriptLoad(getScript()); // hset hashtable domain 1
        System.out.println(sha);

        int scriptParamNumber = 2;

        // 解析脚本并执行
        Object result = jedis.evalsha(sha, scriptParamNumber, "hashtable", "domain");

        System.out.println(result);
    }

    public String getScript() {
        // xxx.lua
        return "if redis.call('hexists', KEYS[1], KEYS[2]) ~= 0 then\n" +
                "\tlocal value = redis.call('hget', KEYS[1], KEYS[2]);\n" +
                "\tlocal newValue = tostring(value) .. \"8\";\n" +
                "\tredis.call('hset', KEYS[1], KEYS[2], newValue);\n" +
                "\treturn newValue;\n" +
                "else\n" +
                "\treturn nil\n" +
                "end";
    }

    /*

    redis + lua 解决高并发下抢红包的问题

    -- 函数：尝试获得红包，如果成功，则返回json字符串，如果不成功，则返回空
    -- 参数：红包队列名， 已消费的队列名，去重的Map名，用户ID
    -- 返回值：nil 或者 json字符串，包含用户ID：userId，红包ID：id，红包金额：money

    -- 如果用户已抢过红包，则返回nil
    if rediscall('hexists', KEYS[3], KEYS[4]) ~= 0 then
     return nil
    else
     -- 先取出一个小红包
     local hongBao = rediscall('rpop', KEYS[1]);
     if hongBao then
      local x = cjsondecode(hongBao);
      -- 加入用户ID信息
      x['userId'] = KEYS[4];
      local re = cjsonencode(x);
      -- 把用户ID放到去重的set里
      rediscall('hset', KEYS[3], KEYS[4], KEYS[4]);
      -- 把红包放到已消费队列里
      rediscall('lpush', KEYS[2], re);
      return re;
     end
    end
    return nil
     */
    public String getRedPacketScript() {

        // -- 函数：尝试获得红包，如果成功，则返回json字符串，如果不成功，则返回空
        // -- 参数：红包队列名， 已消费的队列名，去重的Map名，用户ID
        // -- 返回值：nil 或者 json字符串，包含用户ID：userId，红包ID：id，红包金额：money

        return "if rediscall('hexists', KEYS[3], KEYS[4]) ~= 0 then \n" +
                " return nil \n" +
                "else \n" +
                " -- 先取出一个小红包 \n" +
                " local hongBao = rediscall('rpop', KEYS[1]); \n" +
                " if hongBao then \n" +
                "  local x = cjsondecode(hongBao); \n" +
                "  -- 加入用户ID信息 \n" +
                "  x['userId'] = KEYS[4]; \n" +
                "  local re = cjsonencode(x); \n" +
                "  -- 把用户ID放到去重的set里 \n" +
                "  rediscall('hset', KEYS[3], KEYS[4], KEYS[4]); \n" +
                "  -- 把红包放到已消费队列里 \n" +
                "  rediscall('lpush', KEYS[2], re); \n" +
                "  return re; \n" +
                " end \n" +
                "end \n" +
                "return nil";
    }

}
