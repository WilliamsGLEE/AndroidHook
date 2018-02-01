package com.win16.hookdemo;

import android.util.Log;

import com.win16.hookdemo.utils.FieldUtils;
import com.win16.hookdemo.utils.Utils;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Created by REXZOU on 11/17/2015.
 */
public class HookHelper{


    private static final String TAG = "HookHelper";

    public static void installHook() throws IllegalAccessException, ClassNotFoundException {

        MyActivityManager activityManager = new MyActivityManager();
        Class oriClass = activityManager.getOrignalClass();
        Log.e(TAG, "oriClass:"+oriClass);

        final Object obj = FieldUtils.readStaticField(oriClass, "IActivityManagerSingleton");
        Log.e(TAG, "obj:"+obj);
        final Object oriObj = FieldUtils.readField(obj, "mInstance");
        Log.e(TAG, "oriObj:"+oriObj);
        Log.e(TAG, "oriObj.getClass:"+oriObj.getClass());

        activityManager.orignal = oriObj;
        List<Class<?>> interfaces = Utils.getAllInterfaces(oriObj.getClass());
        Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
        for (Class it : ifs) {
            Log.e(TAG, "iterface:"+it);
        }

        final Object proxyObj = Proxy.newProxyInstance(oriObj.getClass().getClassLoader(),ifs, activityManager);
        Log.e(TAG, ":"+Proxy.getProxyClass(oriObj.getClass().getClassLoader(), ifs));
        Log.e(TAG, "proxy:"+proxyObj);

        FieldUtils.writeStaticField(oriClass, "IActivityManagerSingleton", new android.util.Singleton<Object>() {
            @Override
            protected Object create() {
                return proxyObj;
            }
        });
    }

    // public static void installHook() throws IllegalAccessException, ClassNotFoundException {
    //
    //     MyActivityManager activityManager = new MyActivityManager();
    //     Class oriClass = activityManager.getOrignalClass();
    //     Object obj = FieldUtils.readStaticField(oriClass, "gDefault");
    //     final Object oriObj = FieldUtils.readField(obj, "mInstance");
    //
    //     activityManager.orignal = oriObj;
    //     List<Class<?>> interfaces = Utils.getAllInterfaces(oriObj.getClass());
    //     Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
    //     final Object object = Proxy.newProxyInstance(oriObj.getClass().getClassLoader(),ifs, activityManager);
    //     FieldUtils.writeStaticField(oriClass, "gDefault", new android.util.Singleton<Object>() {
    //         @Override
    //         protected Object create() {
    //             return object;
    //         }
    //     });
    // }
}
