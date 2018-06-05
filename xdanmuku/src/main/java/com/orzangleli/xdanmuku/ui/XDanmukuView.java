package com.orzangleli.xdanmuku.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.TextureView;
import android.view.View;

import com.orzangleli.xdanmuku.controller.DanmuController;
import com.orzangleli.xdanmuku.controller.DanmuControllerImpl;
import com.orzangleli.xdanmuku.controller.DanmuMoveThread;
import com.orzangleli.xdanmuku.vo.SimpleDanmuVo;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>description：
 * <p>===============================
 * <p>creator：lixiancheng
 * <p>create time：2018/6/1 下午3:06
 * <p>===============================
 * <p>reasons for modification：
 * <p>Modifier：
 * <p>Modify time：
 * <p>@version
 */

public class XDanmukuView extends TextureView implements TextureView.SurfaceTextureListener{

    public static final int MSG_UPDATE = 1;

    // 字幕画笔
    private Paint mDanmukuPaint;
    private DanmuController<SimpleDanmuVo> mDanmuController;
    private List<DanmuDrawer> mDanmuDrawerList;
    private DanmuMoveThread mDanmuMoveThread;

    private int mWidth = -1, mHeight = -1;

    public XDanmukuView(Context context) {
        super(context);
        init(context);
    }

    public XDanmukuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public XDanmukuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        initPaint();

        this.setSurfaceTextureListener(this);

        mDanmuController = new DanmuControllerImpl();
        mDanmuDrawerList = new ArrayList<>();
        mDanmuMoveThread = new DanmuMoveThread();
        mDanmuMoveThread.setDanmuController(this, mDanmuController);
        // 设置弹幕透明度
//        this.setAlpha(1f);

        mDanmuMoveThread.start();

    }

    private void initPaint() {
        mDanmukuPaint = new Paint();
        mDanmukuPaint.setColor(Color.RED);
        mDanmukuPaint.setTextSize(30);
    }

    public synchronized void drawDanmukus() {
        if (mWidth <= 0 || mHeight <= 0) {
            return;
        }
        Canvas canvas = this.lockCanvas();
//        Log.i("lxc", "canvas ---> 为空：  " + (canvas == null));
        if (canvas != null) {
            // 清除画布
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPaint(paint);
            List<SimpleDanmuVo> workingList = mDanmuController.getWorkingList();
            if (workingList != null) {
                for (int i=0;i<workingList.size();i++) {
                    SimpleDanmuVo simpleDanmuVo = workingList.get(i);
                    if (simpleDanmuVo == null) {
                        continue;
                    }
                    if (mDanmuDrawerList.size() == 0) {
                        drawDanmukusInternal(canvas, simpleDanmuVo);
                    } else {
                        for (int j=0;j< mDanmuDrawerList.size();j++) {
                            DanmuDrawer danmuDrawer = mDanmuDrawerList.get(j);
                            if (danmuDrawer != null) {
                                int width = danmuDrawer.drawDanmu(canvas, simpleDanmuVo);
                                simpleDanmuVo.setWidth(width);
                            }
                        }
                    }
//                    mDanmuController.updateLineLastDanmuVo(simpleDanmuVo, mWidth);
                }
            }
        }
        unlockCanvasAndPost(canvas);
    }

    // 内置的绘制弹幕的方法
    private void drawDanmukusInternal(Canvas canvas, SimpleDanmuVo simpleDanmuVo) {
        if (canvas == null || simpleDanmuVo == null || simpleDanmuVo.getContent() == null || "".equals(simpleDanmuVo.getContent())) {
            return ;
        }
//        Log.i("lxc", "正在画弹幕 ---> " + simpleDanmuVo.getContent());
        canvas.drawText(simpleDanmuVo.getContent(), mWidth - simpleDanmuVo.getPadding(), (1 + simpleDanmuVo.getLineNum()) * simpleDanmuVo.getLineHeight(), simpleDanmuVo.getDanmuPaint());
        simpleDanmuVo.setWidth((int) (simpleDanmuVo.getDanmuPaint().measureText(simpleDanmuVo.getContent()) + 0.5f));
    }

    public void enqueue(SimpleDanmuVo vo) {
        mDanmuController.enqueue(vo);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE:
                    drawDanmukus();
                    break;
            }
        }
    };

    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("lxc", "onSurfaceTextureAvailable() called with: surface = [" + surface + "], width = [" + width + "], height = [" + height + "]");
        mWidth = width;
        mHeight = height;
        mDanmuMoveThread.setWidth(mWidth);
        Log.i("lxc", "mWidth ---> " + mWidth);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("lxc", "onSurfaceTextureSizeChanged() called with: surface = [" + surface + "], width = [" + width + "], height = [" + height + "]");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("lxc", "onSurfaceTextureDestroyed() called with: surface = [" + surface + "]");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    interface DanmuDrawer {
        /**
         * 返回值为弹幕的长度
         * @param canvas
         * @param simpleDanmuVo
         * @return
         */
        int drawDanmu(Canvas canvas, SimpleDanmuVo simpleDanmuVo);
    }

    public void addDanmuDrawer (DanmuDrawer danmuDrawer) {
        mDanmuDrawerList.add(danmuDrawer);
    }


}
