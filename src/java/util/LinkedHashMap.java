/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * HashMap是无序的。
 * LinkedHashMap是有序的(分为查询顺序和访问顺序，默认是插入顺序)。
 * LinkedHashMap的数据结构是数组+链表。比HashMap多两个属性，before和after,用来关联链表节点之间的顺序。
 * (next只能关联一个桶内的顺序，不同桶之间next为null)
 * LinkedHashMap继承了HashMap，所以他的数据结构也是链表+红黑树，(增加了一条双向链表)
 *
 * 其他的map实现想要转化为LinkedHashMap直接转化：
 *      Map copy = new LinkedHashMap.md(m);
 *
 * 也提供一个构造函数来创建连接的哈希映射{@link #LinkedHashMap(int, float, boolean)}(boolean为true->访问顺序排序；false->插入顺序排序)
 *
 * LinkedHashMap非常适合构建LRU缓存(原生注释如是说)
 *
 * 可以重写{@link #removeEldestEntry（Map.Entry）}方法，以便将新映射添加到Map时自动删除过时映射。
 *
 * LinkedHashMap可以为null.
 *
 * LinkedHashMap由于维护链表，性能略低于HashMap。但是在迭代时，不论capacity是什么样，都和size成正比。(不管初始容量大还是小)
 *
 * 线程不安全，可以使用{@link Collections＃synchronizedMap(new LinkedHashMap.md<>())}
 *
 * 添加或删除，会影响迭代顺序。修改不会影响
 * 按访问顺序排序的LinkedHashMap，get也会影响迭代顺序
 *
 * 迭代遵循fast-fail机制，迭代时修改(除了remove方法)，会报ConcurrentModificationException(不保证一定起作用)
 *
 *
 *
 */
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{

    /**
     * 桶继承HashMap的Node。(hashMap的TreeNode继承此Entry。Entry继承HashMap的Node)。
     * 且增加了before和after来维护双向链表。
     */
    static class Entry<K,V> extends Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    private static final long serialVersionUID = 3801124242820219131L;

    /**
     * 链表头结点，定义一个head，方便遍历时快速找到链表头节点
     */
    transient Entry<K,V> head;

    /**
     * 链表尾节点
     */
    transient Entry<K,V> tail;

    /**
     * 排序方式
     * true->访问顺序排序；
     * false->插入顺序排序;
     */
    final boolean accessOrder;

    /**
     * 把p设为tail，并和head关联
     */
    private void linkNodeLast(Entry<K,V> p) {
        Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }

    // 使用dst替换src.这里只操作了before和after，因为不用考虑不同桶之间的逻辑(next需要考虑)
    private void transferLinks(Entry<K,V> src,
                               Entry<K,V> dst) {
        Entry<K,V> b = dst.before = src.before;
        Entry<K,V> a = dst.after = src.after;
        if (b == null)
            head = dst;
        else
            b.after = dst;
        if (a == null)
            tail = dst;
        else
            a.before = dst;
    }

    // overrides of HashMap hook methods

    void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }

    /**
     * 生成Node节点，并放到tail上。(在LinkedHashMap所有节点还是链表时使用)。
     *
     * 入参Node是HashMap的属性
     */
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        Entry<K,V> p =
            new Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    /**
     * 修改入参p的next属性。并替换p
     */
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        Entry<K,V> q = (Entry<K,V>)p;
        Entry<K,V> t =
            new Entry<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    /**
     * 生成红黑树节点，并放到tail上
     */
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }

    /**
     * 修改入参p的next属性。并替换p
     */
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        Entry<K,V> q = (Entry<K,V>)p;
        TreeNode<K,V> t = new TreeNode<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    /**
     * 删除节点e之后，把e的before和after相关联
     */
    void afterNodeRemoval(Node<K,V> e) {
        Entry<K,V> p =
            (Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }

    /**
     * 在插入节点后，判断是否要删除掉最老的节点(head)
     * removeEldestEntry方法可以重写
     */
    void afterNodeInsertion(boolean evict) {
        Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    /**
     * 当访问顺序属性{@link LinkedHashMap#accessOrder}为true时起作用
     * 将e节点挪到尾部tail
     */
    void afterNodeAccess(Node<K,V> e) {
        Entry<K,V> last;
        // 访问顺序排序，且不是tail节点
        if (accessOrder && (last = tail) != e) {
            Entry<K,V> p =
                (Entry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    /**
     * 写入Entry键值对到ObjectOutputStream中
     */
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        for (Entry<K,V> e = head; e != null; e = e.after) {
            s.writeObject(e.key);
            s.writeObject(e.value);
        }
    }


    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }


    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    public LinkedHashMap(int initialCapacity,
                         float loadFactor,
                         boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }


    /**
     * 遍历查询value是否存在
     */
    public boolean containsValue(Object value) {
        for (Entry<K,V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }

    /**
     * 调用HashMap的getNode查值,查空返回null。
     * 如果[按照访问顺序排序]，调用afterNodeAccess
     */
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

    /**
     * 调用HashMap的getNode查值,查空返回defaultValue。
     * (查询不为空)如果[按照访问顺序排序]，调用afterNodeAccess
     */
    public V getOrDefault(Object key, V defaultValue) {
       Node<K,V> e;
       if ((e = getNode(hash(key), key)) == null)
           return defaultValue;
       if (accessOrder)
           afterNodeAccess(e);
       return e.value;
   }

    /**
     * 删除所有键值对
     */
    public void clear() {
        super.clear();
        head = tail = null;
    }

    /**
     * 可以重写此方法，当put/putAll新值的时候返回true，则删除最老的键值对。(head)
     *
     * 如果Map表示缓存，则很有用
     *
     */
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }

    /**
     * 返回键的Set集合
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new LinkedKeySet();
            keySet = ks;
        }
        return ks;
    }

    final class LinkedKeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final Iterator<K> iterator() {
            return new LinkedKeyIterator();
        }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator()  {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED |
                                            Spliterator.DISTINCT);
        }
        public final void forEach(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.key);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * 遍历遵循fast-fail
     * 该集合支持元素删除，Iterator.remove,Collection.remove,removeAll,retainAll,clear
     * 但是不支持add，addAll
     *
     * 并行性比HashMap差得多
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new LinkedValues();
            values = vs;
        }
        return vs;
    }

    final class LinkedValues extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final Iterator<V> iterator() {
            return new LinkedValueIterator();
        }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED);
        }
        public final void forEach(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.value);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * 遍历遵循fast-fail（除非调用remove操作，或者seValue）
     * 该集合支持元素删除，Iterator.remove,Collection.remove,removeAll,retainAll,clear
     * 但是不支持add，addAll
     *
     * 并行性比HashMap差得多
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new LinkedEntrySet()) : es;
    }

    final class LinkedEntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new LinkedEntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED |
                                            Spliterator.DISTINCT);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * 这是lambda表达式所用的。
     * forEach用来遍历。
     *
     * 通过判断modCount和mc的大小，来触发fast-fail机制
     */
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null)
            throw new NullPointerException();
        int mc = modCount;
        for (Entry<K,V> e = head; e != null; e = e.after)
            action.accept(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null)
            throw new NullPointerException();
        int mc = modCount;
        for (Entry<K,V> e = head; e != null; e = e.after)
            e.value = function.apply(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }

    // 迭代器
    abstract class LinkedHashIterator {
        Entry<K,V> next;
        Entry<K,V> current;
        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextNode() {
            Entry<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            current = e;
            next = e.after;
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class LinkedKeyIterator extends LinkedHashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().getKey(); }
    }

    final class LinkedValueIterator extends LinkedHashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class LinkedEntryIterator extends LinkedHashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }


}
