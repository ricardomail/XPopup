package com.lxj.xpopup;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.lxj.xpopup.animator.PopupAnimator;
import com.lxj.xpopup.core.AttachPopupView;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.core.CenterPopupView;
import com.lxj.xpopup.core.PopupInfo;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.enums.PopupStatus;
import com.lxj.xpopup.enums.PopupType;
import com.lxj.xpopup.impl.AttachListPopupView;
import com.lxj.xpopup.impl.BottomListPopupView;
import com.lxj.xpopup.impl.CenterListPopupView;
import com.lxj.xpopup.impl.ConfirmPopupView;
import com.lxj.xpopup.impl.InputConfirmPopupView;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.lxj.xpopup.interfaces.OnCancelListener;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.lxj.xpopup.interfaces.XPopupCallback;
import com.lxj.xpopup.util.KeyboardUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * PopupView的控制类，控制生命周期：显示，隐藏，添加，删除。
 */
public class XPopup {
    private static XPopup instance = null;
    private static WeakReference<Context> contextRef;
    private PopupInfo tempInfo = null;
    private BasePopupView tempView;
    private Handler handler = new Handler();
    private ViewGroup decorView = null;
    private int primaryColor = Color.parseColor("#121212");
    private static ArrayList<BasePopupView> popupViews = new ArrayList<>();

    private XPopup() {}

    public static XPopup get(Context ctx) {
        if (instance == null) {
            instance = new XPopup();
        }
        contextRef = new WeakReference<>(ctx);
        if (contextRef.get() == null) {
            throw new IllegalArgumentException("context can not be null!");
        }
        if (!(contextRef.get() instanceof Activity)) {
            throw new IllegalArgumentException("context must be an instance of Activity");
        }
        KeyboardUtils.registerSoftInputChangedListener((Activity) contextRef.get(), new KeyboardUtils.OnSoftInputChangedListener() {
            @Override
            public void onSoftInputChanged(int height) {
                if (height == 0) { // 说明对话框隐藏
                    for (BasePopupView pv: popupViews){
                        pv.getPopupContentView().animate().translationY(0)
                                .setDuration(300).start();
                    }
                }
            }
        });
        return instance;
    }

    /**
     * 显示弹窗，并指定tag。
     * @param tag 在同时显示多个弹窗的场景下，tag会有用；否则tag无用，也不需要传。
     */
    public void show(Object tag) {
        if (tempView==null) throw new IllegalArgumentException("要显示的弹窗为空！");
        //1. set popup view
        tempView.popupInfo = tempInfo;
        if(tag!=null)tempView.setTag(tag);
        popupViews.add(tempView);
        tempInfo = null;
        tempView = null;

        //2. show popup view with tag
        for (BasePopupView pv: popupViews){
            if(tag!=null){
                if(pv.getTag() == tag){
                    showInternal(pv);
                    break;
                }
            }else {
                //show all
                showInternal(pv);
            }
        }
    }

    /**
     * 显示弹窗
     */
    public void show(){
        show(null);
    }

    private void showInternal(final BasePopupView pv){
        if(pv.getParent()!=null)return;
        Activity activity = (Activity) contextRef.get();
        decorView = (ViewGroup) activity.getWindow().getDecorView();
        decorView.addView(pv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        decorView.bringChildToFront(pv);

        //2. 执行初始化
        pv.init(new Runnable() { // 弹窗显示动画执行完毕调用
            @Override
            public void run() {
                if (pv.popupInfo.xPopupCallback != null)
                    pv.popupInfo.xPopupCallback.onShow();
            }
        }, new Runnable() {             // 弹窗消失动画执行完毕调用
            @Override
            public void run() {
                popupViews.remove(pv);
                if (pv.popupInfo.xPopupCallback != null)
                    pv.popupInfo.xPopupCallback.onDismiss();

                // 移除弹窗
                decorView.removeView(pv);

                // 释放对象
                release();
            }
        });
    }

    /**
     * 消失
     */
    public void dismiss() {
        dismiss(null);
    }

    /**
     * 消失，让指定tag的弹窗消失。
     * @param tag 在同时显示多弹窗情况下有用，否则没有用。
     */
    public void dismiss(Object tag){
        if(tag==null){
            //如果没有tag，则因此第0个
            popupViews.get(0).dismiss();
        }else {
            int temp = -1;
            for (int i = 0; i < popupViews.size(); i++) {
                if(tag==popupViews.get(i).getTag()){
                    temp = i;
                    break;
                }
            }
            if(temp!=-1){
                popupViews.get(temp).dismiss();
            }
        }
    }

    /**
     * 释放相关资源
     */
    private void release(){
        if(!popupViews.isEmpty()){
            return;
        }
        handler.removeCallbacks(null);
        contextRef.clear();
        contextRef = null;
        if (decorView != null) {
            KeyboardUtils.removeLayoutChangeListener(decorView);
            decorView = null;
        }
    }

    /**
     * 设置主色调
     *
     * @param color
     */
    public void setPrimaryColor(int color) {
        this.primaryColor = color;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    /**
     * 设置显示和隐藏的回调
     *
     * @param callback
     * @return
     */
    public XPopup setPopupCallback(XPopupCallback callback) {
        checkPopupInfo();
        tempInfo.xPopupCallback = callback;
        return this;
    }

    /**
     * 设置弹窗的类型，除非你非常熟悉本库的代码，否则不建议自己设置弹窗类型。
     * @param popupType PopupType其中之一
     * @return
     */
    public XPopup position(PopupType popupType) {
        checkPopupInfo();
        tempInfo.popupType = popupType;
        return this;
    }

    /**
     * 设置某个内置的动画类型
     *
     * @param animation
     * @return
     */
    public XPopup popupAnimation(PopupAnimation animation) {
        checkPopupInfo();
        tempInfo.popupAnimation = animation;
        return this;
    }

    /**
     * 设置自定义的动画器
     *
     * @param animator
     * @return
     */
    public XPopup customAnimator(PopupAnimator animator) {
        checkPopupInfo();
        tempInfo.customAnimator = animator;
        return this;
    }

    public XPopup dismissOnBackPressed(boolean isDismissOnBackPressed) {
        checkPopupInfo();
        tempInfo.isDismissOnBackPressed = isDismissOnBackPressed;
        return this;
    }

    public XPopup dismissOnTouchOutside(boolean isDismissOnTouchOutside) {
        checkPopupInfo();
        tempInfo.isDismissOnTouchOutside = isDismissOnTouchOutside;
        return this;
    }

    public XPopup atView(View view) {
        checkPopupInfo();
        tempInfo.setAtView(view);
        tempInfo.touchPoint = null;
        return this;
    }

    public XPopup hasShadowBg(boolean hasShadowBg) {
        checkPopupInfo();
        tempInfo.hasShadowBg = hasShadowBg;
        return this;
    }

    /**
     * 设置弹窗的宽和高，只对Center和Bottom类型的弹窗有效
     * 语义有歧义，请使用 maxWidthAndHeight
     * @param maxWidth  传0就是不改变
     * @param maxHeight 传0就是不改变
     * @return
     */
    @Deprecated
    public XPopup setWidthAndHeight(int maxWidth, int maxHeight) {
        checkPopupInfo();
        tempInfo.maxWidth = maxWidth;
        tempInfo.maxHeight = maxHeight;
        return this;
    }

    /**
     * 设置弹窗的宽和高的最大值，只对Center和Bottom类型的弹窗有效
     *
     * @param maxWidth  传0就是不限制
     * @param maxHeight 传0就是不限制
     * @return
     */
    public XPopup maxWidthAndHeight(int maxWidth, int maxHeight) {
        checkPopupInfo();
        tempInfo.maxWidth = maxWidth;
        tempInfo.maxHeight = maxHeight;
        return this;
    }

    private void checkPopupInfo() {
        if (tempInfo == null) {
            tempInfo = new PopupInfo();
        }
    }

    /**
     * 收集某个View的按下坐标，用于Attach类型的弹窗显示。如果调用这个方法，弹窗就有了
     * 参考点，无需再调用atView方法了
     *
     * @param view
     * @return
     */
    public XPopup watch(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    checkPopupInfo();
                    tempInfo.touchPoint = new PointF(event.getRawX(), event.getRawY());
                }
                return false;
            }
        });
        return this;
    }

    /************** 便捷方法 ************/

    /**
     * 显示确认和取消对话框
     *
     * @param title           对话框标题
     * @param content         对话框内容
     * @param confirmListener 点击确认的监听器
     * @param cancelListener  点击取消的监听器
     * @return
     */
    public XPopup asConfirm(String title, String content, OnConfirmListener confirmListener, OnCancelListener cancelListener) {
        position(PopupType.Center);

        ConfirmPopupView popupView = new ConfirmPopupView(contextRef.get());
        popupView.setTitleContent(title, content);
        popupView.setListener(confirmListener, cancelListener);
        this.tempView = popupView;
        return this;
    }

    public XPopup asConfirm(String title, String content, OnConfirmListener confirmListener) {
        return asConfirm(title, content, confirmListener, null);
    }

    /**
     * 显示带有输入框，确认和取消对话框
     *
     * @param title           对话框标题
     * @param content         对话框内容
     * @param confirmListener 点击确认的监听器
     * @param cancelListener  点击取消的监听器
     * @return
     */
    public XPopup asInputConfirm(String title, String content, OnInputConfirmListener confirmListener, OnCancelListener cancelListener) {
        position(PopupType.Center);

        InputConfirmPopupView popupView = new InputConfirmPopupView(contextRef.get());
        popupView.setTitleContent(title, content);
        popupView.setListener(confirmListener, cancelListener);
        this.tempView = popupView;
        return this;
    }

    public XPopup asInputConfirm(String title, String content, OnInputConfirmListener confirmListener) {
        return asInputConfirm(title, content, confirmListener, null);
    }

    /**
     * 显示在中间的列表Popup
     *
     * @param title          标题，可以不传，不传则不显示
     * @param data           显示的文本数据
     * @param iconIds        图标的id数组，可以没有
     * @param selectListener 选中条目的监听器
     * @return
     */
    public XPopup asCenterList(String title, String[] data, int[] iconIds, int checkedPosition, OnSelectListener selectListener) {
        position(PopupType.Center);
        this.tempView = new CenterListPopupView(contextRef.get())
                .setStringData(title, data, iconIds)
                .setCheckedPosition(checkedPosition)
                .setOnSelectListener(selectListener);
        return this;
    }

    public XPopup asCenterList(String title, String[] data, OnSelectListener selectListener) {
        return asCenterList(title, data, null, -1, selectListener);
    }

    public XPopup asCenterList(String title, String[] data, int[] iconIds, OnSelectListener selectListener) {
        return asCenterList(title, data, iconIds, -1, selectListener);
    }


    /**
     * 显示在中间加载的弹窗
     *
     * @return
     */
    public XPopup asLoading(String title) {
        position(PopupType.Center);
        this.tempView = new LoadingPopupView(contextRef.get())
                .setTitle(title);
        return this;
    }

    public XPopup asLoading() {
        return asLoading(null);
    }


    /**
     * 显示在底部的列表Popup
     *
     * @param title          标题，可以不传，不传则不显示
     * @param data           显示的文本数据
     * @param iconIds        图标的id数组，可以没有
     * @param checkedPosition  选中的位置，传-1为不选中
     * @param selectListener 选中条目的监听器
     * @return
     */
    public XPopup asBottomList(String title, String[] data, int[] iconIds, int checkedPosition, boolean enableGesture, OnSelectListener selectListener) {
        position(PopupType.Bottom);
        this.tempView = new BottomListPopupView(contextRef.get())
                .setStringData(title, data, iconIds)
                .setCheckedPosition(checkedPosition)
                .setOnSelectListener(selectListener)
                .enableGesture(enableGesture);
        return this;
    }

    public XPopup asBottomList(String title, String[] data, OnSelectListener selectListener) {
        return asBottomList(title, data, null, -1,true, selectListener);
    }
    public XPopup asBottomList(String title, String[] data, int[] iconIds, OnSelectListener selectListener) {
        return asBottomList(title, data, iconIds, -1, true, selectListener);
    }
    public XPopup asBottomList(String title, String[] data, int[] iconIds, int checkedPosition, OnSelectListener selectListener) {
        return asBottomList(title, data, iconIds, checkedPosition, true, selectListener);
    }
    public XPopup asBottomList(String title, String[] data, int[] iconIds, boolean enableGesture, OnSelectListener selectListener) {
        return asBottomList(title, data, iconIds, -1, enableGesture, selectListener);
    }


    /**
     * 显示依附于某View的列表，必须调用atView()方法，指定依附的View
     *
     * @param data           显示的文本数据
     * @param iconIds        图标的id数组，可以为null
     * @param offsetX        x方向偏移量
     * @param offsetY        y方向偏移量
     * @param selectListener 选中条目的监听器
     * @return
     */
    public XPopup asAttachList(String[] data, int[] iconIds, int offsetX, int offsetY, OnSelectListener selectListener) {
        position(PopupType.AttachView);

        this.tempView = new AttachListPopupView(contextRef.get())
                .setStringData(data, iconIds)
                .setOffsetXAndY(offsetX, offsetY)
                .setOnSelectListener(selectListener);
        return this;
    }

    public XPopup asAttachList(String[] data, int[] iconIds, OnSelectListener selectListener) {
        return asAttachList(data, iconIds, 0, 0, selectListener);
    }


    /**
     * 自定义弹窗
     **/
    public XPopup asCustom(BasePopupView popupView) {
        if (popupView instanceof CenterPopupView) {
            position(PopupType.Center);
        } else if (popupView instanceof BottomPopupView) {
            position(PopupType.Bottom);
        } else if (popupView instanceof AttachPopupView) {
            position(PopupType.AttachView);
        } else {
            checkPopupInfo();
        }
        this.tempView = popupView;
        return this;
    }
}
