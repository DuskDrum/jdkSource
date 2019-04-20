package java.util;

import sun.misc.SharedSecrets;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * HashMap基于哈希表的 Map接口实现。与HashTable主要区别为不支持同步和允许null作为key和value。
 * 他不保证数据的有序性，特别是，它不保证数据顺序不随时间变化而变化
 *
 * Hash的hash方法在桶之间正确分散元素时候，Map的 get 和 put 拥有提供恒定时间性能。
 * 对集合视图的迭代需要的时间与 HashMap 的“capacity” 和其大小（键值映射的数量）成比例。
 * 因此，如果迭代性能很重要，不要将初始容量设置得太高（或负载因子设置太低）。
 *
 * HashMap 的实例有两个影响其性能的参数：initial capacity初始容量 和load factor加载因子。
 * capacity 是哈希表中的桶数，initial capacity只是创建哈希表时的容量。
 * 加载因子 衡量哈希表在其容量自动增加之前可以获得多长。
 * 当哈希表中的条目数超过加载因子和当前容量的乘积时，哈希表rehashed（即重建内部数据结构），进行扩容。
 *
 * 作为一般规则，默认加载因子（0.75）在时间和空间成本之间提供了良好的权衡。
 * 较高的加载因子会减少空间开销，但会增加查找成本。
 * 在设置其初始容量时，应考虑映射中的预期条目数及其加载因子，以便最小化重新散列操作的数量。
 * 至于为什么会是0.75，其实不一定非得是0.75，可以看看这篇博客https://www.jianshu.com/p/64f6de3ffcc1
 *
 * HashMap是线程不安全的，可以使用Map m = Collections.synchronizedMap(new HashMap<>())来实现线程安全，
 * 或者使用HashTable和ConcurrentHashMap
 *
 * java的所有集合类(Collection)返回的迭代器都使用"fail-fast"错误机制：
 *     如果在创建迭代器之后对映射进行结构修改,除了使用迭代器的remove，都会抛出{@link ConcurrentModificationException}异常。
 *     ConcurrentModificationException：
 *          是通过modCount来实现的，modCount代表的就是修改次数
 *
 * 但是只能用来检测错误，因为JDK并不保证fail-fast机制一定会发生。
 *
 * HashMap采用数组+链表+红黑树实现，当链表长度超过阈值8时，将链表转换为红黑树 *
 *
 * 当链表转化为树，或者树转化为链表的时候，会保证桶中元素的顺序(Node.next)
 *
 */
public class HashMap<K, V> extends AbstractMap<K, V>
        implements Map<K, V>, Cloneable, Serializable {

    private static final long serialVersionUID = 362498820763181265L;


    /**
     * 默认的初始容量（容量为HashMap中数组的数目）是16，且实际容量必须是2的整数次幂。
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    /**
     * 最大容量，2的30次方，容量大于这个值是会使用他代替(这里和ArrayList不一样，ArrayList超过容量会手动抛异常OOM)
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认装填因子0.75
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 链表最大长度，当桶中节点数8时，链表转成红黑树；
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 当桶中节点数小于6时，红黑树转成链表；
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 在链表转成红黑树之前会判断，只有当容量大于这个值时才会转成红黑树，否则扩容
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 存储链表中的值，每个值是一个node
     */
    static class Node<K, V> implements Map.Entry<K, V> {
        // 对应桶位置的hashCode
        final int hash;
        // final:就是HashMap的key
        final K key;
        V value;
        // 指向下个节点的引用
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final String toString() {
            return key + "=" + value;
        }
        // 位异或运算,两个数转为二进制，相同发为0，不同为1
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    /**
     * 下面的这段代码叫做“扰动函数”（简化了1.7的扰动函数）
     * 通过异或运算（XORs) 将 key.hashCode() 的值从高位扩散到低位。
     *
     * key.hashCode()函数调用的是key键值类型自带的哈希函数，返回int型散列值。
     * 理论上散列值是一个int型，如果直接拿散列值作为下标访问HashMap主数组的话，
     * 考虑到2进制32位带符号的int表值范围从-2147483648到2147483648。
     * 前后加起来大概40亿的映射空间。只要哈希函数映射得比较均匀松散，一般应用是很难出现碰撞的。
     * 但问题是一个40亿长度的数组，内存是放不下的。所以需要“高位扩散到低位”
     *
     * 整个方法：使用hashCode() + 1次位运算 + 1次异或运算（2次扰动）
     * 1. 取hashCode值： h = key.hashCode()
     * 2. 高位参与低位的运算：h ^ (h >>> 16)
     *
     *  此外：
     *  a. 当key = null时，hash值 = 0，所以HashMap的key 可为null
     *      对比HashTable，HashTable对key直接hashCode（），若key为null时，会抛出异常，所以HashTable的key不可为null
     *  b. 当key ≠ null时，则通过先计算出 key的 hashCode()（记为h），然后 对哈希码进行 扰动处理： 按位 异或（^） 哈希码自身右移16位后的二进制
     *
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 如果参数x实现了Comparable接口，返回参数x的类名，否则返回null
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            Type t;
            // 使用ParameterizedType获取泛式类型
            ParameterizedType p;
            if ((c = x.getClass()) == String.class)
                return c;
            // getGenericInterfaces:以Type的形式返回本类直接实现的接口.这样就包含了泛型参数信息
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            // getActualTypeArguments()[0]方法得到该反省
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c)
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * 如果x的类型为kc，则返回k.compareTo(x)，否则返回0
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable) k).compareTo(x));
    }

    /**
     * 这个方法在构造函数上使用，用于找到大于等于initialCapacity的最小的2的幂
     * 详情可以参考：https://blog.csdn.net/fan2012huan/article/details/51097331
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- Fields -------------- */

    /**
     * 哈希桶数组，分配的时候，table的长度总是2的幂，就是桶数组
     */
    transient Node<K, V>[] table;

    /**
     * HashMap将数据转换成set的另一种存储形式，这个变量主要用于迭代功能
     */
    transient Set<Map.Entry<K, V>> entrySet;

    /**
     * 实际存储的数量，HashMap的size()方法，实际返回的就是这个值，isEmpty()也是判断该值是否为0
     */
    transient int size;

    /**
     * modCount用于记录HashMap的修改次数,
     * 在HashMap的put(),get(),remove(),Interator()等方法中,都使用了该属性，保证fast-fail机制
     */
    transient int modCount;

    /**
     * HashMap的扩容阈值，在HashMap中存储的Node键值对超过这个数量时，自动扩容容量为原来的二倍
     *
     * 这里和ArrayList不一样，ArrayList扩容为1.5倍
     *
     * @serial
     */
    int threshold;

    /**
     * HashMap的负加载因子，可计算出当前table长度下的扩容阈值：threshold = loadFactor * table.length
     *
     * @serial
     */
    final float loadFactor;


    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }


    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }


    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    /**
     * 往hashMap中塞其他的Map
     * @param m
     * @param evict
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        // size()函数，值m的实际元素个数
        int s = m.size();
        if (s > 0) {
            // 判断table是否已经初始化
            if (table == null) {
                // s为m的实际元素个数，这里的需求是判断m的长度（即s）是否超过2的30次方
                // 如果s没超过最大容量，但超过最大容量的扩容阈值，就需要立即扩容
                // 这边直接用了一个判断
                float ft = ((float) s / loadFactor) + 1.0F;
                int t = ((ft < (float) MAXIMUM_CAPACITY) ?
                        (int) ft : MAXIMUM_CAPACITY);
                // 计算得到的t大于阈值，则初始化阈值
                if (t > threshold)
                    threshold = tableSizeFor(t);
            // table已初始化，并且m元素个数大于阈值，进行扩容处理
            } else if (s > threshold)
                //扩容处理
                resize();
            // 将m中的所有元素添加至HashMap中
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }


    public int size() {
        return size;
    }


    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 返回指定的key映射的value，如果value为null，则返回null
     * get可以分为三个步骤：
     * 1.先通过hash(Object key)方法计算key的哈希值hash。
     * 2.根据key的hash值找到对应的桶(也就是数组)
     * 3.根据key的equals方法遍历得到hashmap中具体的值，这里注意，equals方法是可以重写的
     *
     * @see #put(Object, Object)
     */
    public V get(Object key) {
        Node<K, V> e;
        //根据key的hash值查询node节点，再根据key查询具体的value
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * 根据key的哈希值和key获取对应的节点
     * getNode可分为以下几个步骤：
     * 1.如果哈希表为空，或key对应的桶为空，返回null
     * 2.如果桶中的第一个节点就和指定参数hash和key匹配上了，返回这个节点。
     * 3.如果桶中的第一个节点没有匹配上，而且有后续节点
     * 3.1如果当前的桶采用红黑树，则调用红黑树的get方法去获取节点
     * 3.2如果当前的桶不采用红黑树，即桶中节点结构为链式结构，遍历链表，直到key匹配
     * 4.找到节点返回null，否则返回null。
     *
     */
    final Node<K, V> getNode(int hash, Object key) {
        Node<K, V>[] tab;
        Node<K, V> first, e;
        int n;
        K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null) {
            // 在遍历之前，确认了第一个桶有值，先判断一下，减少遍历的情况
            // 使用了key的equals方法
            if (first.hash == hash &&
                    ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            // first(第一桶).next指向了下一个桶
            if ((e = first.next) != null) {
                //根据 第一桶 判断是否是红黑树
                // 疑惑是，这个判断表示了e不为空，为什么还要用first.getTreeNode(答：不管用e还是first，进入之后都是用的根节点)
                if (first instanceof TreeNode)
                    return ((TreeNode<K, V>) first).getTreeNode(hash, key);
                //如果当前的桶不采用红黑树，即桶中节点结构为链式结构
                do {
                    //遍历链表，直到key匹配
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        //如果哈希表为空，或者没有找到节点，返回null
        return null;
    }

    /**
     * 如果map中含有key为指定参数key的键值对，返回true
     *
     */
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * 将指定参数key和指定参数value插入map中，如果key已经存在，那就修改value
     *
     */
    public V put(K key, V value) {
        // 倒数第二个参数false：表示允许旧值替换
        // 最后一个参数true：表示HashMap不处于创建模式
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Map.put和其他相关方法的实现需要的方法
     * putVal方法可以分为下面的几个步骤:
     * 1.如果哈希表为空，调用resize()创建一个哈希表。
     * 2.如果指定参数hash在表中没有对应的桶，即为没有碰撞，直接将键值对插入到哈希表中即可。
     * 3.如果有碰撞，遍历桶，找到key映射的节点
     * 3.1桶中的第一个节点就匹配了，将桶中的第一个节点记录起来。
     * 3.2如果桶中的第一个节点没有匹配，且桶中结构为红黑树，则调用红黑树对应的方法插入键值对。
     * 3.3如果不是红黑树，那么就肯定是链表。遍历链表，如果找到了key映射的节点，就记录这个节点，退出循环。
     *    如果没有找到，在链表尾部插入节点。插入后，如果链的长度大于TREEIFY_THRESHOLD这个临界值，则使用treeifyBin方法把链表转为红黑树。
     * 4.如果找到了key映射的节点，且节点不为null
     * 4.1记录节点的vlaue。
     * 4.2如果参数onlyIfAbsent为false，或者oldValue为null，替换value，否则不替换。
     * 4.3返回记录下来的节点的value。
     * 5.如果没有找到key映射的节点（2、3步中讲了，这种情况会插入到hashMap中），插入节点后size会加1，
     *    这时要检查size是否大于临界值threshold，如果大于会使用resize方法进行扩容。
     *
     * @param hash         指定参数key的哈希值
     * @param key          指定参数key
     * @param value        指定参数value
     * @param onlyIfAbsent 如果为true，即使指定参数key在map中已经存在，也不会替换value
     * @param evict        如果为false，数组table在创建模式中
     * @return 如果value被替换，则返回旧的value，否则返回null。当然，可能key对应的value就是null。
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K, V>[] tab;
        Node<K, V> p;
        int n, i;
        //如果哈希表为空，调用resize()创建一个哈希
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        // tab[(n - 1) & hash]是数组上对应的桶位置，这个桶位置还没有值，直接插入
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            Node<K, V> e;
            K k;
            // 比较桶中第一个元素(数组中的结点)的hash值相等，key相等
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                // 将第一个元素赋值给e，用e来记录
                e = p;
                // 当前桶中无该键值对，且桶是红黑树结构，按照红黑树结构插入
            else if (p instanceof TreeNode)
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
                // 当前桶中无该键值对，且桶是链表结构，按照链表结构插入到尾部
            else {
                for (int binCount = 0; ; ++binCount) {
                    // 遍历到链表尾部
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        // 检查链表长度是否达到阈值，达到将该槽位节点组织形式转为红黑树
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    // 链表节点的<key, value>与put操作<key, value>相同时，不做重复操作，跳出循环
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            // 找到或新建一个key和hashCode与插入元素相等的键值对，进行put操作
            if (e != null) { // existing mapping for key
                // 记录e的value
                V oldValue = e.value;
                /**
                 * onlyIfAbsent为false或旧值为null时，允许替换旧值
                 * 否则无需替换
                 */
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                // 访问后回调
                afterNodeAccess(e);
                // 返回旧值
                return oldValue;
            }
        }
        // 更新结构化修改信息
        ++modCount;
        // 键值对数目超过阈值时，进行rehash
        if (++size > threshold)
            resize();
        // 插入后回调
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * 对table进行初始化或者扩容。
     * 如果table为null，则对table进行初始化
     * 如果对table扩容，因为每次扩容都是翻倍，与原来计算（n-1）&hash的结果相比，节点要么就在原来的位置，要么就被分配到“原位置+旧容量”这个位置
     * resize的步骤总结为:
     * 1.计算扩容后的容量，临界值。
     * 2.将hashMap的临界值修改为扩容后的临界值
     * 3.根据扩容后的容量新建数组，然后将hashMap的table的引用指向新数组。
     * 4.将旧数组的元素复制到table中。
     *
     * @return the table
     */
    public final Node<K, V>[] resize() {
        //新建oldTab数组保存扩容前的数组table
        Node<K, V>[] oldTab = table;
        //获取原来数组的长度
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //原来数组扩容的临界值
        int oldThr = threshold;
        int newCap, newThr = 0;
        //如果扩容前的容量 > 0
        if (oldCap > 0) {
            //如果原来的数组长度大于最大值(2^30)
            if (oldCap >= MAXIMUM_CAPACITY) {
                //扩容临界值提高到正无穷
                threshold = Integer.MAX_VALUE;
                //无法进行扩容，返回原来的数组
                return oldTab;
                //如果现在容量的两倍小于MAXIMUM_CAPACITY且现在的容量大于DEFAULT_INITIAL_CAPACITY
            } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                //临界值变为原来的2倍
                newThr = oldThr << 1;
        } else if (oldThr > 0) //如果旧容量 <= 0，而且旧临界值 > 0
            //数组的新容量设置为老数组扩容的临界值
            newCap = oldThr;
        else { //如果旧容量 <= 0，且旧临界值 <= 0，新容量扩充为默认初始化容量，新临界值为DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY
            newCap = DEFAULT_INITIAL_CAPACITY;//新数组初始容量设置为默认值
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);//计算默认容量下的阈值
        }
        // 计算新的resize上限
        if (newThr == 0) {//在当上面的条件判断中，只有oldThr > 0成立时，newThr == 0
            //ft为临时临界值，下面会确定这个临界值是否合法，如果合法，那就是真正的临界值
            float ft = (float) newCap * loadFactor;
            //当新容量< MAXIMUM_CAPACITY且ft < (float)MAXIMUM_CAPACITY，新的临界值为ft，否则为Integer.MAX_VALUE
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ?
                    (int) ft : Integer.MAX_VALUE);
        }
        //将扩容后hashMap的临界值设置为newThr
        threshold = newThr;
        //创建新的table，初始化容量为newCap
        @SuppressWarnings({"rawtypes", "unchecked"})
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        //修改hashMap的table为新建的newTab
        table = newTab;
        //如果旧table不为空，将旧table中的元素复制到新的table中
        if (oldTab != null) {
            //遍历旧哈希表的每个桶，将旧哈希表中的桶复制到新的哈希表中
            for (int j = 0; j < oldCap; ++j) {
                Node<K, V> e;
                //如果旧桶不为null，使用e记录旧桶
                if ((e = oldTab[j]) != null) {
                    //将旧桶置为null
                    oldTab[j] = null;
                    //如果旧桶中只有一个node
                    if (e.next == null)
                        //将e也就是oldTab[j]放入newTab中e.hash & (newCap - 1)的位置
                        newTab[e.hash & (newCap - 1)] = e;
                        //如果旧桶中的结构为红黑树
                    else if (e instanceof TreeNode)
                        //将树中的node分离
                        ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
                    else {  //如果旧桶中的结构为链表,链表重排，jdk1.8做的一系列优化
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;
                        //遍历整个链表中的节点
                        do {
                            next = e.next;
                            // 原索引
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            } else {// 原索引+oldCap
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        // 原索引放到bucket里
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        // 原索引+oldCap放到bucket里
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    /**
     * 将链表转化为红黑树
     */
    final void treeifyBin(Node<K, V>[] tab, int hash) {
        int n, index;
        Node<K, V> e;
        //如果桶数组table为空，或者桶数组table的长度小于MIN_TREEIFY_CAPACITY，不符合转化为红黑树的条件
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            //扩容
            resize();
            //如果符合转化为红黑树的条件，而且hash对应的桶不为null
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            // 红黑树的头、尾节点
            TreeNode<K, V> hd = null, tl = null;
            //遍历链表
            do {
                //替换链表node为树node，建立双向链表
                TreeNode<K, V> p = replacementTreeNode(e, null);
                // 确定树头节点
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            //遍历链表插入每个节点到红黑树
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }

    /**
     * 将参数map中的所有键值对映射插入到hashMap中，如果有碰撞，则覆盖value。
     *
     * @param m 参数map
     * @throws NullPointerException 如果map为null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    /**
     * 删除hashMap中key映射的node
     * remove方法的实现可以分为三个步骤：
     * 1.通过 hash(Object key)方法计算key的哈希值。
     * 2.通过 removeNode 方法实现功能。
     * 3.返回被删除的node的value。
     *
     */
    public V remove(Object key) {
        Node<K, V> e;
        //根据key来删除node。removeNode方法的具体实现在下面
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : e.value;
    }

    /**
     * Map.remove和相关方法的实现需要的方法
     * removeNode方法的步骤总结为:
     * 1.如果数组table为空或key映射到的桶为空，返回null。
     * 2.如果key映射到的桶上第一个node的就是要删除的node，记录下来。
     * 3.如果桶内不止一个node，且桶内的结构为红黑树，记录key映射到的node。
     * 4.桶内的结构不为红黑树，那么桶内的结构就肯定为链表，遍历链表，找到key映射到的node，记录下来。
     * 5.如果被记录下来的node不为null，删除node，size-1被删除。
     * 6.返回被删除的node。
     *
     * @param hash       key的哈希值
     * @param key        key的哈希值
     * @param value      如果 matchValue 为true，则value也作为确定被删除的node的条件之一，否则忽略
     * @param matchValue 如果为true，则value也作为确定被删除的node的条件之一
     * @param movable    如果为false，删除node时不会删除其他node
     * @return 返回被删除的node，如果没有node被删除，则返回null（针对红黑树的删除方法）
     */
    final Node<K, V> removeNode(int hash, Object key, Object value,
                                boolean matchValue, boolean movable) {
        Node<K, V>[] tab;
        Node<K, V> p;
        int n, index;
        //如果数组table不为空且key映射到的桶不为空
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {
            Node<K, V> node = null, e;
            K k;
            V v;
            //如果桶上第一个node的就是要删除的node
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                //记录桶上第一个node
                node = p;
            else if ((e = p.next) != null) {//如果桶内不止一个node
                //如果桶内的结构为红黑树
                if (p instanceof TreeNode)
                    //记录key映射到的node
                    node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
                else {//如果桶内的结构为链表
                    do {//遍历链表，找到key映射到的node
                        if (e.hash == hash &&
                                ((k = e.key) == key ||
                                        (key != null && key.equals(k)))) {
                            //记录key映射到的node
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            //如果得到的node不为null且(matchValue为false||node.value和参数value匹配)
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {
                //如果桶内的结构为红黑树
                if (node instanceof TreeNode)
                    //使用红黑树的删除方法删除node
                    ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
                else if (node == p)//如果桶的第一个node的就是要删除的node
                    //删除node
                    tab[index] = node.next;
                else//如果桶内的结构为链表，使用链表删除元素的方式删除node
                    p.next = node.next;
                ++modCount;//结构性修改次数+1
                --size;//哈希表大小-1
                afterNodeRemoval(node);
                return node;//返回被删除的node
            }
        }
        return null;//如果数组table为空或key映射到的桶为空，返回null。
    }

    /**
     * 删除map中所有的键值对
     */
    public void clear() {
        Node<K, V>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    /**
     * 如果hashMap中的键值对有一对或多对的value为参数value，返回true
     *
     * @param value 参数value
     * @return 如果hashMap中的键值对有一对或多对的value为参数value，返回true
     */
    public boolean containsValue(Object value) {
        Node<K, V>[] tab;
        V v;
        if ((tab = table) != null && size > 0) {
            //遍历数组table
            for (int i = 0; i < tab.length; ++i) {
                //遍历桶中的node
                for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                    if ((v = e.value) == value ||
                            (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回hashMap中所有key的视图。
     * 改变hashMap会影响到set，反之亦然。
     * 如果当迭代器迭代set时，hashMap被修改(除非是迭代器自己的remove()方法)，迭代器的结果是不确定的。
     * set支持元素的删除，通过Iterator.remove、Set.remove、removeAll、retainAll、clear操作删除hashMap中对应的键值对。
     * 不支持add和addAll方法。
     *
     * 这里的keySet是AbstractMap中的default类型，所以需要放到java.util包里
     *
     * @return 返回hashMap中所有key的set视图
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    /**
     * 内部类KeySet
     *
     * ConcurrentModificationException，fast-fail机制
     */
    final class KeySet extends AbstractSet<K> {
        public final int size() {
            return size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<K> iterator() {
            return new KeyIterator();
        }

        public final boolean contains(Object o) {
            return containsKey(o);
        }

        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }

        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        public final void forEach(Consumer<? super K> action) {
            Node<K, V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * 返回hashMap中所有value的collection视图
     * 改变hashMap会改变collection，反之亦然。
     * 如果当迭代器迭代collection时，hashMap被修改（除非是迭代器自己的remove()方法），迭代器的结果是不确定的。
     * collection支持元素的删除，通过Iterator.remove、Collection.remove、removeAll、retainAll、clear操作删除hashMap中对应的键值对。
     * 不支持add和addAll方法。
     *
     * @return 返回hashMap中所有key的collection视图
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    /**
     * 内部类Values
     *
     *
     * ConcurrentModificationException，fast-fail机制
     */
    final class Values extends AbstractCollection<V> {
        public final int size() {
            return size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<V> iterator() {
            return new ValueIterator();
        }

        public final boolean contains(Object o) {
            return containsValue(o);
        }

        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        public final void forEach(Consumer<? super V> action) {
            Node<K, V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * 返回hashMap中所有键值对的set视图
     * 改变hashMap会影响到set，反之亦然。
     * 如果当迭代器迭代set时，hashMap被修改(除非是迭代器自己的remove()方法)，迭代器的结果是不确定的。
     * set支持元素的删除，通过Iterator.remove、Set.remove、removeAll、retainAll、clear操作删除hashMap中对应的键值对。
     * 不支持add和addAll方法。
     *
     * @return 返回hashMap中所有键值对的set视图
     */
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    /**
     * 内部类EntrySet
     */
    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public final int size() {
            return size;
        }

        public final void clear() {
            HashMap.this.clear();
        }

        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            Node<K, V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }

        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }

        public final Spliterator<Map.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        /**
         * 遍历时 ConcurrentModificationException，fast-fail机制
         */
        public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
            Node<K, V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K, V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // JDK8重写的方法

    /**
     * 通过key映射到对应node，如果没映射到则返回默认值defaultValue
     *
     * @param key
     * @param defaultValue
     * @return key映射到对应的node，如果没映射到则返回默认值defaultValue
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    /**
     * 在hashMap中插入参数key和value组成的键值对，如果key在hashMap中已经存在，不替换value
     *
     * @param key
     * @param value
     * @return 如果key在hashMap中不存在，返回旧value
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    /**
     * 删除hashMap中key为参数key，value为参数value的键值对。如果桶中结构为树，则级联删除
     *
     * @param key
     * @param value
     * @return 删除成功，返回true
     */
    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    /**
     * 使用newValue替换key和oldValue映射到的键值对中的value
     *
     * @param key
     * @param oldValue
     * @param newValue
     * @return 替换成功，返回true
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K, V> e;
        V v;
        if ((e = getNode(hash(key), key)) != null &&
                ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    /**
     * 使用参数value替换key映射到的键值对中的value
     *
     * @param key
     * @param value
     * @return 替换成功，返回true
     */
    @Override
    public V replace(K key, V value) {
        Node<K, V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        } else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K, V> e;
        V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
                (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            } else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else
                removeNode(hash, key, null, false, true);
        } else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K, V>[] tab;
        Node<K, V> first;
        int n, i;
        int binCount = 0;
        TreeNode<K, V> t = null;
        Node<K, V> old = null;
        if (size > threshold || (tab = table) == null ||
                (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
            else {
                Node<K, V> e = first;
                K k;
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            } else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    /**
     * ConcurrentModificationException， fast-fail机制
     */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K, V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * ConcurrentModificationException，fast-fail机制
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K, V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // 克隆和序列化

    /**
     * 浅拷贝。
     * clone方法虽然生成了新的HashMap对象，新的HashMap中的table数组虽然也是新生成的，但是数组中的元素还是引用以前的HashMap中的元素。
     * 这就导致在对HashMap中的元素进行修改的时候，即对数组中元素进行修改，会导致原对象和clone对象都发生改变，但进行新增或删除就不会影响对方，因为这相当于是对数组做出的改变，clone对象新生成了一个数组。
     *
     * @return hashMap的浅拷贝
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K, V> result;
        try {
            result = (HashMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets
    final float loadFactor() {
        return loadFactor;
    }

    final int capacity() {
        return (table != null) ? table.length :
                (threshold > 0) ? threshold :
                        DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * 序列化hashMap到ObjectOutputStream中
     * 将hashMap的总容量capacity、实际容量size、键值对映射写入到ObjectOutputStream中。键值对映射序列化时是无序的。
     *
     * @serialData The <i>capacity</i> of the java.util.HashMapNote (the length of the
     * bucket array) is emitted (int), followed by the
     * <i>size</i> (an int, the number of key-value
     * mappings), followed by the key (Object) and value (Object)
     * for each key-value mapping.  The key-value mappings are
     * emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        //写入总容量
        s.writeInt(buckets);
        //写入实际容量
        s.writeInt(size);
        //写入键值对
        internalWriteEntries(s);
    }

    /**
     * 到ObjectOutputStream中读取hashMap
     * 将hashMap的总容量capacity、实际容量size、键值对映射读取出来
     */
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        // 将hashMap的总容量capacity、实际容量size、键值对映射读取出来
        s.defaultReadObject();
        //重置hashMap
        reinitialize();
        //如果加载因子不合法，抛出异常
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                    loadFactor);
        s.readInt();                //读出桶的数量，忽略
        int mappings = s.readInt(); //读出实际容量size
        //如果读出的实际容量size小于0，抛出异常
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                    mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            //调整hashMap大小
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);            // 加载因子
            float fc = (float) mappings / lf + 1.0f;         //初步得到的总容量，后续还会处理
            //处理初步得到的容量，确认最终的总容量
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                    DEFAULT_INITIAL_CAPACITY :
                    (fc >= MAXIMUM_CAPACITY) ?
                            MAXIMUM_CAPACITY :
                            tableSizeFor((int) fc));
            //计算临界值，得到初步的临界值
            float ft = (float) cap * lf;
            //得到最终的临界值
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                    (int) ft : Integer.MAX_VALUE);

            // Check Map.Entry[].class since it's the nearest public type to
            // what we're actually creating.
            SharedSecrets.getJavaOISAccess().checkArray(s, Map.Entry[].class, cap);
            //新建桶数组table
            @SuppressWarnings({"rawtypes", "unchecked"})
            Node<K, V>[] tab = (Node<K, V>[]) new Node[cap];
            table = tab;

            // 读出key和value，并组成键值对插入hashMap中
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class HashIterator {
        Node<K, V> next;        // next entry to return
        Node<K, V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K, V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {
                } while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        /**
         * ConcurrentModificationException， fast-fail机制
         */
        final Node<K, V> nextNode() {
            Node<K, V>[] t;
            Node<K, V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {
                } while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        /**
         * ConcurrentModificationException，fast-fail机制
         */
        public final void remove() {
            Node<K, V> p = current;
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

    final class KeyIterator extends HashIterator
            implements Iterator<K> {
        public final K next() {
            return nextNode().key;
        }
    }

    final class ValueIterator extends HashIterator
            implements Iterator<V> {
        public final V next() {
            return nextNode().value;
        }
    }

    final class EntryIterator extends HashIterator
            implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() {
            return nextNode();
        }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K, V> {
        final HashMap<K, V> map;
        Node<K, V> current;          //记录当前的节点
        int index;                  //当前节点的下标
        int fence;                  //堆大小
        int est;                    //估计大小
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(HashMap<K, V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashMap<K, V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K, V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<K> {
        KeySpliterator(HashMap<K, V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        /**
         * ConcurrentModificationException，fast-fail机制
         */
        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        /**
         * ConcurrentModificationException，fast-fail机制
         */
        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<V> {
        ValueSpliterator(HashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        /**
         * ConcurrentModificationException，fast-fail机制
         */
        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        /**
         * ConcurrentModificationException，fast-fail机制
         */
        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K, V>
            extends HashMapSpliterator<K, V>
            implements Spliterator<Map.Entry<K, V>> {
        EntrySpliterator(HashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        /***
         * ConcurrentModificationException，fast-fail机制
         */
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K, V> m = map;
            Node<K, V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            } else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K, V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        /**
         * ConcurrentModificationException，fast-fail机制
         */
        public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K, V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K, V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    // 创建一个链表结点
    Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
        return new Node<>(hash, key, value, next);
    }

    // 替换一个链表节点
    Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    // 创建一个红黑树节点
    TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // 替换一个红黑树节点
    TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K, V> p) {
    }

    void afterNodeInsertion(boolean evict) {
    }

    void afterNodeRemoval(Node<K, V> p) {
    }

    // 写入hashMap键值对到ObjectOutputStream中
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K, V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K, V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }



    /**
     * 可以看这篇文章：https://zhuanlan.zhihu.com/p/31805309
     *
     * 二叉查找树：
     * 左子树上所有结点的值均小于或等于它的根结点的值。
     * 右子树上所有结点的值均大于或等于它的根结点的值。
     * 左、右子树也分别为二叉排序树。
     *
     * 红黑树：
     * 节点是红色或黑色。根是黑色。所有叶子都是黑色（叶子是NIL节点）。
     * 每个红色节点必须有两个黑色的子节点。(从每个叶子到根的所有路径上不能有两个连续的红色节点。)
     * 从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点（黑高）。
     *
     * 集成LinkedHashMap.Entry<K,V>,而LinkedHashMap.Entry继承了HashMap.Node<K,V>
     * 所以TreeNode依然保有Node的属性，同时由于添加了prev这个前驱指针使得树结构变为了双向的(有next，也有prev)。
     *
     *
     * 红黑树是基于二叉搜索树扩展而来，对于TreeNode来说，排序的依据是结点的hash值，若相等然后比较key值。
     * 左儿子的hash值小于等于父亲，右儿子的hash值大于父亲。
     *
     * 还涉及到 树的插入和删除 会破坏红黑树的性质，也就涉及到了红黑树的旋转(左旋，右旋)
     * 也会涉及到变色。
     *
     */

    static final class TreeNode<K, V> extends LinkedHashMap.Entry<K, V> {
        TreeNode<K, V> parent;  //节点的父亲
        TreeNode<K, V> left;    //节点的左孩子
        TreeNode<K, V> right;   //节点的右孩子
        TreeNode<K, V> prev;    //节点的前一个节点
        boolean red;            //true表示红节点，false表示黑节点

        /**
         * 这里调用的其实就是HashMap.Node的构造方法
         */
        TreeNode(int hash, K key, V val, Node<K, V> next) {
            super(hash, key, val, next);
        }

        /**
         * 返回根节点，循环遍历检查parent是否为null
         */
        final TreeNode<K, V> root() {
            for (TreeNode<K, V> r = this, p; ; ) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * 确保根节点被放到table数组上面，如果不是的话，就将root设为根节点.
         * 在添加TreeNode和删除TreeNode时调用。因为红黑树涉及到旋转，根节点不是固定的TreeNode，要确保根节点在数组上。
         */
        static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                // root.hash 指红黑树对应桶位置的hashCode，这是为了快速定位红黑树的下标(在数组的下标)
                int index = (n - 1) & root.hash;
                // 取出tab[index]中的第一个节点，现在还不清楚整个红黑树在hashMap中的结构
                // 整个hashMap用的同一颗树？
                // 每个桶上有一颗树？
                // 按照这个方法的逻辑应该是每个桶上有棵树，树的根节点在数组上，其他的树节点，通过parent,left,right,prev属性关联

                // 取出的tab[index]第一个节点，就是根节点
                TreeNode<K, V> first = (TreeNode<K, V>) tab[index];
                if (root != first) {
                    Node<K, V> rn;
                    tab[index] = root;
                    // rp是root的前一个节点
                    TreeNode<K, V> rp = root.prev;
                    // rn是root的后一个节点
                    if ((rn = root.next) != null)
                        // 将rn的前一个节点指向rp
                        ((TreeNode<K, V>) rn).prev = rp;
                    if (rp != null)
                        // rp的后一个节点指向rn
                        rp.next = rn;
                    if (first != null)
                        // 原来的first的前一个节点指向root
                        first.prev = root;
                    // root的后一个节点指向原来的first
                    root.next = first;
                    // root的前一个节点
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
            //TODO:将root从rp和rn中抽出来，把rp,rn关联；将first位置换成root；但是并未涉及到parent和旋转。可能是tab[index]放的并不是树的根节点
        }

        /**
         * 从根节点查找hash为h，key为k的节点。kc值得key的Class
         */
        final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
            TreeNode<K, V> p = this;
            // 通过比较h与节点的hash大小循环查找
            do {
                int ph, dir;
                K pk;
                TreeNode<K, V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl; //p.hash>参数hash，移向左子树
                else if (ph < h)
                    p = pr; //p.hash<参数hash,移向右子树
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p; // p.hash=参数hash,并且equals，直接返回
                else if (pl == null)
                    p = pr; //因为是else if ，走到这里的都是ph = h 的,向非空子树移动
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                        (kc = comparableClassFor(k)) != null) && //判断kc是一个可比较的类
                        (dir = compareComparables(kc, k, pk)) != 0) // 比较k和p.key
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)//这里开始的条件仅当输入k=null的时候才会进入，先检查右子树再检查左子树
                    return q;
                else
                    p = pl;
            } while (p != null); // 直到遍历到p为null
            return null;
        }

        /**
         * 获取树节点，通过根节点查找。(方法比较重要)
         * h - 代表key的hash()
         * k - 代表hash的值
         *
         * 寻找某个结点所在的树中是否有hash和key值符合的结点
         * 不管最初调用getTreeNode方法的是哪个节点，都会从树的根节点开始寻找(二叉搜索树)
         * 复杂度从链表的O(n)下降到O(lgn)
         */
        final TreeNode<K, V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * 插入结点的key值k和父结点的key值pk无法比较出大小时(a,b为null,或者compareTo相等)，用于比较k和pk的hash值大小
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                // System.identityHashCode()指的是默认的哈希值(也就是没被重写过的HashCode()方法返回)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }

        /**
         * 据链表生成树，遍历获取结点，一个个插入红黑树中。
         * 每次插入从根开始根据hash值寻找到叶结点位置进行插入，
         * 插入一个结点后调用一次balanceInsertion(root, x)检查x位置的红黑树性质是否需要修复
         *
         */
        final void treeify(Node<K, V>[] tab) {
            TreeNode<K, V> root = null;
            // 这种带“,”的写法在这个类里，用这个类接就可以。在TreeNode类下面用TreeNode
            for (TreeNode<K, V> x = this, next; x != null; x = next) {
                // 根据链表进行遍历
                next = (TreeNode<K, V>) x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    //根节点一定是黑色的
                    x.red = false;
                    root = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K, V> p = root; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        // p.hash>h则dir=-1，p.hash<h则dir=1
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K, V> xp = p;
                        //要插入的位置已没有子结点，则进行插入，否则沿着要插入的子树位置继续向下遍历
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                //x的hash值小于等于p的hash值时尝试插入到左子树
                                xp.left = x;
                            else
                                //x的hash值大于p的hash值时尝试插入到右子树
                                xp.right = x;
                            // 插入后修复红黑树性质
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            // 确保当前的root是直接落在table数组上
            moveRootToFront(tab, root);
        }

        /**
         * 把树转为链表，由于replacementNode这个方法会生成新的Node，所以产生的新链表不再具有树的信息了，原本的TreeNode被gc了
         */
        final Node<K, V> untreeify(HashMap<K, V> map) {
            //hd是头部，tl是尾部
            Node<K, V> hd = null, tl = null;
            for (Node<K, V> q = this; q != null; q = q.next) {
                Node<K, V> p = map.replacementNode(q, null);
                if (tl == null)
                    //第一个结点产生时头部指向它
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * 插入键值对。
         * 只要有h和k值符合的节点就不做插入(h指hash值，用来定位数组位置;k指key)，这里的key必须是==或者equals才算相等。
         * 否则需要进行插入。
         *
         * 红黑树通过比较hash值大小，查询速度优化，但是插入时需要调整树的平衡
         *
         */
        final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] tab,
                                        int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            // 获取根节点
            TreeNode<K, V> root = (parent != null) ? root() : this;
            for (TreeNode<K, V> p = root; ; ) {
                int dir, ph;
                K pk;
                // p.hash>h时dir=-1，p.hash<h时dir=1。用来循环判断去哪个子节点上查找key
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                // 确定已经存在此key值则直接返回，这里不做修改操作
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    // 比较相等时会执行一次
                    if (!searched) {
                        TreeNode<K, V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.find(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }
                // ？直到p的right/left为null
                TreeNode<K, V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K, V> xpn = xp.next;
                    TreeNode<K, V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K, V>) xpn).prev = x;
                    // balanceInsertion方法用来保证红黑树的性质
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * 删除操作，删除操作需要寻找一个节点，填充被删掉的位置，并且要平衡树
         *
         * 节点无子节点时，删除后不需要做其他调整
         * 节点只有一个儿子时，那个儿子就替代他的位置
         *
         *
         * 如果当前的树结点太少，需要转换为线性链表，通常这个值设定为2-6个结点
         *
         */
        final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            // 获取当前数组下标
            int index = (n - 1) & hash;
            TreeNode<K, V> first = (TreeNode<K, V>) tab[index], root = first, rl;
            // next指后一个节点，prev指前一个节点
            TreeNode<K, V> succ = (TreeNode<K, V>) next, pred = prev;
            if (pred == null)
                // 给tab[index](根节点)和first都指向要删除节点的后一个节点(succ)
                tab[index] = first = succ;
            else
                // 把要删除的前一个节点的next指向要删除节点的后一个节点
                pred.next = succ;
            if (succ != null)
                // 要删除节点的后一个节点不为空时，把要删除节点的前一个节点 设为 后一个节点的前一个节点
                succ.prev = pred;
            // first指的是数组上的根节点，如果只有这一个节点，则直接返回
            if (first == null)
                return;
            if (root.parent != null)
                // 确保root指向根节点
                root = root.root();
            // 根为null 或者 根节点没有儿子，说明树的节点很少，转为链表。(这里为什么还要判断rl.left == null，就是左子节点的左子节点为空？)
            if (root == null || root.right == null ||
                    (rl = root.left) == null || rl.left == null) {

                tab[index] = first.untreeify(map);
                return;
            }
            TreeNode<K, V> p = this, pl = left, pr = right, replacement;
            // 删除节点的左右子节点都不为空时
            if (pl != null && pr != null) {
                TreeNode<K, V> s = pr, sl;
                // sl等于删除节点右子节点的左子节点
                while ((sl = s.left) != null)
                    s = sl;
                boolean c = s.red;
                s.red = p.red;
                p.red = c;
                TreeNode<K, V> sr = s.right;
                TreeNode<K, V> pp = p.parent;
                if (s == pr) {
                    p.parent = s;
                    s.right = p;
                } else {
                    TreeNode<K, V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            } else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                TreeNode<K, V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }
            //以replacement为中心，进行红黑树性质的修复，replacement可能为s的右儿子或者p的儿子或者p自己
            TreeNode<K, V> r = p.red ? root : balanceDeletion(root, replacement);
            //p没有儿子或者s没有儿子，直接移除p
            if (replacement == p) {
                TreeNode<K, V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * 将结点太多的桶分割，这个方法只有在resize时调用，数组扩容，把原来的树拆成两颗，放到hash数组的新桶上。
         *
         * 拆分的时候会破坏树结构，所以先拆成两个链表再调用treeify来组装树。
         *
         * 将树从给定的结点分裂成低位和高位的两棵树，若新树结点太少则转为线性链表。只有resize时会调用
         *
         * tab值得是扩容后新的hashMap的table属性，就是桶数组(存放hash值的数组)
         *
         */
        final void split(HashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
            TreeNode<K, V> b = this;
            //低位头尾指针
            TreeNode<K, V> loHead = null, loTail = null;
            //高位头尾指针
            TreeNode<K, V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            //e从this开始遍历直到next为null
            for (TreeNode<K, V> e = b, next; e != null; e = next) {
                next = (TreeNode<K, V>) e.next;
                e.next = null;
                //这个算式实际是判断e.hash新多出来的有效位是0还是1，若是0则分去低位树，是1则分去高位树
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                } else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)
                    //分裂后的低位比6还小，就把红黑树转化为链表
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    // 如果hiHead == null，说明整棵树全都保留了，没有变化，就不用调后面的treeify
                    if (hiHead != null)
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    //高位所处的位置为原本位置+旧数组的大小即bit
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }


        /**
         * 左旋转，红黑树的左旋转，详情见README.md
         *
         */
        static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root,
                                                TreeNode<K, V> p) {
            TreeNode<K, V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        /**
         * 右旋转，详情见README.md
         */
        static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root,
                                                 TreeNode<K, V> p) {
            TreeNode<K, V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        /**
         * 保证插入后红黑树的性质：
         * 1.节点是红色或黑色。
         * 2. 根是黑色。
         * 3. 所有叶子都是黑色（叶子是NIL节点）。
         * 4. 每个红色节点必须有两个黑色的子节点。(从每个叶子到根的所有路径上不能有两个连续的红色节点。)
         * 5. 从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点（黑高）。
         */
        static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root,
                                                      TreeNode<K, V> x) {
            // 方便第5点，先把插入的节点设为红色
            x.red = true;
            for (TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    // 第2点，根是黑的
                    x.red = false;
                    return x;
                // xp代表x的爸爸，xp为黑的或者xp没有父节点，x使用红色没关系
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                // 如果x的父亲节点是左儿子
                if (xp == (xppl = xpp.left)) {
                    // x的父亲的父亲还有个右儿子节点，且右儿子节点是红色的
                    if ((xppr = xpp.right) != null && xppr.red) {
                        // 直接将x父亲的父亲变成红树，这一块不是很明白。所有情况都考虑到了么？
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        // 左旋
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        // 右旋
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else { // 和上面一样
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        /**
         * 删除后调整平衡
         */
        static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root,
                                                     TreeNode<K, V> x) {
            for (TreeNode<K, V> xp, xpl, xpr; ; ) {
                //删除结点为空或者删除的是根结点，直接返回
                if (x == null || x == root)
                    return root;
                //删除后x成为根结点，x的颜色改为黑色
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    //将一个红色的结点提升到删除结点的位置不会改变黑高
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {//x的父亲是左儿子
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // 下面是对称的操作
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K, V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * 从root开始递归检查红黑树的性质，仅在检查root是否落在table上时调用
         */
        static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
            TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (TreeNode<K, V>) t.next;
            if (tb != null && tb.next != t)
                return false; //t的前一个节点tb不为空，tb后一个节点应为t
            if (tn != null && tn.prev != t)
                return false; //t的后一个节点tn不为空，tn的前一个节点应为t
            if (tp != null && t != tp.left && t != tp.right)
                return false; //t的父节点tp不为空，t应该是tp的左子或者右子
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false; //t的左子tl不为空，t应该是tl的父节点，且tl的hash值小于等于t的hash
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false; //t的右子tr不为空，t应该是tr的父节点，且tr的hash值大于等于t的hash
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false; // 如果t是红的，那么t的儿子不能是红的
            if (tl != null && !checkInvariants(tl))
                return false; //递归校验t的左儿子
            if (tr != null && !checkInvariants(tr))
                return false; //递归校验t的右儿子
            return true;
        }
    }

}
