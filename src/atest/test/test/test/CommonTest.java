package atest.test.test.test;

import org.junit.Test;
import sun.misc.Launcher;

import java.net.URL;
import java.util.HashMapNote.HashMapTest;
import java.util.stream.IntStream;

/**
 * @author WY
 * @date 2019/4/11 14:47
 */
public class CommonTest {

    @Test
    public void getClassLoader(){
        ClassLoader classLoader = HashMapTest.class.getClassLoader();
        while(classLoader!=null){
            System.out.println(classLoader);
            classLoader = classLoader.getParent();
        }
        System.out.println(classLoader);
    }

    @Test
    public void getBootstrap() {
        URL[] urls = Launcher.getBootstrapClassPath().getURLs();
        IntStream.range(0,urls.length).forEach(i->{
            System.out.println(urls[i].toExternalForm());
        });
    }


    @Test
    public void testObject(){
        Object obj;
    }

    @Test
    public void testEnum(){
//        Enum
    }
}
