package com.win16.hookdemo;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by REXZOU on 11/17/2015.
 */
public class MyActivityManager  implements InvocationHandler{

    public static final String TAG = "MyActivityManager";
    public Object orignal ;

    public Class<?> getOrignalClass() throws ClassNotFoundException {
        return Class.forName("android.app.ActivityManager");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Log.e(TAG, "========before method called:" + method.getName());
        if (args != null) {
            for (Object obj : args) {
                Log.e(TAG, "arg:" + obj);
            }
            try {
                if (args[2].toString().contains("LoginActivity")) {
                    return 23344;
                }
            } catch (Exception e) {

            }
        }
        final Object obj =  method.invoke(orignal, args);
        Log.e(TAG, "-===========after method called:" + method.getName());
        return obj;
    }
}
