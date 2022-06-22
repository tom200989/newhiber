package com.newhiber.newhiber.impl;

/*
 * Created by qianli.ma on 2019/4/28 0028.
 */
public abstract class RootEventListener<T> {

    /**
     * 返回数据
     *
     * @param t 泛型数据
     */
    public abstract void getData(T t);

    /**
     * 是否仅仅作用于当前页面(默认仅限当前界面接收)
     *
     * @return Ture: 仅仅作用于当前页面
     */
    public boolean isCurrentPageEffectOnly() {
        return true;
    }
}
