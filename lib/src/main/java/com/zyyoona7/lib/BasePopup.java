package com.zyyoona7.lib;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v4.widget.PopupWindowCompat;
import android.transition.Transition;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.PopupWindow;

/**
 * Created by zyyoona7 on 2017/8/3.
 * <p>
 * PopupWindow封装
 */

public abstract class BasePopup<T extends BasePopup> implements PopupWindow.OnDismissListener {
    private static final String TAG = "EasyPopup";

    private static final float DEFAULT_DIM = 0.7f;

    //PopupWindow对象
    private PopupWindow mPopupWindow;

    //context
    private Context mContext;
    //contentView
    private View mContentView;
    //布局id
    private int mLayoutId;
    //获取焦点
    private boolean mFocusable = true;
    //是否触摸之外dismiss
    private boolean mOutsideTouchable = true;

    //宽高
    private int mWidth;
    private int mHeight;

    private int mAnimationStyle;

    private PopupWindow.OnDismissListener mOnDismissListener;

    //弹出pop时，背景是否变暗
    private boolean isBackgroundDim;

    //背景变暗时透明度
    private float mDimValue = DEFAULT_DIM;
    //背景变暗颜色
    @ColorInt
    private int mDimColor = Color.BLACK;

    //背景变暗的view
    @NonNull
    private ViewGroup mDimView;

    private Transition mEnterTransition;
    private Transition mExitTransition;

    private boolean mFocusAndOutsideEnable;

    private View mAnchorView;
    @YGravity
    private int mYGravity = YGravity.BELOW;
    @XGravity
    private int mXGravity = XGravity.LEFT;
    private int mOffsetX;
    private int mOffsetY;

    //传统的显示方法是否需要测量宽高
    //showAsDropDown() showAtLocation()系列
    private boolean isNeedMeasureWH = false;

    private OnMeasureFinishedListener mOnMeasureFinishedListener;

    protected T self() {
        //noinspection unchecked
        return (T) this;
    }

    public T apply() {
        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow();
        }

        onPopupWindowCreated();

        initContentViewAndWH();

        onPopupWindowViewCreated(mContentView);

        if (mAnimationStyle != 0) {
            mPopupWindow.setAnimationStyle(mAnimationStyle);
        }

        initFocusAndBack();
        mPopupWindow.setOnDismissListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mEnterTransition != null) {
                mPopupWindow.setEnterTransition(mEnterTransition);
            }

            if (mExitTransition != null) {
                mPopupWindow.setExitTransition(mExitTransition);
            }
        }

        return self();
    }

    private void initContentViewAndWH() {
        if (mContentView == null) {
            if (mLayoutId != 0 && mContext != null) {
                mContentView = LayoutInflater.from(mContext).inflate(mLayoutId, null);
            } else {
                throw new IllegalArgumentException("The content view is null,the layoutId=" + mLayoutId + ",context=" + mContext);
            }
        }
        mPopupWindow.setContentView(mContentView);

        if (mWidth > 0 || mWidth == ViewGroup.LayoutParams.WRAP_CONTENT || mWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
            mPopupWindow.setWidth(mWidth);
        } else {
            mPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (mHeight > 0 || mHeight == ViewGroup.LayoutParams.WRAP_CONTENT || mHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            mPopupWindow.setHeight(mHeight);
        } else {
            mPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void initFocusAndBack() {
        if (!mFocusAndOutsideEnable) {
            //from https://github.com/pinguo-zhouwei/CustomPopwindow
            mPopupWindow.setFocusable(true);
            mPopupWindow.setOutsideTouchable(false);
            mPopupWindow.setBackgroundDrawable(null);
            //注意下面这三个是contentView 不是PopupWindow，响应返回按钮事件
            mPopupWindow.getContentView().setFocusable(true);
            mPopupWindow.getContentView().setFocusableInTouchMode(true);
            mPopupWindow.getContentView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        mPopupWindow.dismiss();

                        return true;
                    }
                    return false;
                }
            });
            //在Android 6.0以上 ，只能通过拦截事件来解决
            mPopupWindow.setTouchInterceptor(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    if ((event.getAction() == MotionEvent.ACTION_DOWN)
                            && ((x < 0) || (x >= mWidth) || (y < 0) || (y >= mHeight))) {
                        //outside
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                        //outside
                        return true;
                    }
                    return false;
                }
            });
        } else {
            mPopupWindow.setFocusable(mFocusable);
            mPopupWindow.setOutsideTouchable(mOutsideTouchable);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /****自定义生命周期方法****/

    /**
     * PopupWindow对象创建完成
     */
    protected void onPopupWindowCreated() {
        //执行设置PopupWindow属性也可以通过Builder中设置
        //setContentView(x,x,x);
        //...
        initAttributes();
    }

    protected void onPopupWindowViewCreated(View contentView) {
        initViews(contentView);
    }

    protected void onPopupWindowDismiss() {
    }

    /**
     * 可以在此方法中设置PopupWindow需要的属性
     */
    protected abstract void initAttributes();

    /**
     * 初始化view {@see getView()}
     *
     * @param view
     */
    protected abstract void initViews(View view);

    /****设置属性方法****/

    public T setContext(Context context) {
        this.mContext = context;
        return self();
    }

    public T setContentView(View contentView) {
        this.mContentView = contentView;
        this.mLayoutId = 0;
        return self();
    }

    public T setContentView(@LayoutRes int layoutId) {
        this.mContentView = null;
        this.mLayoutId = layoutId;
        return self();
    }

    public T setContentView(Context context, @LayoutRes int layoutId) {
        this.mContext = context;
        this.mContentView = null;
        this.mLayoutId = layoutId;
        return self();
    }

    public T setContentView(View contentView, int width, int height) {
        this.mContentView = contentView;
        this.mLayoutId = 0;
        this.mWidth = width;
        this.mHeight = height;
        return self();
    }

    public T setContentView(@LayoutRes int layoutId, int width, int height) {
        this.mContentView = null;
        this.mLayoutId = layoutId;
        this.mWidth = width;
        this.mHeight = height;
        return self();
    }

    public T setContentView(Context context, @LayoutRes int layoutId, int width, int height) {
        this.mContext = context;
        this.mContentView = null;
        this.mLayoutId = layoutId;
        this.mWidth = width;
        this.mHeight = height;
        return self();
    }

    public T setWidth(int width) {
        this.mWidth = width;
        return self();
    }

    public T setHeight(int height) {
        this.mHeight = height;
        return self();
    }

    public T setAnchorView(View view) {
        this.mAnchorView = view;
        return self();
    }

    public T setYGravity(@YGravity int yGravity) {
        this.mYGravity = yGravity;
        return self();
    }

    public T setXGravity(@XGravity int yGravity) {
        this.mXGravity = yGravity;
        return self();
    }

    public T setOffsetX(int offsetX) {
        this.mOffsetX = offsetX;
        return self();
    }

    public T setOffsetY(int offsetY) {
        this.mOffsetY = offsetY;
        return self();
    }

    public T setAnimationStyle(@StyleRes int animationStyle) {
        this.mAnimationStyle = animationStyle;
        return self();
    }

    public T setFocusable(boolean focusable) {
        this.mFocusable = focusable;
        return self();
    }

    public T setOutsideTouchable(boolean outsideTouchable) {
        this.mOutsideTouchable = outsideTouchable;
        return self();
    }

    /**
     * 是否可以点击PopupWindow之外的地方dismiss
     *
     * @param focusAndOutsideEnable
     * @return
     */
    public T setFocusAndOutsideEnable(boolean focusAndOutsideEnable) {
        this.mFocusAndOutsideEnable = focusAndOutsideEnable;
        return self();
    }

    /**
     * 背景变暗支持api>=18
     *
     * @param isDim
     * @return
     */
    public T setBackgroundDimEnable(boolean isDim) {
        this.isBackgroundDim = isDim;
        return self();
    }

    public T setDimValue(@FloatRange(from = 0.0f, to = 1.0f) float dimValue) {
        this.mDimValue = dimValue;
        return self();
    }

    public T setDimColor(@ColorInt int color) {
        this.mDimColor = color;
        return self();
    }

    public T setDimView(@NonNull ViewGroup dimView) {
        this.mDimView = dimView;
        return self();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public T setEnterTransition(Transition enterTransition) {
        this.mEnterTransition = enterTransition;
        return self();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public T setExitTransition(Transition exitTransition) {
        this.mExitTransition = exitTransition;
        return self();
    }

    /**
     * 传统模式时是否需要测量宽高
     * showAsDropDown() showAtLocation()系列方法
     *
     * @param needMeasureWH
     * @return
     */
    public T setNeedMeasureWH(boolean needMeasureWH) {
        this.isNeedMeasureWH = needMeasureWH;
        return self();
    }

    /**
     * 检查是否调用了 apply() 方法
     */
    private void checkIsApply() {
        if (mPopupWindow == null) {
            apply();
        }
    }

    /**
     * 使用此方法需要在创建的时候调用setAnchorView()等属性设置{@see setAnchorView()}
     */
    public T showAsDropDown() {
        if (mAnchorView == null) {
            return self();
        }

        return showAsDropDown(mAnchorView, mOffsetX, mOffsetY);
    }

    /**
     * PopupWindow自带的显示方法
     *
     * @param anchor
     * @param offsetX
     * @param offsetY
     */
    public T showAsDropDown(View anchor, int offsetX, int offsetY) {
        //防止忘记调用 apply() 方法
        checkIsApply();

        handleBackgroundDim();
        mAnchorView = anchor;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        //是否需要重新测量宽高并执行测量结束回调
        if (isNeedMeasureWH) {
            needMeasureAndMeasure();
        }
        mPopupWindow.showAsDropDown(anchor, mOffsetX, mOffsetY);
        return self();
    }

    public T showAsDropDown(View anchor) {
        //防止忘记调用 apply() 方法
        checkIsApply();

        handleBackgroundDim();
        mAnchorView = anchor;
        //是否需要重新测量宽高并执行测量结束回调
        if (isNeedMeasureWH) {
            needMeasureAndMeasure();
        }
        mPopupWindow.showAsDropDown(anchor);
        return self();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public T showAsDropDown(View anchor, int offsetX, int offsetY, int gravity) {
        //防止忘记调用 apply() 方法
        checkIsApply();

        handleBackgroundDim();
        mAnchorView = anchor;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        //是否需要重新测量宽高并执行测量结束回调
        if (isNeedMeasureWH) {
            needMeasureAndMeasure();
        }
        PopupWindowCompat.showAsDropDown(mPopupWindow, anchor, mOffsetX, mOffsetY, gravity);
        return self();
    }

    public T showAtLocation(View parent, int gravity, int offsetX, int offsetY) {
        //防止忘记调用 apply() 方法
        checkIsApply();

        handleBackgroundDim();
        mAnchorView = parent;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        //是否需要重新测量宽高并执行测量结束回调
        if (isNeedMeasureWH) {
            needMeasureAndMeasure();
        }
        mPopupWindow.showAtLocation(parent, gravity, mOffsetX, mOffsetY);
        return self();
    }

    /**
     * 相对anchor view显示
     * <p>
     * 使用此方法需要在创建的时候调用setAnchorView()等属性设置{@see setAnchorView()}
     * <p>
     * 注意：如果使用 VerticalGravity 和 HorizontalGravity 时，请确保使用之后 PopupWindow 没有超出屏幕边界，
     * 如果超出屏幕边界，VerticalGravity 和 HorizontalGravity 可能无效，从而达不到你想要的效果。
     */
    public T showAtAnchorView() {
        if (mAnchorView == null) {
            return self();
        }
        return showAtAnchorView(mAnchorView, mYGravity, mXGravity);
    }

    /**
     * 相对anchor view显示，适用 宽高不为match_parent
     * <p>
     * 注意：如果使用 VerticalGravity 和 HorizontalGravity 时，请确保使用之后 PopupWindow 没有超出屏幕边界，
     * 如果超出屏幕边界，VerticalGravity 和 HorizontalGravity 可能无效，从而达不到你想要的效果。     *
     *
     * @param anchor
     * @param vertGravity
     * @param horizGravity
     */
    public T showAtAnchorView(@NonNull View anchor, @YGravity int vertGravity, @XGravity int horizGravity) {
        return showAtAnchorView(anchor, vertGravity, horizGravity, 0, 0);
    }

    /**
     * 相对anchor view显示，适用 宽高不为match_parent
     * <p>
     * 注意：如果使用 VerticalGravity 和 HorizontalGravity 时，请确保使用之后 PopupWindow 没有超出屏幕边界，
     * 如果超出屏幕边界，VerticalGravity 和 HorizontalGravity 可能无效，从而达不到你想要的效果。
     *
     * @param anchor
     * @param vertGravity  垂直方向的对齐方式
     * @param horizGravity 水平方向的对齐方式
     * @param x            水平方向的偏移
     * @param y            垂直方向的偏移
     */
    public T showAtAnchorView(@NonNull View anchor, @YGravity final int vertGravity, @XGravity int horizGravity, int x, int y) {
        //防止忘记调用 apply() 方法
        checkIsApply();

        mAnchorView = anchor;
        mOffsetX = x;
        mOffsetY = y;
        mYGravity = vertGravity;
        mXGravity = horizGravity;
        //处理背景变暗
        handleBackgroundDim();
        //测量并为宽高赋值
        needMeasureAndMeasure();

        x = calculateX(anchor, horizGravity, mWidth, mOffsetX);
        y = calculateY(anchor, vertGravity, mHeight, mOffsetY);
//        Log.i(TAG, "showAtAnchorView: w=" + measuredW + ",y=" + measuredH);
        PopupWindowCompat.showAsDropDown(mPopupWindow, anchor, x, y, Gravity.NO_GRAVITY);

        return self();
    }

    /**
     * 是否需要测量 contentView的大小
     * 如果需要重新测量并为宽高赋值
     * NOTE：将之前的添加mOnGlobalLayoutListener 解决宽高度固定或者有ScrollView导致位置不准的情况替换成此方法
     */
    private void needMeasureAndMeasure() {
        final View contentView = getContentView();
        if (mWidth <= 0 || mHeight <= 0) {
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            if (mWidth <= 0) {
                mWidth = contentView.getMeasuredWidth();
            }
            if (mHeight <= 0) {
                mHeight = contentView.getMeasuredHeight();
            }
        }
        //宽高测量完成时的回调
        if (mOnMeasureFinishedListener != null) {
            mOnMeasureFinishedListener.onMeasureFinished(this, mWidth, mHeight,
                    mAnchorView == null ? 0 : mAnchorView.getWidth(), mAnchorView == null ? 0 : mAnchorView.getHeight());
        }
    }

    /**
     * 根据垂直gravity计算y偏移
     *
     * @param anchor
     * @param vertGravity
     * @param measuredH
     * @param y
     * @return
     */
    private int calculateY(View anchor, int vertGravity, int measuredH, int y) {
        switch (vertGravity) {
            case YGravity.ABOVE:
                //anchor view之上
                y -= measuredH + anchor.getHeight();
                break;
            case YGravity.ALIGN_BOTTOM:
                //anchor view底部对齐
                y -= measuredH;
                break;
            case YGravity.CENTER:
                //anchor view垂直居中
                y -= anchor.getHeight() / 2 + measuredH / 2;
                break;
            case YGravity.ALIGN_TOP:
                //anchor view顶部对齐
                y -= anchor.getHeight();
                break;
            case YGravity.BELOW:
                //anchor view之下
                // Default position.
                break;
        }

        return y;
    }

    /**
     * 根据水平gravity计算x偏移
     *
     * @param anchor
     * @param horizGravity
     * @param measuredW
     * @param x
     * @return
     */
    private int calculateX(View anchor, int horizGravity, int measuredW, int x) {
        switch (horizGravity) {
            case XGravity.LEFT:
                //anchor view左侧
                x -= measuredW;
                break;
            case XGravity.ALIGN_RIGHT:
                //与anchor view右边对齐
                x -= measuredW - anchor.getWidth();
                break;
            case XGravity.CENTER:
                //anchor view水平居中
                x += anchor.getWidth() / 2 - measuredW / 2;
                break;
            case XGravity.ALIGN_LEFT:
                //与anchor view左边对齐
                // Default position.
                break;
            case XGravity.RIGHT:
                //anchor view右侧
                x += anchor.getWidth();
                break;
        }

        return x;
    }

    /**
     * 设置监听器
     *
     * @param listener
     */
    public T setOnDismissListener(PopupWindow.OnDismissListener listener) {
        this.mOnDismissListener = listener;
        return self();
    }

    public T setOnMeasureFinishedListener(OnMeasureFinishedListener listener) {
        this.mOnMeasureFinishedListener = listener;
        return self();
    }

    /**
     * 处理背景变暗
     * https://blog.nex3z.com/2016/12/04/%E5%BC%B9%E5%87%BApopupwindow%E5%90%8E%E8%AE%A9%E8%83%8C%E6%99%AF%E5%8F%98%E6%9A%97%E7%9A%84%E6%96%B9%E6%B3%95/
     */
    private void handleBackgroundDim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (isBackgroundDim) {
                if (mDimView != null) {
                    applyDim(mDimView);
                } else {
                    if (getContentView() != null) {
                        Activity activity = (Activity) getContentView().getContext();
                        if (activity != null) {
                            applyDim(activity);
                        }
                    }
                }
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void applyDim(Activity activity) {
        ViewGroup parent = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        //activity跟布局
//        ViewGroup parent = (ViewGroup) parent1.getChildAt(0);
        Drawable dim = new ColorDrawable(mDimColor);
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        dim.setAlpha((int) (255 * mDimValue));
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void applyDim(ViewGroup dimView) {
        ViewGroup parent = dimView;
        Drawable dim = new ColorDrawable(mDimColor);
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        dim.setAlpha((int) (255 * mDimValue));
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }

    /**
     * 清除背景变暗
     */
    private void clearBackgroundDim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (isBackgroundDim) {
                if (mDimView != null) {
                    clearDim(mDimView);
                } else {
                    if (getContentView() != null) {
                        Activity activity = (Activity) getContentView().getContext();
                        if (activity != null) {
                            clearDim(activity);
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void clearDim(Activity activity) {
        ViewGroup parent = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        //activity跟布局
//        ViewGroup parent = (ViewGroup) parent1.getChildAt(0);
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void clearDim(ViewGroup dimView) {
        ViewGroupOverlay overlay = dimView.getOverlay();
        overlay.clear();
    }

    /**
     * 获取PopupWindow中加载的view
     *
     * @return
     */
    public View getContentView() {
        if (mPopupWindow != null) {
            return mPopupWindow.getContentView();
        } else {
            return null;
        }
    }

    /**
     * 获取PopupWindow对象
     *
     * @return
     */
    public PopupWindow getPopupWindow() {
        return mPopupWindow;
    }

    /**
     * 获取PopupWindow 宽
     *
     * @return
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * 获取PopupWindow 高
     *
     * @return
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * 获取纵向Gravity
     *
     * @return
     */
    public int getXGravity() {
        return mYGravity;
    }

    /**
     * 获取横向Gravity
     *
     * @return
     */
    public int getYGravity() {
        return mXGravity;
    }

    /**
     * 获取x轴方向的偏移
     *
     * @return
     */
    public int getOffsetX() {
        return mOffsetX;
    }

    /**
     * 获取y轴方向的偏移
     *
     * @return
     */
    public int getOffsetY() {
        return mOffsetY;
    }

    /**
     * 是否正在显示
     *
     * @return
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    /**
     * 获取view
     *
     * @param viewId
     * @param <T>
     * @return
     */
    public <T extends View> T findViewById(@IdRes int viewId) {
        View view = null;
        if (getContentView() != null) {
            view = getContentView().findViewById(viewId);
        }
        return (T) view;
    }

    /**
     * 消失
     */
    public void dismiss() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    @Override
    public void onDismiss() {
        handleDismiss();
    }

    /**
     * PopupWindow消失后处理一些逻辑
     */
    private void handleDismiss() {
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }

        //清除背景变暗
        clearBackgroundDim();
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        onPopupWindowDismiss();
    }

    /**
     * PopupWindow是否显示在window中
     * 用于获取准确的PopupWindow宽高，可以重新设置偏移量
     */
    public interface OnMeasureFinishedListener {

        /**
         * 在 show方法之后 updateLocation之前执行
         *
         * @param popWidth  PopupWindow准确的宽
         * @param popHeight PopupWindow准确的高
         * @param anchorW   锚点View宽
         * @param anchorH   锚点View高
         */
        void onMeasureFinished(BasePopup basePopup, int popWidth, int popHeight, int anchorW, int anchorH);
    }

}
