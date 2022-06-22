package com.newhiber.newhiber.tools;

import android.content.Context;

import com.newhiber.newhiber.cons.Cons;


public abstract class CrashHanlder implements Thread.UncaughtExceptionHandler {

    public Thread.UncaughtExceptionHandler mDefaultHandler;// 自身接口
    private Context context;

    protected CrashHanlder(Context context) {
        this.context = context;
        init();
    }

    /**
     * 回调接口
     *
     * @param context 环境
     * @param thread  线程
     * @param ex      异常
     */
    public abstract void catchCrash(Context context, Thread thread, Throwable ex);

    /**
     * 初始化
     */
    private void init() {
        // 1.获取系统默认的UncaughtException处理
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 2.设置该CrashHandler为程序的默认处理
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 异常捕获
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 如果用户没有处理则让系统默认的异常处理器来处
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            catchCrash(context, thread, ex);
        }
    }

    /**
     * 自定义错误捕获
     *
     * @param ex 异常
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        // 异常为空, 直接返回false
        if (ex == null) {
            return false;
        }
        // 打印错误
        Lgg.t(Cons.TAG).ee(getClass() + "--> crash cause: " + ex.getCause());
        Lgg.t(Cons.TAG).ee(getClass() + "-->crash message: " + ex.getMessage());
        return true;
    }
}
