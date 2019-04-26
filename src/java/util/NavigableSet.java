/*
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

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea and Josh Bloch with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util;

/**
 * NavigableSet扩展了 SortedSet，为给定搜索目标最接近匹配项。
 * 方法 lower、floor、ceiling 和 higher 分别返回小于、小于等于、大于等于、大于给定元素的元素，如果不存在这样的元素，则返回 null。
 *
 * NavigableSet的接口不鼓励插入null值
 * @param <E>
 */
public interface NavigableSet<E> extends SortedSet<E> {

    /**
     * 返回此集合中 小于给定元素的 最大元素，
     * 如果没有这样的元素，则返回{@code null}。
     */
    E lower(E e);

    /**
     * 返回此set中小于或等于给定元素的最大元素，
     * 如果没有这样的元素，则返回{@code null}。
     *
     */
    E floor(E e);

    /**
     * 返回此set中大于或等于给定元素的最小元素，
     * 如果没有这样的元素，则返回{@code null}。
     */
    E ceiling(E e);

    /**
     * 返回此集合中 大于 给定元素的最小元素，
     * 如果没有这样的元素，则返回{@code null}。
     */
    E higher(E e);

    /**
     * 检索并删除【第一个/最低】元素
     * 如果此集合为空，则返回{@code null}。
     */
    E pollFirst();

    /**
     * 检索并删除【最后一个/最高】元素
     * 如果此集合为空，则返回{@code null}
     */
    E pollLast();

    /**
     * 返回此集合的一个迭代器,升序
     */
    Iterator<E> iterator();

    /**
     * 返回此set中包含元素的逆序视图。
     * 遵循fast-fail
     * 返回的集合排序等同于{@link Collections#reverseOrder(Comparator)}
     * 重复调用两次本方法得到的是顺序视图
     */
    NavigableSet<E> descendingSet();

    /**
     * 以降序返回此集合中元素的迭代器。等同于{@code descendingSet().iterator()}
     */
    Iterator<E> descendingIterator();

    /**
     * 返回此set范围从fromElement到toElement的部分视图。
     * fromInclusive和toInclusive为true，则包含fromElement和toElement
     * 如果fromElement和toElement相等(且boolean都是false)，则返回的集合为空。
     * 返回的集合在其范围之外插入元素时抛出{@code IllegalArgumentException}
     */
    NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                           E toElement, boolean toInclusive);

    /**
     * 详见{@link NavigableSet#subSet(Object, boolean, Object, boolean)}
     */
    NavigableSet<E> headSet(E toElement, boolean inclusive);

    /**
     * 详见{@link NavigableSet#subSet(Object, boolean, Object, boolean)}
     */
    NavigableSet<E> tailSet(E fromElement, boolean inclusive);

    /**
     * 重载方法
     */
    SortedSet<E> subSet(E fromElement, E toElement);

    /**
     * 重载方法
     */
    SortedSet<E> headSet(E toElement);

    /**
     * 重载方法
     */
    SortedSet<E> tailSet(E fromElement);
}
