package java.util.HashMapNote;

import javaSource.classloader.MyTestClassLoader;
import org.ehcache.sizeof.SizeOf;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;


/**
 * @author WY
 * @date 2019/4/11 16:45
 *
 * 需要注意,运行会出错：
 * 这条安全异常是由Java类加载的“双亲委派模型”所导致的。在双亲委派模型中，由父加载类加载的类，下层加载器是不能加载的。
 * 本例中最高层加载器BootstrapClassLoader加载了classpath路径下所定义的java.包内的类，
 * 而java包就不能由BootstrapClassLoader的下层加载器AppClassLoader加载了。
 * 这也是java安全机制中对于恶意代码所采取的防护措施。
 *
 */
public class HashMapTest extends HashMap{

    /**
     * 测试getTreeNode方法，{@link java.util.HashMap.TreeNode#getTreeNode}
      */

    @Test
    public void testGetTreeNode(){
        // 如果解决了异常：java.lang.SecurityException: Prohibited package name: java.util，就可以直接调用扩容方法resize了
        MyTestClassLoader myTestClassLoader = new MyTestClassLoader();
        Map<String,String> param = new HashMap<>();
        param.put("1","2");
        // 循环8次扩容
        IntStream.range(0,8).forEach(i->{
            System.out.println("这是第.."+i);
//            ((HashMap<String, String>) param).resize(); //打破双亲委派的封印才能正常运行(挪到java.util包下面)
        });
        param.get("1");

    }


    /**
     * 测试|与||的区别
     */
    @Test
    public void test(){
        //两个数都转为二进制，然后从高位开始比较，两个数只要有一个为1则为1，否则就为0
        System.out.printf("测试位或运算符[%d]\n",12|8);
        //当|用作判断时，前后代码都会执行，且任是有一个为true就是true
        int i = 0;
        if(true||i-1!=0) {
            System.out.printf("测试||的区别 [%d]\n", i);
            if(true | (i--)==0) {
                System.out.printf("测试|的区别 [%d]\n", i);
            }
        }
    }

























    class Hasher{

        @Override
        public int hashCode() {
            // 保证碰撞
            return 1;
        }
    }

}
