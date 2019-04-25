* LinkedHashMap有序；查询顺序，访问顺序
* LinkedHashMap继承HashMap，他的数据结构也是链表--红黑树。之所以叫LinkedHashMap是因为他还维护了一张双向链表用来排序
* LinkedHashMap比HashMap多before,after,head,tail属性，用来维护双向链表，变得有顺序
* LinkedHashMap遍历速度只和实际数据量有关。
    * 当HashMap容量initialCapacity很大，实际数据很少时(不常见)，LinkedHashMap遍历速度快
    * 其他时候，HashMap遍历效率高