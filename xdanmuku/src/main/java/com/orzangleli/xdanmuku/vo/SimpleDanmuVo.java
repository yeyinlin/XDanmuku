package com.orzangleli.xdanmuku.vo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.orzangleli.xdanmuku.controller.DanmuEnqueueThread;
import com.orzangleli.xdanmuku.util.XUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2018/6/1 下午3:09
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 * <p>@version
 */

public class SimpleDanmuVo<T> implements Comparable<SimpleDanmuVo> {
    private static final int LOW_SPEED = 2;
    private static final int NORMAL_SPEED = 4;
    private static final int HIGH_SPEED = 8;
    private static final int SMALL_TEXT_SIZE = 30;
    private static final int PRIORITY_LOW = 1;
    private static final int PRIORITY_NORMAL = 2;
    private static final int PRIORITY_HIGH = 3;

    // 弹幕优先级
    private int mPriority;
    // 弹幕所在的航道
    private int mLineNum = -1;
    // 弹幕距离右边屏幕的距离
    private int mPadding = Integer.MIN_VALUE;
    // 弹幕长度
    private int mWidth = 0;
    // 弹幕高度
    private int mHeight = 0;

    // 弹幕速度
    private int mSpeed;
    // 业务弹幕数据类型
    private T mData;
    // 弹幕内容
    private String mContent;
    // 弹幕颜色
    private int mDanmuColor;
    // 弹幕字体大小
    private int mDanmuTextSize;
    // 画笔
    private Paint mDanmuPaint, mDefaultPaint;
    // 边框颜色 如果为-1则没有边框
    private int mBorderColor;
    // 边框宽度
    private int BORDER_WIDTH = 5;
    // 边框画笔
    private Paint mBorderPaint;
    // 弹幕行为, 默认从右到左
    private Behavior mBehavior = Behavior.RIGHT2LEFT;

    private Paint.FontMetricsInt mDefaultFontMetricsInt;

    private Path mPath;

    private Bitmap mCacheBitmap;

    /**
     * 弹幕行为 支持从右到左，从左到右，顶部悬停，中间悬停，底部悬停
     */
    public enum Behavior {
        RIGHT2LEFT,
        LEFT2RIGHT,
        TOP,
        BOTTOM,
        CENTER,
        CUSTOM
    }



    private static final Object sPoolSync = new Object();
    private final static int MAX_POOL_SIZE = 100;
    private static List<SimpleDanmuVo> sPool = new ArrayList<>(MAX_POOL_SIZE);

    private SimpleDanmuVo() {

    }

    public static SimpleDanmuVo obtain(String content) {
        return obtain(content, HIGH_SPEED, Color.RED, SMALL_TEXT_SIZE, null, null, PRIORITY_NORMAL, -1);
    }

    public static SimpleDanmuVo obtain(String content, int borderColor) {
        return obtain(content, HIGH_SPEED, Color.RED, SMALL_TEXT_SIZE, null, null, PRIORITY_NORMAL, borderColor);
    }

    public static SimpleDanmuVo obtain(String content, int speed, int danmuColor, int danmuTextSize, Paint danmuPaint, Object data, int priority, int borderColor) {
        SimpleDanmuVo simpleDanmuVo = null;
        synchronized (sPoolSync) {
            if (sPool != null && sPool.size() > 0) {
                simpleDanmuVo = sPool.remove(0);
            }
        }
        if (simpleDanmuVo == null) {
            simpleDanmuVo = new SimpleDanmuVo();
        }
        simpleDanmuVo.mContent = content;
        simpleDanmuVo.mSpeed = speed;
        simpleDanmuVo.mDanmuColor = danmuColor;
        simpleDanmuVo.mDanmuTextSize = danmuTextSize;
        simpleDanmuVo.mDanmuPaint = danmuPaint;
        simpleDanmuVo.mData = data;
        simpleDanmuVo.mPriority = priority;
        simpleDanmuVo.mPadding = Integer.MIN_VALUE;
        simpleDanmuVo.mLineNum = -1;
        simpleDanmuVo.mWidth = 0;
        simpleDanmuVo.mBorderColor = borderColor;
        simpleDanmuVo.mBehavior = Behavior.RIGHT2LEFT;
        simpleDanmuVo.mPath = new Path();
        XUtils.clearBitmap(simpleDanmuVo.mCacheBitmap);
        return simpleDanmuVo;
    }

    public void recycle() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                if (sPool.size() >= MAX_POOL_SIZE) {
                    sPool.remove(0);
                }
                sPool.add(this);
            }
        }
    }


    public int getLineNum() {
        return mLineNum;
    }

    public void setLineNum(int mLineNum) {
        this.mLineNum = mLineNum;
    }

    public int getPadding() {
        return mPadding;
    }

    public synchronized void setPadding(int mPadding) {
        this.mPadding = mPadding;
    }

    public int getSpeed() {
        return mSpeed;
    }

    public void setSpeed(int mSpeed) {
        this.mSpeed = mSpeed;
    }

    public T getData() {
        return mData;
    }

    public void setData(T mData) {
        this.mData = mData;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String mContent) {
        this.mContent = mContent;
    }

    public int getDanmuColor() {
        return mDanmuColor;
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        this.mBorderColor = borderColor;
    }

    public void setDanmuColor(int mDanmuColor) {
        this.mDanmuColor = mDanmuColor;
        if (mDanmuPaint != null) {
            mDanmuPaint.setColor(mDanmuColor);
        }
    }

    public int getDanmuTextSize() {
        return mDanmuTextSize;
    }

    public void setDanmuTextSize(int mDanmuTextSize) {
        this.mDanmuTextSize = mDanmuTextSize;
        if (mDanmuPaint != null) {
            mDanmuPaint.setTextSize(mDanmuTextSize);
        }
    }

    public Paint getDanmuPaint() {
        if (mDanmuPaint == null) {
            if (mDefaultPaint == null) {
                mDefaultPaint = new Paint();
            }
            mDefaultPaint.setAntiAlias(true);
            mDefaultPaint.setTextSize(mDanmuTextSize);
            mDefaultPaint.setColor(mDanmuColor);
            return mDefaultPaint;
        }
        return mDanmuPaint;
    }

    public Paint.FontMetricsInt getFontMetrics() {
        if (mDefaultFontMetricsInt == null) {
            Paint danmuPaint = getDanmuPaint();
            if (danmuPaint != null) {
                mDefaultFontMetricsInt = danmuPaint.getFontMetricsInt();
                return mDefaultFontMetricsInt;
            }
        }
        return mDefaultFontMetricsInt;
    }

    public Paint getBorderPaint() {
        if (mBorderPaint == null) {
            mBorderPaint = new Paint();
        }
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(BORDER_WIDTH);
        if (mBorderColor != -1) {
            mBorderPaint.setColor(mBorderColor);
        }
        return mBorderPaint;
    }

    public void setDanmuPaint(Paint mDanmuPaint) {
        this.mDanmuPaint = mDanmuPaint;
    }

    public Behavior getBehavior() {
        return mBehavior;
    }

    public void setBehavior(Behavior behavior) {
        this.mBehavior = behavior;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int mWidth) {
        this.mWidth = mWidth;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    @Override
    public int compareTo(@NonNull SimpleDanmuVo o) {
        return this.getPriority() - o.getPriority();
    }

    // 内置的绘制弹幕的方法

    /**
     * 绘制弹幕的终极技能：
     * 第一次使用drawText绘制这个文本，并保存在一个bitmap中
     * 之后的移动都只需要绘制这个bitmao即可。
     * @param canvas
     * @param width
     * @param height
     */
    public void drawDanmukusInternal(Canvas canvas, int width, int height) {
        if (canvas == null || this.getContent() == null || "".equals(this.getContent())) {
            return;
        }

        int laneHeight = height / DanmuEnqueueThread.MAX_LINE_NUMS;

        float x = 0, y = 0;
        if (mBehavior == Behavior.RIGHT2LEFT) {
            x = this.getPadding();
        } else if (mBehavior == Behavior.LEFT2RIGHT){
            x = this.getPadding() - mWidth;
        }
        y = laneHeight * getLineNum();

        if (mCacheBitmap == null) {
            Rect bounds = XUtils.getTextBoundsApproximately(this.getDanmuPaint().getTextSize(), this.getContent().length());
            mWidth = bounds.width();
            mHeight = bounds.height();

            // fixme 不要使用固定的 laneHeight， 可以根据文字大小，自行设置宽度，但是需要注意取对应的缓存
            mCacheBitmap = Bitmap.createBitmap(mWidth, laneHeight, Bitmap.Config.ARGB_8888);
            Canvas innerCanvas = new Canvas(mCacheBitmap);

            Paint.FontMetricsInt fontMetrics = getFontMetrics();
            if (fontMetrics == null) {
                return;
            }

            int yPos = (laneHeight - mHeight) / 2 + Math.abs(fontMetrics.ascent + fontMetrics.leading);

//            innerCanvas.drawColor(Color.RED);

            innerCanvas.drawText(this.getContent(), 0, yPos, this.getDanmuPaint());

            if (this.getBorderColor() != -1) {
                mPath.reset();
                mPath.moveTo(0, 0);
                mPath.lineTo(mWidth, 0);
                mPath.lineTo(mWidth, mHeight);
                mPath.lineTo(0, mHeight);
                mPath.lineTo(0, 0);
                canvas.drawPath(mPath, getBorderPaint());
            }
        }

        canvas.drawBitmap(mCacheBitmap, x, y, this.getDanmuPaint());
    }

    // TODO: 2018/8/8 调用此方法防止内存泄漏
    public void destroy() {
        mCacheBitmap.recycle();
        mCacheBitmap = null;
    }

}
