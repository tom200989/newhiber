package com.newhiber.newhiber.hiber;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.newhiber.newhiber.cons.Cons;
import com.newhiber.newhiber.hiber.language.RootApp;
import com.newhiber.newhiber.tools.Lgg;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by qianli.ma on 2019/1/28 0028.
 */
public class ActivityHelper implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        printRootAcList("check before");
        Activity tempActivity = null;
        for (Activity listActivity : RootApp.activities) {
            // 0.查找原先存在的AC
            String listName = listActivity.getClass().getName();
            String currentName = activity.getClass().getName();
            if (listName.equalsIgnoreCase(currentName)) {
                tempActivity = listActivity;
            }
        }

        if (tempActivity != null) {
            // 1.先移除原有的AC
            tempActivity.finish();
            printRootAcList("remove before");
            RootApp.activities.remove(tempActivity);
        }

        // 2.再添加新建的AC
        printRootAcList("add before");
        RootApp.activities.add(activity);
        // 3.赋值当前顶层的activity
        RootApp.TOP_ACTIVITY = activity;
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        RootApp.TOP_ACTIVITY = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        RootApp.activities.remove(activity);
    }

    /* -------------------------------------------- private -------------------------------------------- */

    /**
     * 定位生命周期流程
     *
     * @param pre 预设打印前缀
     */
    private void printRootAcList(String pre) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{\n");
        for (Activity activity : RootApp.activities) {
            buffer.append(activity.getClass().getSimpleName()).append("\n");
        }
        buffer.append("}");
        Lgg.t(Cons.TAG3).ii(pre + buffer.toString());
    }

    /* -------------------------------------------- public -------------------------------------------- */

    /**
     * 判断ac位于视窗栈顶
     *
     * @param context 域
     * @param acName  activity绝对路径
     * @return T:位于栈顶
     */
    protected static boolean isTopActivity(Context context, String acName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (task.topActivity.getClassName().equalsIgnoreCase(acName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 杀死除共有部分的MainActivity外的Activity
     *
     * @param keepActivitys 需要保持存活的Activity
     */
    protected static void killActivitys(Class... keepActivitys) {

        // 1.创建记录需要保留的Activity索引的集合
        List<Activity> keepTemp = new ArrayList<>();

        // 2.遍历找到需要保留的Activity
        for (Activity currActivity : RootApp.activities) {
            for (Class keep : keepActivitys) {
                // 2.1.比较类名
                String currentName = currActivity.getClass().getName();
                String keepName = keep.getName();
                if (currentName.equalsIgnoreCase(keepName)) {
                    keepTemp.add(currActivity);
                }
            }
        }

        // 3.从当前集合中清理出需要保留的部分
        for (Activity activity : keepTemp) {
            RootApp.activities.remove(activity);
        }

        // 4.将剩下的部分进行finish
        for (Activity activity : RootApp.activities) {
            activity.finish();
        }
    }

    /**
     * 清除全部的activity
     */
    protected static void killAllActivity() {
        // 0.先finish
        for (Activity activity : RootApp.activities) {
            activity.finish();
        }
        // 1.在清空集合
        RootApp.activities.clear();
    }
}
