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
 * 继承SortedMap接口，搜索给定目标返回最接近的匹配项
 * 方法lowerEntry、floorEntry、ceilingEntry和higherEntry分别返回小于、小于等于、大于等于、大于给定键的键关联的 Map.Entry对象，如果不存在这样的键，则返回null
 * 类似地，方法lowerKey，floorKey，ceilingKey和higherKey返回小于、小于等于、大于等于、大于给定键的键
 *
 * firstEntry，pollFirstEntry，lastEntry和pollLastEntry，它返回和或删除最小和最大的映射（如果存在），否则返回{@code null}
 */
public interface NavigableMap<K,V> extends SortedMap<K,V> {

    /**
     * 返回与严格小于给定键相关联的键-值映射，如果没有这样的键，则返回{@code null}。
     */
    Entry<K,V> lowerEntry(K key);

    /**
     * 返回严格小于给定键的最大键，如果没有这样的键，则返回{@code null}。
     */
    K lowerKey(K key);

    /**
     * 返回与小于或等于给定键关联的键-值映射，如果没有这样的键，则返回{@code null}。
     */
    Entry<K,V> floorEntry(K key);

    /**
     * 返回小于或等于给定键的最大键，如果没有这样的键，则返回{@code null}。
     */
    K floorKey(K key);

    /**
     * 返回与大于或等于给定键关联的键-值映射，如果没有此键，则返回{@code null}。
     */
    Entry<K,V> ceilingEntry(K key);

    /**
     * 返回与大于或等于给定键的最大值，如果没有此键，则返回{@code null}。
     */
    K ceilingKey(K key);

    /**
     * 返回与严格大于给定键关联的键-值映射，如果没有这样的键，则返回{@code null}。
     */
    Entry<K,V> higherEntry(K key);

    /**
     * 返回与严格大于给定键的最小值，如果没有这样的键，则返回{@code null}。
     */
    K higherKey(K key);

    /**
     * 返回与此映射中的【最小键】关联的键-值映射，如果映射为空，则返回{@code null}。
     */
    Entry<K,V> firstEntry();

    /**
     * 返回与此映射中的【最大键】关联的键-值映射，如果映射为空，则返回{@code null}。
     */
    Entry<K,V> lastEntry();

    /**
     * 删除并返回与此映射中的【最小键】关联的键-值映射，如果映射为空，则返回{@code null}。
     */
    Entry<K,V> pollFirstEntry();

    /**
     * 删除并返回与此映射中的【最大键】关联的键-值映射，如果映射为空，则返回{@code null}。
     */
    Entry<K,V> pollLastEntry();

    /**
     * 用来生成，降序排列的地图，Todo:降序之后上面的那些方法是什么样的规则
     * 迭代时注意fast-fail
     * 返回的Map排序相当于{@link Collections#reverseOrder(Comparator) }
     *
     * 连续调用两次本方法得到的是原来的Map
     *
     */
    NavigableMap<K,V> descendingMap();

    /**
     * 返回此映射中所有键的{@link NavigableSet}视图。 set的迭代器按升序返回键
     * 遵循fast-fail
     * 支持删除但不支持新增
     * Iterator.remove,Set.remove,removeAll,removeAll,clear
     * add,addAll
     */
    NavigableSet<K> navigableKeySet();

    /**
     * 返回此映射中包含的键的反向{@link NavigableSet}视图。 set的迭代器按降序返回。
     * 遵循fast-fail
     * 支持删除但不支持新增
     * Iterator.remove,Set.remove,removeAll,removeAll,clear
     * add,addAll
     */
    NavigableSet<K> descendingKeySet();

    /**
     * 返回key的范围从fromKey到toKey对应的键值对。
     * fromInclusive 代表是否包含fromKey
     * toInclusive 代表是否包含toKey
     * 如果fromKey和toKey相等，则返回的Map为空，除非fromInclusive和toInclusive都为true
     *
     * 在返回的Map中插入一个新键值对，会抛出异常{@code IllegalArgumentException}
     */
    NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                             K toKey, boolean toInclusive);

    /**
     * 返回键【小于】(如果inclusive为true时，也可以等于)toKey的键值对。
     *
     * 在返回的Map中插入一个新键值对，会抛出异常{@code IllegalArgumentException}
     */
    NavigableMap<K,V> headMap(K toKey, boolean inclusive);

    /**
     * 返回键【大于】(如果inclusive为true时，也可以等于)fromKey的键值对。
     *
     * 在返回的Map中插入一个新键值对，会抛出异常{@code IllegalArgumentException}
     */
    NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);

    /**
     * 重载方法，和{@link NavigableMap#subMap(Object, boolean, Object, boolean)}
     * 本方法返回的是SortedMap
     */
    SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * 重载方法，和{@link NavigableMap#headMap(Object, boolean)}
     * 本方法返回的是SortedMap
     */
    SortedMap<K,V> headMap(K toKey);

    /**
     * 重载方法，和{@link NavigableMap#tailMap(Object, boolean)}
     * 本方法返回的是SortedMap
     */
    SortedMap<K,V> tailMap(K fromKey);
}
