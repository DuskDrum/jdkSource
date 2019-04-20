/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

import java.io.Serializable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

/**
 * 抽象类，不能被实例化(用new来实现)。
 * Enum<E extends Enum<E>>可以暂时理解为Enum里面的内容都是E extends Enum类型。
 * 这里的E我们就理解为枚举，extends表示上界，相当于把一个子类或者自己当成参数，传入到自身。
 * 主要是限定形态参数实例化的对象，要求只能是Enum，这样才能对compareTo之类的方法所传入的参数进行形态检查
 */
public abstract class Enum<E extends Enum<E>>
        implements Comparable<E>, Serializable {
    /**
     * 枚举名,final
     */
    private final String name;

    public final String name() {
        return name;
    }

    /**
     * 枚举的序数,final
     */
    private final int ordinal;

    public final int ordinal() {
        return ordinal;
    }

    /**
     * 唯一的构造函数定义为protected,其他类无法调用这个构造函数
     * Enum类是抽象类，但是不准调用构造函数(不准继承)
     *
     * 想要调用这个类，需要使用Java的语法糖，enum定义
     */
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    /**
     * 重写toString,返回枚举名
     */
    public String toString() {
        return name;
    }

    /**
     * 跟object类的equals方法一样，不懂为啥重写
     */
    public final boolean equals(Object other) {
        return this==other;
    }

    /**
     * 不懂为啥重写
     */
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * 枚举类，不允许克隆方法，直接抛异常
     */
    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * 类的泛型E指的是，E extends Enum<E>，即E的子类或本身
     *
     * 其实比较的就是两个枚举的序列大小。
     *
     * 但是什么样的枚举序列会大呢，暂时只有构造函数给序列赋值
     */
    public final int compareTo(E o) {
        Enum<?> other = (Enum<?>)o;
        Enum<E> self = this;
        if (self.getClass() != other.getClass() &&
                self.getDeclaringClass() != other.getDeclaringClass())
            throw new ClassCastException();
        return self.ordinal - other.ordinal;
    }

    /***
     * getSuperclass：直接返回父类，擦除泛型
     * 如果父类是枚举类，就返回本身的class,否则是父类class
     *
     * Enum不是不能继承么= =，包内继承？
     */
    @SuppressWarnings("unchecked")
    public final Class<E> getDeclaringClass() {
        Class<?> clazz = getClass();
        Class<?> zuper = clazz.getSuperclass();
        return (zuper == Enum.class) ? (Class<E>)clazz : (Class<E>)zuper;
    }

    /**
     * 在序列化的时候Java仅仅是将枚举对象的name属性输出到结果中，
     * 反序列化的时候则是通过Enum的valueOf方法来根据名字查找枚举对象。
     *
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumType,
                                                String name) {
        // 调用enumType这个Class对象的enumConstantDirectory()
        // 方法返回的map中获取名字为name的枚举对象
        T result = enumType.enumConstantDirectory().get(name);
        if (result != null)
            return result;
        if (name == null)
            throw new NullPointerException("Name is null");
        throw new IllegalArgumentException(
                "No enum constant " + enumType.getCanonicalName() + "." + name);
    }

    /**
     * 枚举类不支持finalize方法
     */
    protected final void finalize() { }

    /**
     * 防止默认反序列化，直接抛异常
     */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        throw new InvalidObjectException("can't deserialize enum");
    }

    /**
     * 在某些特殊情况下，反序列化会用readObjectNoData代替readObject
     * 直接抛异常
     */
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("can't deserialize enum");
    }
}
