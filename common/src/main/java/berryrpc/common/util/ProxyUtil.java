package berryrpc.common.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class ProxyUtil {
    /**
     * Currently, this only returns new instance of classes that is accessible to this class
     * and its nullary constructor accessible too.
     * @param cls the type of the instantiated object
     * @return new instance of the type {@code cls}
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static Object newInstance(Class<?> cls) {
        Object instance = null;
        try {
            instance = cls.newInstance();
        } catch (IllegalAccessException e) {
            log.error("The class or its nullary constructor is not accessible", e);
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            log.error("Instantiation exception", e);
            throw new RuntimeException(e);
        }
        return instance;
    }

    /**
     * Use CgLib to instantiate a class.
     * @param cls the type of the instantiated object
     * @return new instance of the type {@code cls}
     */
    public static Object newInstanceByCglib(Class<?> cls) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(cls);
        enhancer.setCallback(NoOp.INSTANCE);
        return enhancer.create();
        // Todo return (T) enhancer.create(ctr.getParameterTypes(),args);

    }

    public static Object invokeMethod(Object target, Method method, Object ...args) throws IllegalAccessException, InvocationTargetException {
        Object ret = null;
        try {
            method.setAccessible(true);
            ret = method.invoke(target, args);
        } catch (IllegalAccessException e) {
            log.error("Access exception", e);
            throw e;
        } catch (InvocationTargetException e) {
            log.error("", e);
            throw e;
        }
        return ret;
    }
}
