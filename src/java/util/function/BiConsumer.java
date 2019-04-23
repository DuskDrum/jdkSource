/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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


/**
 * 和Consumer很类似，有输入没输出
 * 只是BiConsumer是两个输入，其他的都和Consumer类似
 *
 */
@FunctionalInterface
public interface BiConsumer<T, U> {

    /**
     * 两个输入，没有输出
     * @param t
     * @param u
     */
    void accept(T t, U u);

    /**
     * after是一个T的父类BiConsumer.
     *
     * 直接在 此类调用accept之后，调用after的accept方法(方法入参是BiConsumer，和BiFunction不同)
     */
    default BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }
}
