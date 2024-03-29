package com.newhiber.newhiber.bean;

import com.newhiber.newhiber.impl.PermissionAction;

import java.io.Serializable;

/*
 * Created by qianli.ma on 3/28/2024.
 * 2024新权限用法: 申请权限的实体类
 *
 */
public class ApplyPermissionBean implements Serializable {

    public String[] permissions;
    public PermissionAction action;

    public ApplyPermissionBean() {
    }

    public ApplyPermissionBean(String[] permissions, PermissionAction action) {
        this.permissions = permissions;
        this.action = action;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public ApplyPermissionBean setPermissions(String[] permissions) {
        this.permissions = permissions;
        return this;
    }

    public PermissionAction getAction() {
        return action;
    }

    public ApplyPermissionBean setAction(PermissionAction action) {
        this.action = action;
        return this;
    }
}

