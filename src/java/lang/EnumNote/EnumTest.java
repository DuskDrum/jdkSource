package java.lang.EnumNote;

/**
 * @author WY
 * @date 2019/4/20 23:14
 */
public class EnumTest {

    class EnumSon extends Enum{
        // 这里发现，Enum类虽然是抽象类，但是不允许被继承：
        // 因为Enum类提供的构造方法是 protected 修饰的
    }
}
