package com.newhiber.newhiber.impl;

import java.util.List;

/**
 * 2024新权限用法: 申请权限失败后的回调接口
 */
public interface PermissionAction {

    public enum PermissionType {
        NOW_OTHER_FALSE,// 用户点击了[不再询问], 通过该标记触发跳转应用详情页
        NOW_OTHER_TRUE,// 用户没有点击[不再询问], 通过该标记触发再次触发系统默认的权限弹窗
        NOW_WRITE_READ // 对于大于10.0的版本来讲, 只有直接跳转到[打开所有文件]设置页
    }

    /**
     * 申请权限失败后的回调接口
     *
     * @param permissionType    权限类型
     * @param deniedPermissions 被拒绝的权限
     */
    void onDenied(PermissionType permissionType, List<String> deniedPermissions);

    /**
     * 申请权限全部通过后的回调接口
     */
    void onAllGranted();
}
