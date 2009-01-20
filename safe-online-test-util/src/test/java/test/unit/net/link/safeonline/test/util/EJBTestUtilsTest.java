/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package test.unit.net.link.safeonline.test.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class EJBTestUtilsTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(EJBTestUtilsTest.class);


    public static interface TestIface {

        void func();
    }

    public static class TestClass implements TestIface {

        private static final Log testLOG = LogFactory.getLog(TestClass.class);


        public void func() {

            testLOG.debug("func invoked");
        }
    }

    private static class TestInvocationHandler implements InvocationHandler {

        private static final Log handlerLOG = LogFactory.getLog(TestInvocationHandler.class);

        private final Object     object;


        public TestInvocationHandler(Object object) {

            this.object = object;
        }

        public Object invoke(@SuppressWarnings("unused") Object proxy, Method method, Object[] args)
                throws Throwable {

            handlerLOG.debug("invoke");
            return method.invoke(object, args);
        }
    }


    public void testProxy()
            throws Exception {

        // setup
        TestClass origObject = new TestClass();
        TestInvocationHandler testInvocationHandler = new TestInvocationHandler(origObject);
        TestIface testObject = (TestIface) Proxy.newProxyInstance(EJBTestUtilsTest.class.getClassLoader(), TestClass.class.getInterfaces(),
                testInvocationHandler);

        // operate
        testObject.func();

        // verify
        LOG.debug("testObject type: " + testObject.getClass().getName());
        assertFalse(testObject instanceof TestClass);
    }


    private static class TestInterceptor implements MethodInterceptor {

        private static final Log interceptorLOG = LogFactory.getLog(TestInterceptor.class);

        private final Object     object;


        public TestInterceptor(Object object) {

            this.object = object;
        }

        public Object intercept(@SuppressWarnings("unused") Object obj, Method method, Object[] args,
                                @SuppressWarnings("unused") MethodProxy proxy)
                throws Throwable {

            interceptorLOG.debug("intercept");
            return method.invoke(object, args);
        }
    }


    public void testCgLib()
            throws Exception {

        // setup
        TestClass origObject = new TestClass();

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(TestClass.class);
        TestInterceptor testInterceptor = new TestInterceptor(origObject);
        enhancer.setCallback(testInterceptor);
        TestClass object = (TestClass) enhancer.create();

        // operate
        object.func();

        // verify
        // assertTrue(object instanceof TestClass);
        // ^ object is declared as TestClass, making this assert pretty useless
    }
}
