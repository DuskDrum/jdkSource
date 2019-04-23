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

/**
 * Predicate断言型函数式接口
 * T：代表输入参数。
 * 接受一个输入参数，返回一个布尔值结果。
 */
@FunctionalInterface
public interface Predicate<T> {

    /**
     * 入参为T，出参为boolean
     */
    boolean test(T t);

    /**
     * 逻辑与，other和自己都为true才返回true
     */
    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    /**
     * 取反
     */
    default Predicate<T> negate() {
        return (t) -> !test(t);
    }

    /**
     * 逻辑或，other和本身有一个为true就行
     */
    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    /**
     * 判断本身和targetRef是否相等
     */
    static <T> Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }
}
