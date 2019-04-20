/*
 * Copyright (c) 1994, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Object类是每一个类的父类。
 * 所有的对象（包括数组）都实现了object类的方法。
 */
public class Object {

    /**
     * native 方法，表示由c/c++完成,在jre/bin目录下有很多.dll
     */
    private static native void registerNatives();

    static {
        registerNatives();
    }

    /**
     * native方法
     * 返回Object的运行时class对象，返回的对象是被静态同步方法锁定的对象
     *     （这意味着，该类的所有对象中，同时只有一个对象可以获得锁）。
     * 而且实际上返回的class对象是多态的，可以是调用者的子类
     */
    public final native Class<?> getClass();

    /**
     * native方法
     * hashCode方法返回调用对象的hash码(int)。hashCode必须满足以下协议：
     * 1.对同一个对象多次调用hashCode()方法，必须返回相同的值。
     * 2.如果两个对象通过equals方法相等，那么两个对象的hashCode必须相等。
     * 3.如果两个对象通过equals方法不相等，两个对象的hashCode不一定不相等。
     *   但是应该知道，不相等的对象返回不同的hash值，有助于提高hash表的性能。
     */
    public native int hashCode();

    /**
     * 判断两个对象是不是相等。该方法遵循如下性质：
     * 1.自反性：对于任意非空引用x，则x.equals(x)为true。
     * 2.对称性：对于任意非空引用x、y，若x.equals(y)为true，则y.equals(x)也为true。
     * 3.传递性：对于任意非空引用x、y、z，若x.equals(y)为true且y.equals(z)为true，
     *   则x.equals(z)为true。
     * 4.对于任何非空引用值x和y，多次调用x.equals（y）始终返回true或者始终返回false，
     * 5.对于任意非空引用x，则x.equals(null)返回false。
     *
     * 重写equals方法必须重写hashCode方法来保证对任意两个对象equals
     * 返回true时，他们的hashCode返回值必须相等。
     *
     */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    /**
     * protected修饰的native方法，创建和返回一个对象的复制。注意以下几点：
     * 1：x.clone() != x  是true
     * 2：一个对象可以被克隆的前提是该对象代表的类实现了Cloneable接口，
     *    否者会抛出一个CloneNotSupportedException异常。
     * 3：clone方法是浅复制，意思是不会新分配的内存，而是只创建了一个新引用。
     */
    protected native Object clone() throws CloneNotSupportedException;

    /**
     * 返回一个表示该对象的字符串，默认实现是：
     *  className+@+Integer.toHexString(hashCode())
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * native方法
     * 随机唤醒一个（因调用了wait方法）处于等待状态（waiting 或 time_wait）的线程
     */
    public final native void notify();

    /**
     * native方法
     * 唤醒所有（因调用了wait方法）处于等待状态（waiting 或 time_wait）的线程
     */
    public final native void notifyAll();

    /**
     * native 方法
     * 线程间的通信，将当前线程状态设为:等待状态（time_waiting)
     * timeout单位为毫秒,该方法只能同步方法或同步块中调用,超过时间后线程重新进入:可运行状态(runnable)
     */
    public final native void wait(long timeout) throws InterruptedException;

    /**
     * 重载方法，
     * timeout单位是毫秒，nanos单位是纳秒。
     * 比较迷的是：nanos>0，timeout+1就去调用wait(long timeout)了
     */
    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait(timeout);
    }

    /**
     * 调用了wait(0L)
     * wait()的作用是让当前线程进入等待状态，同时也会让当前线程释放它所持有的锁。
     *
     * 直到其他线程调用此对象的 notify()或notifyAll()方法，当前线程被唤醒(进入“就绪状态”)
     */
    public final void wait() throws InterruptedException {
        wait(0);
    }

    /**
     * 这个方法用于当对象被回收时调用，这个由JVM支持，
     * Object的finalize方法默认是什么都没有做，
     * 如果子类需要在对象被回收时执行一些逻辑处理，可以重写finalize方法。
     */
    protected void finalize() throws Throwable { }
}
