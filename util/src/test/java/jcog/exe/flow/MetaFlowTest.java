package jcog.exe.flow;

import jcog.WTF;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static jcog.exe.flow.MetaFlow.exe;
import static jcog.exe.flow.MetaFlow.stack;

class MetaFlowTest {

    static void a() {
        exe().good((float) (Math.random()*1f));
    }
    static int b() {
        if (Math.random() < 0.5f)
            exe().bad( 0.2f, "x" );
        else {
            if (Math.random() < 0.1f) {
                throw new WTF();
            } else {
                exe().bad(0.5f, "y");
            }
        }
        return 0;
    }

    static Object c(int x) {
        a(); b();
        return null;
    }

    @Test public void testMetaFlowExample() {

        MetaFlow m = exe().forkUntil(System.nanoTime() + 500L * 1_000_000L,
                MetaFlowTest::a,
                MetaFlowTest::b,
                () -> c(0));
        System.out.println(m.plan.prettyPrint());
    }

    @Disabled
    @Test
    public void testbyteBuddy() throws IllegalAccessException, InstantiationException {

        Class<?> m = new ByteBuddy(ClassFileVersion.JAVA_V11)
                .subclass(MyClass.class)
                //.rebase(NAR.class, ClassFileLocator.ForClassLoader.ofClassPath())
                //.annotateType(AnnotationDescription.Builder.ofType(Baz.class).build())
                .method(ElementMatchers.isAnnotatedWith(MetaFlow.Value.class)).

                        intercept(MethodDelegation.to(new GeneralInterceptor()))
//                        intercept(InvocationHandlerAdapter.of((objWrapper, method, margs) -> {
//                            try {
//                                Method superMethod = objWrapper.getClass().getSuperclass().getDeclaredMethod(
//                                        method.getName(), Util.typesOfArray(margs));
//                                return superMethod.invoke(objWrapper, margs); //method.invoke(objWrapper, margs);
//                            } catch (Throwable t) {
//                                throw new RuntimeException(t);
//                            }
//                        }))
                .make()

                //.load(Thread.currentThread().getContextClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .load(MyClass.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();

        MyClass mm = (MyClass) m.newInstance();

        System.out.println(m);
        System.out.println(mm);
        System.out.println(mm.getClass() + " extends " + mm.getClass().getSuperclass());
        mm.test();
        mm.test(2);
    }
    @Test
    public void testMetaFlow1() {


        MetaFlow f = exe();
        stack().reset().print();
        System.out.println();


    }

    public static class GeneralInterceptor {
        @RuntimeType
        public Object intercept(@AllArguments Object[] args,
                                //@Origin Method method
                                @SuperCall Callable<?> zuper
        ) {
            // intercept any method of any signature
            try {
                Object returnValue = zuper.call();
                System.out.println(returnValue + " " + Arrays.toString(args));
                return returnValue;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class MyClass {

        public MyClass() {
        }

        @MetaFlow.Value
        public float test() {
            return 1f;
        }
        @MetaFlow.Value
        public float test(float param) {
            return param;
        }
    }

}