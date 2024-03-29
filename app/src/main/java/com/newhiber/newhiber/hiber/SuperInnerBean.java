package com.newhiber.newhiber.hiber;

import android.view.View;

import com.newhiber.newhiber.bean.StringBean;
import com.newhiber.newhiber.cons.SUPERMISSION;
import com.newhiber.newhiber.impl.SuperPermissListener;

import java.io.Serializable;

import androidx.annotation.LayoutRes;

/*
 * Created by Administrator on 2021/01/022.
 */
@Deprecated
public class SuperInnerBean implements Serializable {

    /**
     * 权限自定义制图
     */
    private View superView;

    /**
     * 权限默认字符内容
     */
    private StringBean stringBean;

    /**
     * 当前的fragment
     */
    private Class currentFrag;

    @LayoutRes
    private int layoutId;

    /**
     * 权限行为(用于赋值requestCode)
     */
    private SUPERMISSION supermission;
    
    private SuperPermissListener listener;

    public SuperPermissListener getListener() {
        return listener;
    }

    public SuperInnerBean setListener(SuperPermissListener listener) {
        this.listener = listener;
        return this;
    }

    public SUPERMISSION getSupermission() {
        return supermission;
    }

    public SuperInnerBean setSupermission(SUPERMISSION supermission) {
        this.supermission = supermission;
        return this;
    }

    public View getSuperView() {
        return superView;
    }

    public SuperInnerBean setSuperView(View superView) {
        this.superView = superView;
        return this;
    }

    public StringBean getStringBean() {
        return stringBean;
    }

    public SuperInnerBean setStringBean(StringBean stringBean) {
        this.stringBean = stringBean;
        return this;
    }

    public Class getCurrentFrag() {
        return currentFrag;
    }

    public SuperInnerBean setCurrentFrag(Class currentFrag) {
        this.currentFrag = currentFrag;
        return this;
    }

    public int getLayoutId() {
        return layoutId;
    }

    public SuperInnerBean setLayoutId(@LayoutRes int layoutId) {
        this.layoutId = layoutId;
        return this;
    }
}
