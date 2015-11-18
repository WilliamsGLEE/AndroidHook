写过windows编辑的同学肯定对当时的特别强大的HOOK记忆犹新，不管是什么系统事件，都能捕捉住，那么在android中，如何实现Hook机制呢。

> hook，又叫钩子，通常是指对一些方法进行拦截。这样当这些方法被调用时，也能够执行我们自己的代码，这也是面向切面编程的思想（AOP）

android中，本身并不提供这样的拦截机制，但是有时候，我们可以在一些特殊的场合实现一种的Hook方法。
大致思路：
1. 找到需要Hook方法的系统类（最好是单例的情况)
2. 利用java的动态代理这个系统类
3. 使用反射的方法把这个系统类替换成你的动态代理类

这里，我们以最常见到的拦截 Activity的生命周期为例。经过研究观察，我们发现在android.app.ActivityManagerNative中有一个gDefault属性，这个属性是static final的，返回的是一个singleton的IActivityManager。

```
private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
	protected IActivityManager More ...create() {
		IBinder b = ServiceManager.getService("activity");
			if (false) {
				Log.v("ActivityManager", "default service binder = " + b);
			}
			IActivityManager am = asInterface(b);
			if (false) {
				Log.v("ActivityManager", "default service = " + am);
			}
			return am;
	}
};
```

这个就给我们可以操作的入口。
有了目标之后，就可以用自己的动态代理，取代这个类。我们先建立一个动态代理。

```
public class MyActivityManager  implements InvocationHandler{

    public static final String TAG = "MyActivityManager";
    public Object orignal ;

    public Class<?> getOrignalClass() throws ClassNotFoundException {
        return Class.forName("android.app.ActivityManagerNative");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Log.d(TAG, "before method called:" + method.getName());
        final Object obj =  method.invoke(orignal, args);
        Log.d(TAG, "after method called:" + method.getName());
        return obj;
    }
}
```

这个类实现了InvocationHandler，主要用于动态代理。实现的比较简单，仅仅是在调用系统方法的前后，打印日志，当然你也可以做得更多。调用系统的方法也是以原始系统对象的基础上来调用的。

现在我们就可以做点小动作，把系统的类替换成我们的类。

前面说到了，我们需要获取原始系统对象。
第一步获取Singleton<IActivityManager> gDefault对象。

```
MyActivityManager activityManager = new MyActivityManager();
        Class oriClass = activityManager.getOrignalClass();
        Object obj = FieldUtils.readStaticField(oriClass, "gDefault");
```

这是个由Singleton包装的IActivityManager, 再进一步，查看Singleton的源码：

```
public abstract class Singleton<T> {
    private T mInstance;

    public Singleton() {
    }

    protected abstract T create();

    public T get() {
        synchronized(this) {
            if(this.mInstance == null) {
                this.mInstance = this.create();
            }

            return this.mInstance;
        }
    }
}
```

最终是要获取mInstance的对象

```
final Object oriObj = FieldUtils.readField(obj, "mInstance");
```

这个oriObj就是IActivityManager的一个实现。

我们把这个对象赋给MyActivityManager.

```
activityManager.orignal = oriObj;
```

接下来，创建一个代理，

```
List<Class<?>> interfaces = Utils.getAllInterfaces(oriObj.getClass());
        Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
        final Object object = Proxy.newProxyInstance(oriObj.getClass().getClassLoader(),ifs, activityManager);
```

最后，把这个代理对象写入到系统的对象中。

```
 FieldUtils.writeStaticField(oriClass, "gDefault", new android.util.Singleton<Object>() {
            @Override
            protected Object create() {
                return object;
            }
        });
```

运行程序，可以看到以下日志：

11-18 14:24:24.322 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:checkPermission
11-18 14:24:24.327 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:checkPermission
11-18 14:24:24.327 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:checkPermission
11-18 14:24:24.327 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:checkPermission
11-18 14:24:24.387 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:broadcastIntent
11-18 14:24:24.477 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:broadcastIntent
11-18 14:24:24.502 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:activityResumed
11-18 14:24:24.507 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:activityResumed
11-18 14:24:24.507 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:broadcastIntent
11-18 14:24:24.507 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:broadcastIntent
11-18 14:24:24.507 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:activityPaused
11-18 14:24:24.512 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:activityPaused
11-18 14:24:24.622 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:activitySlept
11-18 14:24:24.622 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:activitySlept
11-18 14:24:24.812 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:activityStopped
11-18 14:24:24.812 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:activityStopped
11-18 14:24:24.832 9881-9881/com.win16.hooktest D/MyActivityManager: before method called:activityIdle
11-18 14:24:24.832 9881-9881/com.win16.hooktest D/MyActivityManager: after method called:activityIdle

小结
这里提供一种拦截系统API的思路，供大家参考。另外还有一种更加底层的拦截方式，修改JVM中的slot来实现，有兴趣的同学可以参考:[https://github.com/alibaba/dexposed](https://github.com/alibaba/dexposed)

本文相关源码下载：[https://github.com/mutsinghua/AndroidHook](https://github.com/mutsinghua/AndroidHook)