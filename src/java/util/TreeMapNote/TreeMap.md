## TreeMap

## 源码中的思考
1. 红黑树的遍历方式:

    红黑树(二叉树)的遍历将二叉树中的结点由非线性排列变为线性排列。
    一颗二叉树由根节点、左子树和右子树三部分组成，只讨论三种：DLR(先序)，LDR(中序),LRD(后序)（D、L、R分别代表遍历根节点、遍历左子树、遍历右子树）
    
    ![](https://github.com/MorningBells/picture/blob/master/%E7%BA%A2%E9%BB%91%E6%A0%91%E5%85%88%E5%BA%8F%E4%B8%AD%E5%BA%8F%E5%90%8E%E5%BA%8F.jpg?raw=true)
    
    * 先序遍历(DLR):首先访问根节点，然后先序遍历左子树，最后先序右子树。
        * A-B-D-H-I-E-J-C-F-G
    * 中序遍历(LDR):首先中序遍历根节点的左子树，然后根节点，最后中序遍历右子树
        * H-D-I-B-J-E-A-F-C-G
    * 后序遍历(LRD):首先后序遍历根节点的左子树，然后后续遍历根节点右子树，最后根节点    
        * H-I-D-J-E-B-F-G-C-A
       
    #### TreeMap#successor方法：
     ```java
        static <K,V> Entry<K,V> successor(Entry<K,V> t) {
           if (t == null)
               return null;
           else if (t.right != null) {
               // 有右子树的节点，后继节点就是右子树的“最左子树”
               // 因为“最左子树”是一个节点的最小子树(通俗说就是红黑树里左子树比右子树小)
               Entry<K,V> p = t.right;
               while (p.left != null)
                   p = p.left; //  遍历赋值，最左边的节点
               return p;
           } else {
               // 如果右子树为空，则寻找当前节点所在左子树的第一个爸爸
               Entry<K,V> p = t.parent;
               Entry<K,V> ch = t;
               // 校验p是不是p爸爸的右儿子，当ch!=p.right时跳出循环
               while (p != null && ch == p.right) {
                   ch = p;
                   p = p.parent;
               }
               return p;
           }
       }
     ```
     怎么理解这个方法呢，这个方法常和TreeMap#getFirstEntry连用(返回树的最左节点)，和中序遍历很类似，有已下的情况：
     > a. t为空节点，返回null<br/>
       b. t为有右子树的节点，返回就是右子树的“最左节点”<br/>
       c. t为没有右子树，本身为右子树的节点，返回该节点所在左子树的第一个parent节点。有点难理解，建议画颗树瞧瞧<br/>
       d. t为没有右子树，本身为左子树的节点，直接返回parent节点(可以和c合到一起)
       
     #### TreeMap#predecessor方法：
     ```java
     static <K,V> Entry<K,V> predecessor(Entry<K,V> t) {
            if (t == null)
                return null;
            else if (t.left != null) {
                Entry<K,V> p = t.left;
                while (p.right != null)
                    //t左子节点的最右子节点
                    p = p.right;
                return p;
            } else {
                Entry<K,V> p = t.parent;
                Entry<K,V> ch = t;
                // 返回右子树的第一个parent节点，当ch!=p.left时跳出循环
                while (p != null && ch == p.left) {
                    ch = p;
                    p = p.parent;
                }
                return p;
            }
        }
    ```
    这个方法常和TreeMap#successor方法完全类似：
    > a. t为空节点，返回null<br/>
      b. t为有左子树的节点，返回就是左子树的“最右节点”<br/>
      c. t为没有左子树，本身为左子树的节点，返回该节点所在右子树的第一个parent节点<br/>
      d. t为没有左子树，本身为右子树的节点，直接返回parent节点(可以和c合到一起)
           
       
    