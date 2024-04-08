package com.newhiber.newhiber.hiber;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.lintcheck.lintcheck.helper.LintHelper;
import com.newhiber.newhiber.R;
import com.newhiber.newhiber.bean.ApplyPermissionBean;
import com.newhiber.newhiber.bean.GradientBean;
import com.newhiber.newhiber.bean.RootProperty;
import com.newhiber.newhiber.bean.SkipBean;
import com.newhiber.newhiber.cons.Cons;
import com.newhiber.newhiber.hiber.language.LangHelper;
import com.newhiber.newhiber.impl.PermissionAction;
import com.newhiber.newhiber.impl.RootEventListener;
import com.newhiber.newhiber.tools.Lgg;
import com.newhiber.newhiber.tools.backhandler.BackHandlerHelper;
import com.newhiber.newhiber.ui.DefaultFragment;
import com.newhiber.newhiber.ui.LintActivity;
import com.newhiber.newhiber.ui.PermissFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.FloatRange;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import static com.newhiber.newhiber.impl.PermissionAction.PermissionType.NOW_OTHER_FALSE;
import static com.newhiber.newhiber.impl.PermissionAction.PermissionType.NOW_OTHER_TRUE;
import static com.newhiber.newhiber.impl.PermissionAction.PermissionType.NOW_WRITE_READ;

/*
 * Created by qianli.ma on 2018/6/20 0020.
 */

@SuppressLint("Registered")
public abstract class RootMAActivity extends FragmentActivity {

    /**
     * 配置对象
     */
    private RootProperty rootProperty;

    /**
     * fragment调度器
     */
    protected FraHelpers fraHelpers;

    /**
     * 日志标记
     */
    public String TAG = Cons.TAG;

    /**
     * 流程标记
     */
    public String TRACK = Cons.TRACK;

    /**
     * 状态栏颜色ID 如:R.color.xxx
     */
    private int colorStatusBar = R.color.colorHiberAccent;

    /**
     * 状态栏渐变色对象
     */
    private GradientBean gradientBean;

    /**
     * 状态栏启动沉浸模式
     */
    private boolean immerse;

    /**
     * 状态栏透明度
     */
    @FloatRange(from = 0, to = 1)
    private float statusbarAlpha = 1;

    /**
     * 布局ID 如:R.layout.xxx
     */
    private int layoutId = R.layout.activity_hiber;

    /**
     * 是否保存Activity状态 建议为false
     */
    private boolean isSaveInstanceState;

    /**
     * 项目目录
     */
    private String projectDirName = Cons.RootDir;

    /**
     * fragment容器ID 如:R.id.frame
     */
    private int containId = R.id.fl_hiber_contain;

    /**
     * 自定义吐司
     */
    public TextView tvToast;

    /**
     * fragment字节码数组 如:[fragment1.class,fragment2.class,...]
     */
    private Class[] fragmentClazzs = {DefaultFragment.class};

    /**
     * 是否需要全屏
     */
    private boolean isFullScreen = true;

    /**
     * 包名, 用于Lint检测
     */
    protected String packageName = "";

    /**
     * 存储「frag绝对路径, frag字节码」
     */
    private final HashMap<String, Class> classFragMap = new HashMap<>();

    /**
     * 通过extra方式需要接收的信标符号
     */
    protected static String INTENT_NAME = "SkipBean";

    /**
     * lint检查开关 ( T: 打开lint检查, 默认true)
     */
    public boolean isLintCheck = true;

    /**
     * Eventbus 泛型字节码集合
     */
    protected List<Class> eventClazzs = new ArrayList<>();

    /**
     * Eventbus 数据回调监听器集合
     */
    protected List<RootEventListener> eventListeners = new ArrayList<>();

    /**
     * 用于辅助framgent -- 防止fragment跳转过快导致的事务没及时提交
     */
    protected Handler handler = new Handler();

    /**
     * 2024新权限用法: 用于装载需要申请的权限(由外部传入)
     */
    protected String[] needPermissions = {};

    /**
     * 2024新权限用法: 申请权限的回调动作
     * (外部Activity通过重写 Activity.applyPermission() 进行传入)
     */
    protected PermissionAction permissActionForActivity;

    /**
     * 2024新权限用法: 申请权限的回调动作
     * (外部Fragment通过调用 startPermission() 进行传入)
     */
    protected PermissionAction permissActionForFragment;

    /**
     * 2024新权限用法: 全部权限通过后的回调 (由fragment传入)
     */
    // protected Runnable allGrantedRunnable = null;

    /**
     * 2024新权限用法: 权限被拒绝后的回调 (由fragment传入)
     */
    // protected Runnable deniedRunable = null;

    /**
     * 2024新权限用法: 用于装载需要申请的权限(由外部传入)
     * 格式: [String[], deniedRunnable]
     * String[]: 需要申请的权限
     * deniedRunnable: 被拒绝后的操作
     *
     * @return 需要申请的权限集合
     */
    public ApplyPermissionBean applyPermission() {
        return null; // 默认返回空
    }

    /**
     * 2024新权限用法: 申请权限结果处理器
     */
    private final ActivityResultLauncher<String[]> permissinLauncher = registerForActivityResult(//
            new ActivityResultContracts.RequestMultiplePermissions(), //
            this::handlePermissionsResult);//

    /**
     * 2024新权限用法: 跳转[所有文件]及[应用详情页]处理器
     */
    private final ActivityResultLauncher<Intent> settingLauncher = registerForActivityResult(//
            new ActivityResultContracts.StartActivityForResult(), result -> {//
                // TODO: 3/29/2024 当前业务暂时没有这个需求, 以后在说
                Log.i(TAG, getClass().getName() + " A0.15 用户打开了系统设置页: " + result.getResultCode());
            });

    /**
     * // (toat 保留) 3.2024新权限用法: 处理权限结果
     *
     * @param permissions 权限结果
     */
    private void handlePermissionsResult(Map<String, Boolean> permissions) {
        // (toat 保留) 3.1.打印所有请求的权限
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            Log.v(TAG, getClass().getName() + " A0.4.1 用户操作一轮之后的结果: " + entry.getKey() + " : " + entry.getValue());
        }

        // (toat 保留) 3.2.创建一个新的Map来存储被拒绝的权限
        Map<String, Boolean> deniedPermissions = new ArrayMap<>();
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (!entry.getValue()) {
                Log.v(TAG, getClass().getName() + " A0.5 用户拒绝的权限有哪些: " + entry.getKey() + " : " + entry.getValue());
                deniedPermissions.put(entry.getKey(), entry.getValue());
            }
        }

        // (toat 保留) 3.3.集合如果为空, 说明全部权限都通过了 (但要注意的是如果大于10.0的版本, 在2.3.2步骤中移除了读写, 因此这里要补充上)
        if (deniedPermissions.isEmpty()) {

            // (toat 保留) [避免空申请: 如果权限全部通过就不重复申请避免弹出权限框影响生命周期] (B4) 3.3.4对读写做特殊处理
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

                // 再次对needpermissions做一次判断, 看看是否只剩下读写权限
                for (String permission : needPermissions) {
                    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.put(permission, false);
                    }
                }

                Log.v(TAG, getClass().getName() + " A0.14 (针对Android 10.0 的特殊处理) 做读写开关的判断");
                boolean containsWrite = deniedPermissions.containsKey(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                boolean containsRead = deniedPermissions.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE);

                if ((deniedPermissions.size() == 1 && (containsWrite || containsRead)) || (deniedPermissions.size() == 2 && containsWrite && containsRead)) {

                    boolean isPass = Environment.isExternalStorageManager(); // 大于Android 10.0的版本. 判断是否打开了[所有文件]开关
                    Log.v(TAG, getClass().getName() + " A0.14.2 (针对Android 10.0 的特殊处理) 此时检查读写的开关是否有打开(true: 打开): " + isPass);
                    if (isPass) {// 打开了[所有文件]开关
                        Log.i(TAG, getClass().getName() + " A0.14.3 (针对Android 10.0 的特殊处理) 用户已经打开了读写开关, 执行回调给外部 ==> allPassRunable()");
                        if (permissActionForFragment != null) permissActionForFragment.onAllGranted(); // 执行fragment的回调
                        if (permissActionForActivity != null) permissActionForActivity.onAllGranted(); // 执行activity的回调
                        RootFrag.isReloadData = RootFrag.isTmpReload; // (toat 保留: 防止无限申请权限的操作) A3. 如果权限通过后, 全部还原
                        Log.i(TAG, getClass().getName() + " A0.14.4 (针对Android 10.0 的特殊处理) 就不再走下面权限的逻辑了");
                    } else {// 没有打开[所有文件]开关
                        if (permissActionForFragment != null) {// 回调给fragment
                            Log.v(TAG, getClass().getName() + " A0.14.4.1 (针对Android 10.0 的特殊处理) 用户没有打开读写开关, 执行 ==> deniedAction(false), 此时回调给Fragment做业务处理");
                            permissActionForFragment.onDenied(NOW_WRITE_READ, new ArrayList<>(deniedPermissions.keySet()));
                        }
                        if (permissActionForActivity != null) {// 回调给Activity
                            Log.v(TAG, getClass().getName() + " A0.14.4.2 (针对Android 10.0 的特殊处理) 用户没有打开读写开关, 执行 ==> deniedAction(false), 此时回调给Activity应该弹窗提示用户了");
                            permissActionForActivity.onDenied(NOW_WRITE_READ, new ArrayList<>(deniedPermissions.keySet()));
                        }
                    }
                    return; // (toat 保留) 3.4.结束方法执行(一定要结束, 即如果识别到是大于Android 10.0的读写申请, 就不再往下判断其他权限, 直到这个读写权限处理完毕)
                }
            }


            Log.i(TAG, getClass().getName() + " A0.5 全部权限都已通过, 执行回调给外部 ==> allPassRunable()");
            if (permissActionForFragment != null) permissActionForFragment.onAllGranted(); // 执行fragment的回调
            if (permissActionForActivity != null) permissActionForActivity.onAllGranted(); // 执行activity的回调
            RootFrag.isReloadData = RootFrag.isTmpReload; // (toat 保留: 防止无限申请权限的操作) A3. 如果权限通过后, 全部还原
        } else {

            // (toat 保留) 3.4.再去判断其他权限
            for (Map.Entry<String, Boolean> entry : deniedPermissions.entrySet()) {
                String key = entry.getKey();
                Boolean value = entry.getValue();
                boolean isshould = ActivityCompat.shouldShowRequestPermissionRationale(this, key);
                Log.w(TAG, getClass().getName() + " A0.6 有权限是被拒绝的, 所以要判断一下用户是否点击了[不再询问](用户如果点击了的话, 会返回false), 当前检查" + key + "的结果是: " + isshould);
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, key)) {

                    // 此处还需要加入一个额外的判断, 就是当系统随机万一首先申请读写权限时
                    // 对于大于10.0的版本来讲, shouldShowRequestPermissionRationale 永远都是false的
                    // 所以如果遇到这种情况, 就先跳过读写权限的判断, 先识别其他权限
                    if (key.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) // 
                                || key.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Log.w(TAG, getClass().getName() + " A0.7 用户确实点击了 " + key + " [不再询问]: 但是这是读写权限, 先跳过, 继续判断其他权限");
                        continue;
                    }

                    if (permissActionForFragment != null) {// 回调给fragment
                        Log.w(TAG, getClass().getName() + " A0.7 用户确实点击了 " + key + " [不再询问]: 执行 ==> deniedAction(false), 直接返回给Fragment做业务处理, 不做其他校验了");
                        permissActionForFragment.onDenied(NOW_OTHER_FALSE, new ArrayList<>(deniedPermissions.keySet()));
                    }
                    if (permissActionForActivity != null) { // 回调给Activity
                        Log.w(TAG, getClass().getName() + " A0.7 用户确实点击了 " + key + " [不再询问]: 执行 ==> deniedAction(false), 直接返回给Activity用户先处理当前这个权限了, 不做其他校验了");
                        permissActionForActivity.onDenied(NOW_OTHER_FALSE, new ArrayList<>(deniedPermissions.keySet()));
                    }
                    return;
                }
            }

            // (toat 保留) 3.5.权限被拒绝 -- 回调给用户处理
            if (permissActionForFragment != null) {// 回调给fragment
                Log.i(TAG, getClass().getName() + " A0.8 如果以上的权限都没有被点击过[不再询问], 那么就执行 ==> deniedAction(true), 给Fragment做业务处理");
                permissActionForFragment.onDenied(NOW_OTHER_TRUE, new ArrayList<>(deniedPermissions.keySet()));
            }
            if (permissActionForActivity != null) { // 回调给Activity
                Log.i(TAG, getClass().getName() + " A0.8 如果以上的权限都没有被点击过[不再询问], 那么就执行 ==> deniedAction(true), 给用户一个机会再次申请权限");
                permissActionForActivity.onDenied(NOW_OTHER_TRUE, new ArrayList<>(deniedPermissions.keySet()));
            }
        }
    }

    /**
     * // (toat 保留) 2.2024新权限用法: 发起权限申请(外部Fragment根据业务情况调用)
     *
     * @param permissActionForFragment 全部权限通过后的回调 (由fragment传入)
     */
    public void startPermission(PermissionAction permissActionForFragment) {
        Log.i(TAG, "-------------------------------------------------------------------------------------- startPermission --------------------------------------------------------------------------------------");
        // (toat 保留) 2.保存用户允许权限后的回调
        this.permissActionForFragment = permissActionForFragment;
        // 默认需要展示权限说明
        doPermissionAction(NOW_OTHER_TRUE);
    }


    /**
     * 2024新权限用法: 执行权限操作
     *
     * @param permissionType 权限类型
     */
    private void doPermissionAction(PermissionAction.PermissionType permissionType) {
        // (toat 保留) 2.1.检查是否有权限需要申请 (防止用户自己去设置页打开)
        if (needPermissions == null || needPermissions.length == 0) {
            Log.v(TAG, getClass().getName() + " A0.1 没有权限需要申请");
            if (permissActionForFragment != null) permissActionForFragment.onAllGranted(); // 直接回调给fragment
            if (permissActionForActivity != null) permissActionForActivity.onAllGranted(); // 直接回调给Activity
            RootFrag.isReloadData = RootFrag.isTmpReload; // (toat 保留: 防止无限申请权限的操作) A3. 如果权限通过后, 全部还原
            return;
        }

        // (toat 保留) [避免空申请: 如果权限全部通过就不重复申请避免弹出权限框影响生命周期] (B2) 2.1.1.如果只剩大于10.0的权限没有申请了且用户上一次已经将状态切换为NOW_WRITE_READ, 就直接去申请
        if (permissionType == NOW_WRITE_READ) {
            Log.v(TAG, getClass().getName() + " A0.1.1 如果只剩大于10.0的权限没有申请了且用户上一次已经将状态切换为NOW_WRITE_READ, 就直接去申请");
            openManageAllFilesAccessPermissionSettings();
            return;
        }

        // (toat 保留) 2.2.用于存储未授权的权限
        List<String> deniedPermissions = new ArrayList<>();
        // (toat 保留) 2.3.检查是否有未授权的权限
        for (String permission : needPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission);
            }
        }

        // (toat 保留) [避免空申请: 如果权限全部通过就不重复申请避免弹出权限框影响生命周期] (B1) 2.3.1.对读写做特殊处理
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

            Log.v(TAG, getClass().getName() + " A0.14 (针对Android 10.0 的特殊处理) 做读写开关的判断");
            boolean containsWrite = deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean containsRead = deniedPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE);

            // 如果仅仅只有读写, 就走读写的逻辑
            if ((deniedPermissions.size() == 1 && (containsWrite || containsRead)) || (deniedPermissions.size() == 2 && containsWrite && containsRead)) {

                boolean isPass = Environment.isExternalStorageManager(); // 大于Android 10.0的版本. 判断是否打开了[所有文件]开关
                Log.v(TAG, getClass().getName() + " A0.14.2 (针对Android 10.0 的特殊处理) 此时检查读写的开关是否有打开(true: 打开): " + isPass);
                if (isPass) {// 打开了
                    Log.i(TAG, getClass().getName() + " A0.14.3 (针对Android 10.0 的特殊处理) 用户已经打开了读写开关, 执行回调给外部 ==> allPassRunable()");
                    if (permissActionForFragment != null) permissActionForFragment.onAllGranted(); // 执行fragment的回调
                    if (permissActionForActivity != null) permissActionForActivity.onAllGranted(); // 执行activity的回调
                    RootFrag.isReloadData = RootFrag.isTmpReload; // (toat 保留: 防止无限申请权限的操作) A3. 如果权限通过后, 全部还原
                    Log.i(TAG, getClass().getName() + " A0.14.4 (针对Android 10.0 的特殊处理) 就不再走下面权限的逻辑了");
                } else {// 没有打开
                    if (permissActionForFragment != null) {// 回调给fragment
                        Log.v(TAG, getClass().getName() + " A0.14.4.1 (针对Android 10.0 的特殊处理) 用户没有打开读写开关, 执行 ==> deniedAction(false), 此时回调给Fragment做业务处理");
                        permissActionForFragment.onDenied(NOW_WRITE_READ, deniedPermissions);
                    }
                    if (permissActionForActivity != null) {// 回调给Activity
                        Log.v(TAG, getClass().getName() + " A0.14.4.2 (针对Android 10.0 的特殊处理) 用户没有打开读写开关, 执行 ==> deniedAction(false), 此时回调给Activity应该弹窗提示用户了");
                        permissActionForActivity.onDenied(NOW_WRITE_READ, deniedPermissions);
                    }
                }
                return; // (toat 保留) 3.4.结束方法执行(一定要结束, 即如果识别到是大于Android 10.0的读写申请, 就不再往下判断其他权限, 直到这个读写权限处理完毕)
            } else {// 如果集合中不仅仅只有读写权限, 那么就移除读写权限, 先去判断其他权限
                // (toat 保留) ) [避免空申请: 如果权限全部通过就不重复申请避免弹出权限框影响生命周期] (B3) 2.3.2.如果不止一个权限, 那么就先把读写权限去掉, 去判断其他权限
                deniedPermissions.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                deniedPermissions.remove(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // (toat 保留) 2.4.如果存在未授权的权限 (排除Android10.0以上的读写权限)，申请权限
        if (!deniedPermissions.isEmpty()) {
            Log.v(TAG, getClass().getName() + " A0.2 哪些没有权限: " + deniedPermissions);

            // (toat 保留) 2.5.申请其他权限
            if (permissionType == NOW_OTHER_TRUE) {
                // 首次默认去申请
                Log.v(TAG, getClass().getName() + " A0.11 此时如果是第一次页面发起申请 或者 用户在之前的行为没有连续拒绝或者点击[不再询问], 那就再次对所有权限发起申请");
                permissinLauncher.launch(deniedPermissions.toArray(new String[0]));
            } else if (permissionType == NOW_WRITE_READ) {
                // 对于大于10.0的版本来讲, 只有直接跳转到[打开所有文件]设置页
                Log.v(TAG, getClass().getName() + " A0.11 此时如果是用户点击了弹窗且识别到是大于Android 10.0的读写类型, 就直接跳转到[打开所有文件]设置页");
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) openManageAllFilesAccessPermissionSettings();
            } else {
                // 如果用户点击了[不再询问], 那么就引导用户去设置页打开权限
                Log.v(TAG, getClass().getName() + " A0.12 此时如果是用户点击了弹框并且用户在之前的行为连续拒绝或者点击[不再询问], 那就引导用户去设置页打开权限");
                openAppSettingsPage();
                Log.v(TAG, getClass().getName() + " A0.13 这时候页面跳转到应用详情页了");
            }

        } else {
            // (toat 保留) 2.6.如果权限都被授权了
            Log.v(TAG, getClass().getName() + " A0.3 已经有所有请求的权限, 执行回调给外部 ==> allPassRunable()");
            // (toat 保留) 2.7.抛出给外部继续做业务逻辑
            if (permissActionForFragment != null) permissActionForFragment.onAllGranted(); // 直接回调给fragment
            if (permissActionForActivity != null) permissActionForActivity.onAllGranted(); // 直接回调给Activity
            RootFrag.isReloadData = RootFrag.isTmpReload; // (toat 保留: 防止无限申请权限的操作) A3. 如果权限通过后, 全部还原
        }
    }

    /**
     * 2024新权限用法: 跳转到系统设置页面(外部调用)
     */
    public void toSetting(PermissionAction.PermissionType permissionType) {
        Log.v(TAG, getClass().getName() + " A0.9 用户这时点击弹窗了");
        // (toat 保留) 再次发起权限申请, 将用户引导到设置页(如果检测到有权限是[不再询问]的话)
        Log.v(TAG, getClass().getName() + " A0.10 这是看下之前回调给用户的是什么值: " + permissionType);
        doPermissionAction(permissionType);
    }


    /**
     * 2024新权限用法: 跳转到[所有文件]设置页
     */
    private void openManageAllFilesAccessPermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        // startActivity(intent);
        settingLauncher.launch(intent);
        // (toat 保留: 防止无限申请权限的操作) A4. 不管触发了哪类设置页的跳转, 都结束当前APP, 为下次的权限检测做准备
        killAllActivitys();
    }

    /**
     * 2024新权限用法: 跳转到应用设置页面
     */
    private void openAppSettingsPage() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(intent);
        settingLauncher.launch(intent);
        // (toat 保留: 防止无限申请权限的操作) A4. 不管触发了哪类设置页的跳转, 都结束当前APP, 为下次的权限检测做准备
        killAllActivitys();
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 前置操作)
        beforeAllFirst();
        // (toat 保留): 1.2024新权限用法, 接收外部的权限申请
        ApplyPermissionBean apb = applyPermission();
        if (apb != null) {
            needPermissions = apb.getPermissions();
            permissActionForActivity = apb.getAction();
        } else {
            Lgg.t(TAG).ii("目前没有权限申请");
        }
        // 0.检测action与category是否符合规范
        boolean isActionCategoryMatch = checkActionCategory();
        if (isActionCategoryMatch) {// 0.1.符合条件则正常执行

            if (checkStardard()) {// 0.2.standard配置符合
                Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":onCreate()");
                // 1.获取初始化配置对象
                rootProperty = initProperty();
                if (rootProperty != null) {// 属性对象不为空
                    // 2.分发配置
                    dispatherProperty(rootProperty);
                    // 3.设置无标题栏(必须位于 super.onCreate(savedInstanceState) 之上)
                    if (isFullScreen) {
                        requestWindowFeature(Window.FEATURE_NO_TITLE);
                    }

                    // 4.android 运行版本在 [android 9.0 P] 以下才做规范判断
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        // 4.1.包名检查
                        if (!packageCheck(packageName)) {
                            toast(R.string.PACKAGE_NAME_NOT_MATCH, 5000);
                            return;
                        }

                        // 4.2.子线程 -- lint检查开发规范
                        new Thread(() -> {
                            List<Integer> lintCodes = lintCheck(packageName);
                            if (lintCodes.size() > 0 && isLintCheck) {
                                runOnUiThread(this::killAllActivitys);
                                Intent intent = new Intent(this, LintActivity.class);
                                intent.putIntegerArrayListExtra(LintHelper.class.getSimpleName(), (ArrayList<Integer>) lintCodes);
                                startActivity(intent);
                            }
                        }).start();
                    }

                    // 5.填充视图
                    setContentView(getHiberView(layoutId, gradientBean));
                    // 8.处理从其他组件传递过来的数据
                    handleIntentExtra(getIntent());
                    // 9.视图填充完毕
                    onCreateFinish(layoutId);
                    // 10.注册Activity的Eventbus
                    EventBus.getDefault().register(this);

                } else {// 属性对象为空
                    String proErr = getString(R.string.ROOT_PROPERTY_ERR);
                    toast(proErr, 5000);
                    Lgg.t(TAG).ee(proErr);
                }

            } else {// 没有配置standard
                String err = getString(R.string.STANDARD_TIP);
                toast(err, 5000);
                Lgg.t(TAG).ee(err);
            }

        } else {// 0.1.开发人员没有按照规定配置manifest
            String err = getString(R.string.INIT_ERR);
            toast(err, 5000);
            Lgg.t(TAG).ee(err);
        }
    }

    /**
     * 创建 statubarView
     *
     * @param drawable 自定义背景
     * @return statubarView
     */
    private View createStatubarView(Drawable drawable) {
        // 获取状态栏高度
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statubarHeight = getResources().getDimensionPixelSize(resourceId);
        // 创建一个imageview
        ImageView statubarView = new ImageView(this);
        ViewGroup.LayoutParams vp = new ViewGroup.LayoutParams(-1, statubarHeight);
        statubarView.setLayoutParams(vp);
        // 设置背景
        statubarView.setBackground(drawable);
        // 设置ID
        statubarView.setId(new Random().nextInt());
        return statubarView;
    }

    /**
     * 获取自定义状态栏
     *
     * @param gradientBean 渐变对象
     * @return 自定义状态栏
     */
    private View getStausbarView(GradientBean gradientBean) {
        // 准备一个空状态栏容器
        View statubarView = null;
        // 隐藏原来状态栏 并 生成自定义状态栏
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        // 外部指定渐变
        if (gradientBean != null) {
            // 自定义drawable - drawable优先
            Drawable drawable = gradientBean.getDrawable();
            if (drawable != null) {
                statubarView = createStatubarView(drawable);
            } else {// 渐变
                GradientDrawable.Orientation orientation = gradientBean.getOrientation();
                int[] colors = gradientBean.getColors();
                if (orientation == null || colors == null || colors.length <= 0) {// 渐变模式未赋值
                    toast(R.string.GRADIENT_COLOR_NOT_SET, 5000);
                } else {
                    // 生成渐变对象
                    GradientDrawable gradientDrawable = new GradientDrawable();
                    int[] colorInts = new int[colors.length];
                    for (int i = 0; i < colors.length; i++) colorInts[i] = ContextCompat.getColor(this, colors[i]);
                    gradientDrawable.setColors(colorInts);
                    gradientDrawable.setOrientation(orientation);
                    gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    // 填充
                    statubarView = createStatubarView(gradientDrawable);
                }
            }
        } else {
            // 7.设置状态栏颜色 (单一色)
            statubarView = createStatubarView(new ColorDrawable(ContextCompat.getColor(this, colorStatusBar)));
        }
        // 设置透明度
        if (statubarView != null) statubarView.setAlpha(statusbarAlpha);
        return statubarView;
    }

    /**
     * 生成带吐司的布局
     *
     * @param layoutId 布局ID
     * @return 复合布局
     */
    private View getHiberView(int layoutId, GradientBean gradientBean) {
        // 获取到一个自定义状态栏
        View stausbarView = getStausbarView(gradientBean);
        // 生成顶级布局
        RelativeLayout hiberRelative = new RelativeLayout(this);
        RelativeLayout.LayoutParams vp = new RelativeLayout.LayoutParams(-1, -1);
        hiberRelative.setLayoutParams(vp);
        // 填充外部布局
        View inflate = View.inflate(this, layoutId, null);
        RelativeLayout.LayoutParams vlp = new RelativeLayout.LayoutParams(-1, -1);
        inflate.setLayoutParams(vlp);
        hiberRelative.addView(inflate);
        // 添加标题栏
        if (stausbarView != null) hiberRelative.addView(stausbarView);
        // 非沉浸式 - 重新排序位置 (让业务视图 below Statubar)
        if (stausbarView != null & !immerse) {
            ((RelativeLayout.LayoutParams) inflate.getLayoutParams()).addRule(RelativeLayout.BELOW, stausbarView.getId());
            inflate.setLayoutParams(vlp);
        }
        // 新建吐司文本
        tvToast = new TextView(this);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(-2, -2);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rlp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        rlp.setMarginStart(15);
        rlp.setMarginEnd(15);
        rlp.bottomMargin = 60;
        tvToast.setLayoutParams(rlp);
        int px = 8;
        tvToast.setPadding(px, px, px, px);
        tvToast.setTextColor(Color.WHITE);
        tvToast.setBackgroundResource(R.drawable.corner2);
        tvToast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvToast.setAlpha(0);
        hiberRelative.addView(tvToast);
        return hiberRelative;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Method--> " + getClass().getSimpleName() + ":onResume()");
        // 视图结构树加载并显示到屏幕后 - 回调 - 一般用于获取控件大小
        Looper.myQueue().addIdleHandler(new IdleHandlerImpl());
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Lgg.t(TAG).ww("Method--> " + getClass().getSimpleName() + ":onPause()");
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearEvents();
    }

    /**
     * 接收自定义的Eventbus数据
     *
     * @param object 多元化数据
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onRootEvent(Object object) {
        if (!(object instanceof FragBean)) {
            Log.i(TAG, "onRootEvent: 过滤出非FragBean类型的数据");
            getDataHandler(object);
        }
    }

    /**
     * 接收到Eventbus之后的处理
     *
     * @param object 泛型对象
     */
    protected void getDataHandler(Object object) {
        // 1.检测集合是否为空
        if (eventClazzs != null & eventListeners != null) {
            // 2.检测用户是否需要监听回调
            if (eventClazzs.size() > 0 & eventListeners.size() > 0) {
                for (int i = 0; i < eventClazzs.size(); i++) {
                    // 3.获取到外部指定的泛型
                    Class eventClazz = eventClazzs.get(i);
                    // 4.判断Eventbus接收到的数据是否符合用户传递的类型
                    if (eventClazz.isAssignableFrom(object.getClass())) {
                        // 5.获取监听器
                        RootEventListener listener = eventListeners.get(i);
                        if (listener != null) {
                            // 6.用户是否指定了［仅仅作用域当前界面］的条件
                            if (listener.isCurrentPageEffectOnly()) {/* 指定当前页面才起作用 */
                                // 6.1.检测［当前屏幕的页面］是否等于［接收数据的页面］
                                if (ActivityHelper.isTopActivity(this, getClass().getName())) {
                                    // 6.2.移除stick包
                                    EventBus.getDefault().removeStickyEvent(object);
                                    // 6.3.将数据回调到外部
                                    listener.getData(object);
                                }
                            } else {/* 不指定当前页面才起作用--> 即全部注册的页面均发生作用 */
                                EventBus.getDefault().removeStickyEvent(object);
                                listener.getData(object);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 视图填充完毕
     *
     * @param layoutId 视图ID
     */
    public void onCreateFinish(int layoutId) {

    }

    /**
     * 包名检查
     *
     * @param packageName 包名
     * @return T: 通过
     */
    private boolean packageCheck(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            toast(getString(R.string.PACKAGE_NAME_NOT_SET), 5000);
            return false;
        }
        // 此处不能使用getPackageName(), 因为在其他module下, 使用getPackageName()获取到的包名永远都是application的包名
        if (!Objects.requireNonNull(getClass().getPackage()).getName().startsWith(packageName)) {
            toast(getString(R.string.PACKAGE_NAME_NOT_MATCH), 5000);
            return false;
        }
        return true;
    }

    /**
     * lint检查归法
     *
     * @param packageName 包名
     */
    private List<Integer> lintCheck(String packageName) {
        LintHelper.extendActivity = RootMAActivity.class;
        LintHelper.extendFragment = RootFrag.class;
        return LintHelper.getLintResult(packageName);
    }

    /**
     * 处理从其他组件传递过来的数据
     *
     * @param intent 意图
     */
    private void handleIntentExtra(Intent intent) {

        // 0.获取到序列
        Serializable extra = intent.getSerializableExtra(INTENT_NAME);
        // 0.1.序列为空(启动APP初始化时)
        if (extra == null) {
            // 0.2.初始化第一个
            initFragment(0, "");

        } else {
            // 0.2.判断开发是否传递错误参数
            boolean isSkipbeanType = extra instanceof SkipBean;
            if (!isSkipbeanType) {// 不是跳转过来的skipbean
                // 如果是推送过来的, 一定是字节
                if (extra instanceof byte[]) {
                    SkipBean skipBean = getBeanPassByte(extra);
                    if (skipBean != null) {
                        hadData(skipBean);
                    }

                } else {// 未知数据
                    toast(getString(R.string.SKIPBEAN_TIP), 5000);
                }
            } else {
                // 1.正常获取到skipbean并检测attach是否实现了序列化
                SkipBean skipBean = (SkipBean) extra;
                // 2.当推送过来或者跳转过来时, 处理数据
                hadData(skipBean);
            }
        }
    }

    /**
     * 当推送过来或者跳转过来时, 处理数据
     *
     * @param skipBean 数据
     */
    private void hadData(SkipBean skipBean) {
        // 判断是否为自身AC
        String currentActivityClassName = getClass().getName();
        String targetActivityClassName = skipBean.getTargetActivityClassName();
        if (targetActivityClassName.equalsIgnoreCase(currentActivityClassName)) {
            // 是自身AC
            Class targetFragClass = searchFragClassByName(skipBean.getTargetFragmentClassName());
            int classFragIndex = searchFragIndexByClass(targetFragClass);
            Object attach = skipBean.getAttach();
            initFragment(classFragIndex, attach);
        } else {
            // 不是自身AC(推送)
            Activity activity = this;
            boolean isSingleTop = false;
            boolean isFinish = false;
            boolean isOverridePending = false;
            int delay = 0;
            RootHelper.toActivityImplicit(activity, targetActivityClassName, isSingleTop, isFinish, isOverridePending, delay, skipBean);
        }
    }

    /**
     * 如果是通过推送过来的(推送是BYTE形式),此时序列化必定为空, 则进行byte[]转换
     * 为了适配某些手机从pendingIntent传递过来的数据有可能为字节类型
     *
     * @param extra 数据对象
     */
    private SkipBean getBeanPassByte(Serializable extra) {
        // 获取到传递过来的字节
        byte[] b_skipbean = (byte[]) extra;
        // 判断空值
        if (b_skipbean != null && b_skipbean.length > 0) {
            String str = new String(b_skipbean);
            SkipBean skipBean = JSONObject.parseObject(str, SkipBean.class);
            // 处理附件, 把jsonobject转换成object
            Object jsonObj = skipBean.getAttach();
            if (jsonObj instanceof JSONObject) {
                String attachJson = JSONObject.toJSONString(jsonObj);
                Object attachObj = JSONObject.parseObject(attachJson, skipBean.getAttachClass());
                skipBean.setAttach(attachObj);
            }
            return skipBean;
        }
        return null;
    }

    /**
     * 检查开发人员是否配置了action以及category, action是否符合规范(绝对路径)
     *
     * @return T:符合
     */
    @SuppressLint("PrivateApi") // 消除「packageManager.getClass().getDeclaredMethod」的警告
    @SuppressWarnings("unchecked")// 消除「method.invoke(packageManager, getPackageName()」的警告
    private boolean checkActionCategory() {

        // 如果是小于Android 6.0或者大于android 8.0, 则不能使用反射, PackageManager没有对应的API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M | Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String builder = "根据Android规定, 在Android >= P之后不能使用反射机制获取IntentFilters" + "\n" + "如使用框架中出现异常, 请根据开发文档自行检测Manifest文件中Action以及Category是否符合开发规范";
            Lgg.t(TAG).ww(builder);
            return true;
        }

        // >= 6.0 , 使用反射调用PackageManager的获取全部intent-filter
        try {
            PackageManager packageManager = getPackageManager();
            Method method = packageManager.getClass().getDeclaredMethod("getAllIntentFilters", String.class);
            method.setAccessible(true);
            List<IntentFilter> intentFilters = (List<IntentFilter>) method.invoke(packageManager, getPackageName());
            for (IntentFilter intentFilter : intentFilters) {
                // 检测<action>中是否配置自身类的绝对路径--> 必须强制要求外部人员在action里配置的是自身AC的绝对路径
                boolean isSetActionBySelfName = intentFilter.hasAction(getClass().getName());
                // 检测<category>是否配置DEFAULT标签
                boolean isSetCategoryByDefault = intentFilter.hasCategory("android.intent.category.DEFAULT");
                // <action> 与 <category>必须同时符合条件
                if (isSetActionBySelfName & isSetCategoryByDefault) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String inflectErr = getString(R.string.INFLECT_ERR);
            Lgg.t(Cons.TAG).ee(inflectErr);
        }
        return false;
    }

    /**
     * @return 检查是否配置了stardard
     */
    private boolean checkStardard() {
        ComponentName componentName = getComponentName();
        ActivityInfo activityInfo;
        try {
            activityInfo = getPackageManager().getActivityInfo(componentName, PackageManager.GET_META_DATA);
            int launchMode = activityInfo.launchMode;
            if (launchMode != ActivityInfo.LAUNCH_MULTIPLE) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String singleTaskErr = getString(R.string.STANDARD_ERR);
            Lgg.t(Cons.TAG).ee(singleTaskErr);
            toast(singleTaskErr, 5000);
        }
        return true;
    }

    /**
     * 根据frag的绝对路径获取到对应的frag字节码
     *
     * @param targetFragmentClassName fragment的绝对路径
     * @return frag字节码
     */
    private Class searchFragClassByName(String targetFragmentClassName) {
        if (!TextUtils.isEmpty(targetFragmentClassName)) {
            for (String fragName : classFragMap.keySet()) {
                if (fragName.equalsIgnoreCase(targetFragmentClassName)) {
                    return classFragMap.get(fragName);
                }
            }
        }
        return fragmentClazzs[0];
    }

    /**
     * 根据frag字节码找到该字节码在数组中的索引
     *
     * @param currentClass frag字节码
     * @return 对应的索引
     */
    private int searchFragIndexByClass(Class currentClass) {
        for (int i = 0; i < fragmentClazzs.length; i++) {
            if (currentClass == fragmentClazzs[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 国际化语言必须实现的方法
     *
     * @param context 上下文
     */
    @Override
    protected void attachBaseContext(Context context) {
        // 国际化语言切换时必须使用以下方式 (需要把context与语言配置进行绑定)
        super.attachBaseContext(LangHelper.getContext(context));
    }

    /**
     * 分发配置
     *
     * @param rootProperty 配置
     */
    private void dispatherProperty(RootProperty rootProperty) {
        // 初始化赋值
        Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":dispatherProperty()");
        Lgg.t(TAG).vv(rootProperty.toString());
        isFullScreen = rootProperty.isFullScreen();
        colorStatusBar = rootProperty.getColorStatusBar() <= 0 ? colorStatusBar : rootProperty.getColorStatusBar();
        gradientBean = rootProperty.getGradientStatusBar();
        immerse = rootProperty.isStatusbarImmerse();
        statusbarAlpha = rootProperty.getStatusbarAlpha();
        layoutId = rootProperty.getLayoutId() <= 0 ? layoutId : rootProperty.getLayoutId();
        TAG = TextUtils.isEmpty(rootProperty.getTAG()) ? TAG : rootProperty.getTAG();
        isSaveInstanceState = rootProperty.isSaveInstanceState();
        projectDirName = TextUtils.isEmpty(rootProperty.getProjectDirName()) ? projectDirName : rootProperty.getProjectDirName();
        containId = rootProperty.getContainId() <= 0 ? containId : rootProperty.getContainId();
        packageName = rootProperty.getPackageName();
        fragmentClazzs = rootProperty.getFragmentClazzs() == null || rootProperty.getFragmentClazzs().length <= 0 ? fragmentClazzs : rootProperty.getFragmentClazzs();
        // 对fragmentClazzs做二次处理--> 将权限fragment加载进集合中
        fragmentClazzs = putInnerFragmentIn();
        // 将fragment转换成map形式
        saveClassMap(fragmentClazzs);
    }

    /**
     * 把Permissfragment添加进集合
     *
     * @return 新集合
     */
    private Class[] putInnerFragmentIn() {
        List<Class> tempList = new ArrayList<>(Arrays.asList(fragmentClazzs));
        tempList.add(PermissFragment.class);
        Class[] newFrags = new Class[tempList.size()];
        for (int i = 0; i < tempList.size(); i++) {
            newFrags[i] = tempList.get(i);
        }
        return newFrags;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        /*
         * 重写这个方法是为了解决: 用户点击权限允许后界面无法继续加载的bug.
         * NoSaveInstanceStateActivityName(): 一般由第一个Activity进行实现
         * onSaveInstanceState()方法在获取权限时, 导致fragment初始化失败
         * 如果当前的Activity没有必要保存状态 (默认是: Activity被后台杀死后,系统会保存Activity状态)
         * 则不需要调用 「super.onSaveInstanceState(outState)」这个方法
         */
        Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":onSaveInstanceState() == " + isSaveInstanceState);
        if (isSaveInstanceState) {
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onBackPressed() {
        if (!BackHandlerHelper.handleBackPress(this)) {
            boolean isDispatcher = onBackClick();
            if (!isDispatcher) {
                // 如果fragment没有处理--> 则直接退出
                super.onBackPressed();
            }
        }
    }

    /**
     * 初始化fragment
     */
    private void initFragment(int initIndex, Object attach) {
        Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":initFragment()");
        // 容器
        int contain = containId;
        Class firstFrag = fragmentClazzs[initIndex];
        // 初始化fragment调度器
        initFragmentSchedule(contain, firstFrag, attach);
    }

    /**
     * 初始化frahelper单例
     *
     * @param contain   容器
     * @param firstFrag 首屏
     */
    private void initFragmentSchedule(int contain, Class firstFrag, Object attach) {
        Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":initFragmentSchedule()");
        if (fraHelpers == null) {
            synchronized (FraHelpers.class) {
                if (fraHelpers == null) {
                    fraHelpers = new FraHelpers(this, fragmentClazzs, firstFrag, contain, attach);
                    Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":new FraHelpers()");
                }
            }
        } else {
            Lgg.t(TAG).vv("Method--> " + getClass().getSimpleName() + ":FraHelpers.getInstance()");
            onNexts();
        }
    }

    /**
     * 将字节码list转换为hashmap
     *
     * @param fragmentClazzs 需要转换的集合
     */
    private void saveClassMap(Class[] fragmentClazzs) {
        for (Class clz : fragmentClazzs) {
            String clzAbsoluteName = clz.getName();
            classFragMap.put(clzAbsoluteName, clz);
        }
    }

    /**
     * 封装传输对象
     *
     * @param classWhichFragmentStart 哪个fragment跳转的
     * @param targetFragmentClass     跳转到哪个目标Fragment
     * @param attach                  附件
     * @return 传输对象
     */
    private FragBean transferFragbean(Class classWhichFragmentStart, Class targetFragmentClass, Object attach) {
        // 1.创建一个新的传输对象
        FragBean fragBean = new FragBean();
        // 2.检测传递进来的参数是否为fragment类型
        boolean whichIsFragment = Fragment.class.isAssignableFrom(classWhichFragmentStart);
        boolean targetIsFragment = Fragment.class.isAssignableFrom(targetFragmentClass);
        if (whichIsFragment & targetIsFragment) {// 同时符合条件
            fragBean.setCurrentFragmentClass(classWhichFragmentStart);
            fragBean.setTargetFragmentClass(targetFragmentClass);

        } else if (whichIsFragment & !targetIsFragment) {// target不符合条件--> 使用which填充
            fragBean.setCurrentFragmentClass(classWhichFragmentStart);
            fragBean.setTargetFragmentClass(classWhichFragmentStart);

        } else if (!whichIsFragment & targetIsFragment) {// which不符合条件--> 使用target填充
            fragBean.setCurrentFragmentClass(targetFragmentClass);
            fragBean.setTargetFragmentClass(targetFragmentClass);

        } else {// 两个同时不为true--> 则默认跳转第一个
            fragBean.setCurrentFragmentClass(fragmentClazzs[0]);
            fragBean.setTargetFragmentClass(fragmentClazzs[0]);
        }
        // 3.设置附件
        fragBean.setAttach(attach == null ? "" : attach);
        // 4.返回封装后对象
        return fragBean;
    }

    /**
     * 封装skipbean
     *
     * @param current        当前的fragment
     * @param activityClass  目标Activity的action
     * @param target         目标Fragment
     * @param attach         附件
     * @param isTargetReload 是否重载对象Fragment
     * @param isFinish       是否结束当前Activity
     * @return skipbean
     */
    private SkipBean getSkipbean(Class current, Class activityClass, Class target, Object attach, boolean isTargetReload, boolean isFinish) {
        SkipBean skipBean = new SkipBean();

        boolean isCurrentFrag = Fragment.class.isAssignableFrom(current);
        boolean isTargetFrag = Fragment.class.isAssignableFrom(target);
        if (isCurrentFrag & isTargetFrag) {
            skipBean.setCurrentFragmentClassName(current.getName());
            skipBean.setTargetFragmentClassName(target.getName());

        } else if (isCurrentFrag & !isTargetFrag) {
            skipBean.setCurrentFragmentClassName(current.getName());
            skipBean.setTargetFragmentClassName(current.getName());

        } else if (!isCurrentFrag & isTargetFrag) {
            skipBean.setCurrentFragmentClassName(getClass().getName());
            skipBean.setTargetFragmentClassName(target.getName());

        } else {
            skipBean.setCurrentFragmentClassName(getClass().getName());
            skipBean.setTargetFragmentClassName(getClass().getName());

        }

        skipBean.setTargetActivityClassName(activityClass.getName());
        skipBean.setAttach(attach);
        skipBean.setTargetReload(isTargetReload);
        skipBean.setCurrentACFinish(isFinish);
        return skipBean;
    }

    /**
     * 清空关于Eventbus的集合
     */
    private void clearEvents() {
        eventClazzs.clear();
        eventListeners.clear();
        eventListeners = null;
        eventClazzs = null;
    }

    /**
     * 移除指定的fragment
     *
     * @param needkills 指定的fragment
     */
    private void killWhich(Class... needkills) {
        if (needkills.length > 0 & fraHelpers != null) {
            for (Class needkill : needkills) {
                if (Fragment.class.isAssignableFrom(needkill)) {
                    fraHelpers.remove(needkill);
                }
            }
        }
    }

    /**
     * 追踪流程(Fragment)
     *
     * @param classWhichFragmentStart 由哪个界面跳转
     * @param targetFragmentClass     跳到哪里去
     * @param attach                  什么类型附件
     * @param isTargetReload          是否重置目标
     * @param needkills               跳转后被杀死的fragment
     */
    private void trackFragment(Class classWhichFragmentStart, Class targetFragmentClass, Object attach, boolean isTargetReload, Class... needkills) {
        if (Lgg.LOG_FLAG == Lgg.SHOW_ALL) {
            StringBuilder builder = new StringBuilder();
            builder.append("RootLog\n");
            builder.append("------------------------------ 跳转流程 ------------------------------").append("\n");
            builder.append("----- from: ").append(classWhichFragmentStart.getSimpleName()).append("\n");
            builder.append("----- to: ").append(targetFragmentClass.getSimpleName()).append("\n");
            builder.append("----- attach: ").append(attach != null ? attach.getClass().getSimpleName() : "Null").append("\n");
            builder.append("----- isTargetReload: ").append(isTargetReload).append("\n");
            builder.append("----- kill: ").append(needkills != null && needkills.length > 0 ? needkills[0].getSimpleName() : "Null").append("\n");
            builder.append("----------------------------------------------------------------------");
            builder.append("\n");
            Lgg.w(TRACK, builder.toString());
            Lgg.w(TAG, builder.toString());
        }
    }

    /**
     * 追踪流程(Activity)
     *
     * @param current        由哪个界面跳转
     * @param targetAC       跳转到哪个Activity
     * @param target         跳到哪里去
     * @param attach         什么类型附件
     * @param isTargetReload 是否重置目标
     * @param needKills      跳转后被杀死的fragment
     */
    private void trackActivity(Class current, Class targetAC, Class target, Object attach, boolean isTargetReload, Class... needKills) {
        if (Lgg.LOG_FLAG == Lgg.SHOW_ALL) {
            StringBuilder builder = new StringBuilder();
            builder.append("RootLog\n");
            builder.append("------------------------------ 跳转流程 ------------------------------").append("\n");
            builder.append("----- from: ").append(current.getSimpleName()).append("\n");
            builder.append("----- targetAc: ").append(targetAC.getSimpleName()).append("\n");
            builder.append("----- to: ").append(target.getSimpleName()).append("\n");
            builder.append("----- attach: ").append(attach != null ? attach.getClass().getSimpleName() : "Null").append("\n");
            builder.append("----- isTargetReload: ").append(isTargetReload).append("\n");
            builder.append("----- kill: ").append(needKills != null && needKills.length > 0 ? needKills[0].getSimpleName() : "Null").append("\n");
            builder.append("----------------------------------------------------------------------");
            builder.append("RootLog\n");
            Lgg.w(TRACK, builder.toString());
            Lgg.w(TAG, builder.toString());
        }
    }

    /**
     * 追踪流程(Module)
     *
     * @param current        由哪个界面跳转
     * @param activityClass  跳转到哪个Activity
     * @param target         跳到哪里去
     * @param attach         什么类型附件
     * @param isTargetReload 是否重置目标
     * @param needKills      跳转后被杀死的fragment
     */
    private void trackModule(Class current, String activityClass, String target, Object attach, boolean isTargetReload, Class... needKills) {
        if (Lgg.LOG_FLAG == Lgg.SHOW_ALL) {
            StringBuilder builder = new StringBuilder();
            builder.append("RootLog\n");
            builder.append("------------------------------ 跳转流程 ------------------------------").append("\n");
            builder.append("----- from: ").append(current.getSimpleName()).append("\n");
            builder.append("----- targetAc: ").append(activityClass).append("\n");
            builder.append("----- to: ").append(target).append("\n");
            builder.append("----- attach: ").append(attach != null ? attach.getClass().getSimpleName() : "Null").append("\n");
            builder.append("----- isTargetReload: ").append(isTargetReload).append("\n");
            builder.append("----- kill: ").append(needKills != null && needKills.length > 0 ? needKills[0].getSimpleName() : "Null").append("\n");
            builder.append("----------------------------------------------------------------------");
            builder.append("\n");
            Lgg.w(TRACK, builder.toString());
            Lgg.w(TAG, builder.toString());
        }
    }

    /* -------------------------------------------- public method -------------------------------------------- */

    /**
     * 同module + 同Activity: 普通跳转
     *
     * @param classWhichFragmentStart 当前
     * @param targetFragmentClass     目标
     * @param attach                  额外附带数据对象
     * @param isTargetReload          是否重载视图
     * @param needkills               跳转前需要杀死哪些fragment
     */
    public void toFrag(Class classWhichFragmentStart, Class targetFragmentClass, Object attach, boolean isTargetReload, Class... needkills) {
        handler.postDelayed(() -> {
            // 检测传输目标是否为空
            if (classWhichFragmentStart == null | targetFragmentClass == null) {
                toast(getString(R.string.NULL_TIP), 5000);
                return;
            }
            // 检测附件是否实现序列化
            if (attach != null) {
                if (!(attach instanceof Serializable)) {
                    String string = getString(R.string.ATTACH_NOT_SERILIZABLE);
                    String format = String.format(string, attach.getClass().getSimpleName());
                    toast(format, 5000);
                    return;
                }
            }
            RootFrag.whichFragmentStart = classWhichFragmentStart.getSimpleName();
            // 0.转换并封装传输对象
            FragBean fragBean = transferFragbean(classWhichFragmentStart, targetFragmentClass, attach);
            // 1.再传输(否则会出现nullPointException)
            EventBus.getDefault().removeStickyEvent(FragBean.class);
            // 2.先跳转
            fraHelpers.transfer(fragBean.getTargetFragmentClass(), isTargetReload, needkills);
            EventBus.getDefault().postSticky(fragBean);
            // 3.追踪流程
            trackFragment(classWhichFragmentStart, targetFragmentClass, attach, isTargetReload, needkills);
        }, 0);
    }

    /**
     * 同module + 同Activity: 普通跳转(延迟)
     *
     * @param current        当前
     * @param target         目标
     * @param attach         附带
     * @param isTargetReload 是否重载视图
     * @param delayMilis     延迟毫秒数
     * @param needkills      跳转前需要杀死哪些fragment
     */
    public void toFrag(Class current, Class target, Object attach, boolean isTargetReload, int delayMilis, Class... needkills) {
        Thread ta = new Thread(() -> {
            try {
                Thread.sleep(delayMilis);
                runOnUiThread(() -> handler.postDelayed(() -> toFrag(current, target, attach, isTargetReload, needkills), 0));
            } catch (InterruptedException e) {
                e.printStackTrace();
                Lgg.t(Cons.TAG).ee("RootMAActivity error: " + e.getMessage());
            }
        });
        ta.start();
    }

    /**
     * 同module + 不同Activity 跳转
     *
     * @param current        当前的fragment
     * @param targetAC       目标Activity
     * @param target         目标fragment
     * @param attach         附件
     * @param isTargetReload 是否重载目标fragment
     * @param needKills      跳转前需要杀死哪些fragmentF
     */
    public void toFragActivity(Class current, Class targetAC, Class target, Object attach, boolean isTargetReload, Class... needKills) {
        handler.postDelayed(() -> {
            // 检测传输目标是否为空
            if (current == null | targetAC == null | target == null) {
                toast(getString(R.string.NULL_TIP), 5000);
                return;
            }
            // 检测附件是否实现序列化
            if (attach != null) {
                if (!(attach instanceof Serializable)) {
                    String string = getString(R.string.ATTACH_NOT_SERILIZABLE);
                    String format = String.format(string, attach.getClass().getSimpleName());
                    toast(format, 5000);
                    return;
                }
            }
            SkipBean skipbean = getSkipbean(current, targetAC, target, attach, isTargetReload, false);
            RootHelper.toActivityImplicit(this, skipbean.getTargetActivityClassName(), false, false, false, 0, skipbean);
            if (needKills.length > 0) {
                killWhich(needKills);
            }
            // 追踪流程
            trackActivity(current, targetAC, target, attach, isTargetReload, needKills);
        }, 0);
    }

    /**
     * 同module + 不同Activity 跳转(带延时和自定义结束当前)
     *
     * @param current        当前的fragment
     * @param targetAC       目标Activity
     * @param target         目标fragment
     * @param attach         附件
     * @param isTargetReload 是否重载目标fragment
     * @param isFinish       是否结束当前AC
     * @param delay          延迟毫秒数
     * @param needKills      跳转前需要杀死哪些fragment
     */
    public void toFragActivity(Class current, Class targetAC, Class target, Object attach, boolean isTargetReload, boolean isFinish, int delay, Class... needKills) {
        handler.postDelayed(() -> {
            // 检测传输目标是否为空
            if (current == null | targetAC == null | target == null) {
                toast(getString(R.string.NULL_TIP), 5000);
                return;
            }
            // 检测附件是否实现序列化
            if (attach != null) {
                if (!(attach instanceof Serializable)) {
                    String string = getString(R.string.ATTACH_NOT_SERILIZABLE);
                    String format = String.format(string, attach.getClass().getSimpleName());
                    toast(format, 5000);
                    return;
                }
            }
            SkipBean skipbean = getSkipbean(current, targetAC, target, attach, isTargetReload, isFinish);
            RootHelper.toActivityImplicit(this, skipbean.getTargetActivityClassName(), false, isFinish, false, delay, skipbean);
            if (needKills.length > 0) {
                killWhich(needKills);
            }
            // 追踪流程
            trackActivity(current, targetAC, target, attach, isTargetReload, needKills);
        }, 0);
    }

    /**
     * 不同module + 不同Activity 跳转
     *
     * @param current        当前的fragment
     * @param activityClass  目标Activity的action
     * @param target         目标fragment
     * @param attach         附件
     * @param isTargetReload 是否重载目标fragment
     * @param needKills      跳转前需要杀死哪些fragment
     */
    public void toFragModule(Class current, String activityClass, String target, Object attach, boolean isTargetReload, Class... needKills) {
        handler.postDelayed(() -> {
            // 检测传输目标是否为空
            if (current == null | activityClass == null | target == null) {
                toast(getString(R.string.NULL_TIP), 5000);
                return;
            }
            // 检测附件是否实现序列化
            if (attach != null) {
                if (!(attach instanceof Serializable)) {
                    String string = getString(R.string.ATTACH_NOT_SERILIZABLE);
                    String format = String.format(string, attach.getClass().getSimpleName());
                    toast(format, 5000);
                    return;
                }
            }
            SkipBean skipbean = new SkipBean();
            skipbean.setCurrentFragmentClassName(current.getName());
            skipbean.setTargetActivityClassName(activityClass);
            skipbean.setTargetFragmentClassName(target);
            skipbean.setAttach(attach);
            skipbean.setTargetReload(isTargetReload);
            skipbean.setCurrentACFinish(false);
            RootHelper.toActivityImplicit(this, activityClass, false, false, false, 0, skipbean);
            if (needKills.length > 0) {
                killWhich(needKills);
            }
            // 追踪流程
            trackModule(current, activityClass, target, attach, isTargetReload, needKills);
        }, 0);
    }

    /**
     * 不同module + 不同Activity 跳转 (带延时和自定义结束当前)
     *
     * @param current        当前的fragment
     * @param activityClass  目标Activity的action
     * @param target         目标fragment
     * @param attach         附件
     * @param isTargetReload 是否重载目标fragment
     * @param isFinish       是否结束当前AC
     * @param delay          延迟毫秒数
     * @param needKills      跳转前需要杀死哪些fragment
     */
    public void toFragModule(Class current, String activityClass, String target, Object attach, boolean isTargetReload, boolean isFinish, int delay, Class... needKills) {
        handler.postDelayed(() -> {
            // 检测传输目标是否为空
            if (current == null | activityClass == null | target == null) {
                toast(getString(R.string.NULL_TIP), 5000);
                return;
            }
            // 检测附件是否实现序列化
            if (attach != null) {
                if (!(attach instanceof Serializable)) {
                    String string = getString(R.string.ATTACH_NOT_SERILIZABLE);
                    String format = String.format(string, attach.getClass().getSimpleName());
                    toast(format, 5000);
                    return;
                }
            }
            SkipBean skipbean = new SkipBean();
            skipbean.setCurrentFragmentClassName(current.getName());
            skipbean.setTargetActivityClassName(activityClass);
            skipbean.setTargetFragmentClassName(target);
            skipbean.setAttach(attach);
            skipbean.setTargetReload(isTargetReload);
            skipbean.setCurrentACFinish(false);
            RootHelper.toActivityImplicit(this, activityClass, false, isFinish, false, delay, skipbean);
            if (needKills.length > 0) {
                killWhich(needKills);
            }
            // 追踪流程
            trackModule(current, activityClass, target, attach, isTargetReload, needKills);
        }, 0);
    }

    /**
     * 设置Eventbus数据接收监听器
     *
     * @param clazz             数据类型, 如 XXX.class
     * @param rootEventListener 数据回调监听器
     * @param <T>               泛型
     */
    public <T> void setEventListener(Class<T> clazz, RootEventListener<T> rootEventListener) {
        if (clazz != null && rootEventListener != null) {
            eventClazzs.add(clazz);
            eventListeners.add(rootEventListener);
        } else {
            if (clazz == null) {
                toast(R.string.EVENT_BUS_CLASS_IS_NULL, 5000);
            } else {
                toast(R.string.EVENT_BUS_LISTENER_IS_NULL, 5000);
            }
        }
    }

    /**
     * 发送Eventbus事件
     *
     * @param obj     数据
     * @param isStick T:粘性
     */
    public void sendEvent(Object obj, boolean isStick) {
        RootEvent.sendEvent(obj, isStick);
    }

    /**
     * 杀死APP
     */
    public void kill() {
        RootHelper.kill();
    }

    /**
     * 吐司提示
     *
     * @param tip      提示
     * @param duration 时长
     * @param page     由哪个fragment或者Activity弹出
     */
    public void toast(String tip, int duration, Class... page) {
        Class clazz = getClass();// 默认Activity
        if (page != null) {
            if (page.length > 0) {
                clazz = page[0];
            }
        }
        RootHelper.toast(this, tip, duration, clazz);
    }

    /**
     * 吐司提示
     *
     * @param stringId 字符资源ID
     * @param duration 时长
     * @param page     由哪个fragment或者Activity弹出
     */
    public void toast(@StringRes int stringId, int duration, Class... page) {
        Class clazz = getClass();// 默认Activity
        if (page != null) {
            if (page.length > 0) {
                clazz = page[0];
            }
        }
        RootHelper.toast(this, getString(stringId), duration, clazz);
    }

    /**
     * 创建传输对象的KEY
     *
     * @return 传输对象的KEY
     */
    public static String getPendingIntentKey() {
        return RootMAActivity.INTENT_NAME;
    }

    /**
     * 创建传输对象
     *
     * @param targetAC   目标AC
     * @param targetFrag 目标fragment
     * @param attach     附件
     * @return skipbean
     */
    public static byte[] getPendingIntentValue(String targetAC, String targetFrag, Object attach) {
        if (TextUtils.isEmpty(targetAC) | TextUtils.isEmpty(targetFrag)) {
            throw new RuntimeException("请检查是否指定了目标Activity或者目标Fragment");
        }
        SkipBean skipBean = new SkipBean();
        skipBean.setCurrentFragmentClassName("");
        skipBean.setTargetActivityClassName(targetAC);
        skipBean.setTargetFragmentClassName(targetFrag);
        if (attach == null) {
            attach = new Object();
        }
        skipBean.setAttach(attach);
        skipBean.setTargetReload(true);
        skipBean.setCurrentACFinish(false);
        skipBean.setAttachClass(attach.getClass());
        return JSONObject.toJSONString(skipBean).getBytes();
    }

    /**
     * 清理Activity
     *
     * @param keepActivitys 需要保留的Activity
     */
    public void killActivitys(Class... keepActivitys) {
        ActivityHelper.killActivitys(keepActivitys);
    }

    /**
     * 清理全部的Activity
     */
    public void killAllActivitys() {
        ActivityHelper.killAllActivity();
    }

    /**
     * 获取SD卡根路径
     *
     * @param dirName 目录名
     * @return 根路径
     */
    public String getSdPath(String dirName) {
        // 规避斜杆错误
        dirName = dirName.startsWith(File.separator) ? dirName : File.separator + dirName;
        // 定义版本层
        int SDK_cur = Build.VERSION.SDK_INT;
        int SDK_Q = Build.VERSION_CODES.Q;
        int SDK_R = Build.VERSION_CODES.R;

        // 当前版本 < Android Q
        if (SDK_cur < SDK_Q) {
            Lgg.i(TAG, "当前路径为[SDK_cur < SDK_Q], 路径为[传统模式]");
            return Environment.getExternalStorageDirectory().getAbsolutePath() + dirName;
        }

        // 当前版本 == Android Q
        if (SDK_cur == SDK_Q) {
            // requestLegacyExternalStorage 兼容
            if (isRequestLegacyExternalStorage(this)) {// requestLegacyExternalStorage = true - 传统路径
                Lgg.i(TAG, "当前路径为[SDK_cur == SDK_Q], 路径为[传统模式]");
                return Environment.getExternalStorageDirectory().getAbsolutePath() + dirName;
            } else {// requestLegacyExternalStorage = false - 沙盒路径
                Lgg.i(TAG, "当前路径为[SDK_cur == SDK_Q], 路径为[沙盒模式]");
                return Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + dirName;
            }

        }

        // 当前版本 > Android Q (大于等于 Android R)
        if (SDK_cur >= SDK_R) {
            if (Environment.isExternalStorageManager()) {// 是否有超管权限 - 传统模式
                Lgg.i(TAG, "当前路径为[SDK_cur >= SDK_R], 且开通超管权限, 路径为[传统模式]");
                return Environment.getExternalStorageDirectory().getAbsolutePath() + dirName;
            } else {// 沙盒模式
                Lgg.i(TAG, "当前路径为[SDK_cur >= SDK_R], 且未开通超管权限, 路径为[沙盒模式]");
                return Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + dirName;
            }
        } else {
            return Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + dirName;
        }

    }

    /**
     * 判断AndroidMainfest.xml中 [RequestLegacyExternalStorage] 的值 (仅在Android Q这个版本使用)
     *
     * @param context 域
     * @return T: 设置了true
     */
    public boolean isRequestLegacyExternalStorage(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            Field field = appInfo.getClass().getDeclaredField("privateFlags");
            field.setAccessible(true);
            int value = (int) field.get(appInfo);
            Lgg.i(TAG, "读取Legacy标签成功");
            return (value & (1 << 29)) != 0;
        } catch (Exception e) {
            Lgg.e(TAG, "读取Legacy标签出错");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 隐藏软键盘
     */
    public void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /**
     * 显示软键盘
     */
    public void showKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInputFromInputMethod(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /* -------------------------------------------- abstract -------------------------------------------- */

    /**
     * onCreated方法里, 任何操作之前
     */
    public void beforeAllFirst() {

    }

    /**
     * 初始配置
     *
     * @return 配置对象
     */
    public abstract RootProperty initProperty();

    /**
     * 你的业务逻辑
     */
    public abstract void onNexts();

    /**
     * 回退键的点击事件
     *
     * @return true:自定义逻辑 false:super.onBackPress()
     */
    public abstract boolean onBackClick();

    /**
     * 视图全部绘制完毕 (即视图已经加载到结构树上, 可用于获取控件大小)
     */
    public void inflateViewFinish() {

    }

    /* -------------------------------------------- impl -------------------------------------------- */

    /**
     * IdleHandler 实现类
     */
    class IdleHandlerImpl implements MessageQueue.IdleHandler {

        @Override
        public boolean queueIdle() {
            // 回调
            inflateViewFinish();
            // return true，此Idle一直在Handler中, 循环执行
            // return false, 执行1次后就从Handler线程中remove掉
            return false;
        }
    }

}
