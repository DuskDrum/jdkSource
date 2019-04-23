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
 * Consumer消费型函数式接口
 *
 * T：代表输入参数。
 * Consumer：代表接受一个输入餐胡并且无返回的操作
 *
 * 在使用lambda时，也可以直接在{}中写逻辑，比如复制两个list的值：
 *
 * List<SourceBean> sourceList = ...;
 * List<TargetBean> targetList = new ArrayList<>();
 * sourceList.stream().forEach(source->{
 *     TargetBean target = new TargetBean();
 *     BeanUtils.copyProperties(source, target);
 *     targetList.add(target);
 * });
 *
 */
@FunctionalInterface
public interface Consumer<T> {

    /**
     * 只有一个输入T，没有返回
     *
     */
    void accept(T t);

    /**
     * after是一个T的父类Consumer.
     *
     * 直接在 此类调用accept之后，调用after的accept方法
     *
     */
    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
