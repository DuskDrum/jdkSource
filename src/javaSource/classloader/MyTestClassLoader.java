package javaSource.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author WY
 * @date 2019/4/15 13:56
 */
public class MyTestClassLoader extends  ClassLoader{

    private String rootUrl;

    @Override
    protected Class<?> findClass(String name) {
        Class clazz = null;//this.findLoadedClass(name); // 父类已加载
        //if (clazz == null) {  //检查该类是否已被加载过
        byte[] classData = getClassData(name);  //根据类的二进制名称,获得该class文件的字节码数组
        if (classData == null) {
            System.err.println("未找到此类");
            return null;
        }
        clazz = defineClass(name, classData, 0, classData.length);  //将class的字节码数组转换成Class类的实例
        //}
        return clazz;
    }


    private byte[] getClassData(String name) {
        InputStream is = null;
        try {
            String path = classNameToPath(name);
            URL url = new URL(path);
            byte[] buff = new byte[1024*4];
            int len = -1;
            is = url.openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((len = is.read(buff)) != -1) {
                baos.write(buff,0,len);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String classNameToPath(String name) {
        return rootUrl + "/" + name.replace(".", "/") + ".class";
    }


}
