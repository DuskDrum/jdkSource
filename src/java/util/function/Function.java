/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.function;

import java.util.Objects;

/***
 * Function功能型函数式接口
 *
 * 接受一个输入参数，返回一个结果。T就是参数，R就是结果。
 * 比较核心的就是apply方法，apply方法调用方式有两种：lambda和匿名内部类
 *
 * (x) -> x+20;//lambda做法
 *
 * new Function<Integer,Integer>(){
 *    @Override
 *    public Integer apply(Integer t){
 *        return t+20;
 *    }
 * }  // 匿名内部类做法
 *
 */
@FunctionalInterface
public interface Function<T, R> {

    /**
     * 接受输入参数T，对输入T执行所需操作后，返回一个结果R
     */
    R apply(T t);

    /**
     * before是一个入参为V的父类，结果为T子类的Function
     * 这个方法就是根据入参V先apply，再根据前一步得到的T再做一次apply(迭代？)
     */
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    /**
     * 和上一个方法很像
     * after是一个入参是R的父类，结果为V的子类的Function
     * 这个方法就是本类先入参T调用apply方法，得到的R作为入参再调用一次apply
     */
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    /**
     * 一个一直输出 输入的方法
     */
    static <T> Function<T, T> identity() {
        return t -> t;
    }
}
