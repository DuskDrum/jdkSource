## 已知
* HashMap的结构：数组+(链表/树）
    * (bucket)数组:根据hashCode方法定位
    * (Node)链表：找到bucket后，通过keys.equals()定位
    * (TreeNode)树：当链表过长时，用红黑树代替链表
* 当达到HashMap的负载因子容量时，会扩容，进行rehashing操作

## 问题
* 类中定义了哪些方法，有什么作用
* 类中定义了哪些内部类，有什么作用
* 怎么减少碰撞(hashCode方法和equals方法)
* jdk1.8怎么避免rehashing的死循环的
* 多线程死循环，数据丢失，数据重复怎么回事
## 其他的问题
1. jdk1.8中为什么说hashMap并发问题？1.8已经没有1.7中形成环链的问题。put get 不应该都安全的吗，其他的都有fast-fail 为什么还是说它不适合用于并发环境下呢？
    * 1.8解决了死循环问题，但是还没解决，数据丢失，数据重复问题。他有并发问题的根本是put get没有做同步处理。举个例子，HashMap进行put操作时是先计算hashCode找到桶，然后遍历桶内的链表找到插入位置插入。如果2个线程t1、t2分别put一个hashCode相同的元素e1、e2，就可能导致找到相同的插入位置(a)，t1里a.next=e1，t2里a.next=e2，就只有一个数据保留了下来，丢了一个。
2. 参考HashMap的性能：[如何看待代码中滥用HashMap？](https://www.zhihu.com/question/28119895)


## 源码解析
1. 红黑树的旋转

    ![](https://raw.githubusercontent.com/MorningBells/picture/master/%E5%B7%A6%E6%97%8B%E8%BD%AC.png)
    
    ```java
          
    //左旋操作，上图中从右到左，y的爸爸x,变成了y的左儿子。y的左儿子变成了x的右儿子
    // 方法中：p指的是图中x,root指的是这棵树的根节点
    static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root,TreeNode<K, V> p) {
       TreeNode<K, V> r, pp, rl;
       //x的右儿子，也就是y
       if (p != null && (r = p.right) != null) {
           // 把x的右儿子赋值为y的左儿子
           if ((rl = p.right = r.left) != null)
               rl.parent = p;
           //y的爸爸赋值为x的爸爸
           if ((pp = r.parent = p.parent) == null)
               //判断如果是根节点，一定是黑色的
               (root = r).red = false;
           // 用y替换x原来的位置(y变成了x原来爸爸的儿子)
           else if (pp.left == p)
               pp.left = r;
           else
               pp.right = r;
           // x变成了y的左儿子
           r.left = p;
           p.parent = r;
       }
       return root;
   }
   //右旋操作,上图中从左到右，x的爸爸y,变成了x的右儿子。x的右儿子变成了y的左儿子
   // root指的是根节点，p是y
   static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root,TreeNode<K, V> p) {
       TreeNode<K, V> l, pp, lr;
       // l是p的左儿子，即x
       if (p != null && (l = p.left) != null) {
           // y的左儿子赋值为x的右儿子
           if ((lr = p.left = l.right) != null)
               lr.parent = p;
           // y的爸爸变成x的爸爸
           if ((pp = l.parent = p.parent) == null)
               (root = l).red = false;
           else if (pp.right == p)
               pp.right = l;
           else
               pp.left = l;
           //y成为x的右儿子
           l.right = p;
           p.parent = l;
       }
       return root;
   }

    ```