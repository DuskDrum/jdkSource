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
 * 和Function很类似，有输入有输出
 * 只是BiFunction是两个输入，一个输出，其他的都和Function类似
 *
 */
@FunctionalInterface
public interface BiFunction<T, U, R> {

    /**
     * 两个入参，一个出参
     */
    R apply(T t, U u);

    /**
     * after是一个入参是R的父类，结果为V的子类的Function
     * 这个方法就是本类先入参T，U调用BiFunction的apply方法，得到的R作为入参再调用一次Function的apply方法
     */
    default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }

    //这里思考下为什么比 Function 少了compose和identity方法
    // 我觉得是因为BiFunction只是用来拓展Function的，多传一个入参，
    // BiFunction方法的入参和出参的还得是Function(<V>),compose方法做不到，所以就没有这个方法了
}
