package com.hyf.redis.pojo;

import java.io.Serializable;

/**
 * 需要实现序列化接口
 * <p>
 * redis直接传递对象会报序列化异常，redis默认使用jdk序列化
 *
 * @author baB_hyf
 * @date 2020/11/07
 */
public class Person implements Serializable {
    private String  name;
    private Integer age;
    private boolean sex;

    public Person(String name, Integer age, boolean sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public boolean isSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", sex=" + sex +
                '}';
    }
}
