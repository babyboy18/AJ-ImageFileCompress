package justin.com.ajcompress;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.justin.library.AJCompress;
import com.justin.library.OnCompressListener;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import me.iwf.photopicker.PhotoPicker;

public class WaterMarkActivity extends AppCompatActivity {
    private static final String TAG = "WaterMarkActivity";
    private TextView m_tvIndex;
    private Button m_btnNext;
    private ImageView m_ivImage;
    private ArrayList<String> mImageArray;
    private File mComFile;
    private int mIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watermark);

        m_tvIndex = (TextView) findViewById(R.id.tv_image_index);
        m_btnNext = (Button) findViewById(R.id.btn_next_image);
        m_ivImage = (ImageView) findViewById(R.id.iv_image_show);
        mImageArray = new ArrayList<String>(1);

        m_btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIndex++;
                if (mIndex >= mImageArray.size()) {
                    mIndex = 0;
                }
                m_tvIndex.setText("第" + (mIndex + 1) + "张图");
                String path = mImageArray.get(mIndex);
                Glide.with(WaterMarkActivity.this).load(new File(path)).into(m_ivImage);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoPicker.builder()
                        .setPhotoCount(1)
                        .setShowCamera(true)
                        .setShowGif(true)
                        .setPreviewEnabled(false)
                        .start(WaterMarkActivity.this, PhotoPicker.REQUEST_CODE);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);

                compress(new File(photos.get(0)));
            }
        }
    }

    private void compress(File file) {
        AJCompress.create(this)
                .loadFile(file)
                .setLevel(AJCompress.FIRST_LEVEL)
                .setOnCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        mComFile = file;
                        mImageArray.clear();
                        mImageArray.add(file.getAbsolutePath());
                        addWaterMark();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).run();
    }

    private void addWaterMark() {
        waterMarkOne();
        waterMarkSecond();
        waterMarkThree();

        mIndex = 0;
        m_tvIndex.setText("第" + mIndex + 1 + "张图");
        String path = mImageArray.get(mIndex);
        Glide.with(WaterMarkActivity.this).load(new File(path)).into(m_ivImage);
    }

    private void waterMarkOne() {
        Bitmap tempBitmap = initWatermark(BitmapFactory.decodeFile(mComFile.getAbsolutePath()),
                R.drawable.icon_photo_watermark);
        if (tempBitmap != null) {
            String savePath = mComFile.getAbsolutePath() + "_1";
            saveBitmap(tempBitmap, savePath, 100, Bitmap.CompressFormat.PNG);
            mImageArray.add(savePath);
        }
    }

    public boolean saveBitmap(Bitmap b, String absolutePath, int quality, Bitmap.CompressFormat format) {
        String fileName = absolutePath;
        File f = new File(fileName);
        FileOutputStream fOut = null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(format, quality, stream);
        try {
            f.createNewFile();
            fOut = new FileOutputStream(f);
            fOut.write(stream.toByteArray());
            fOut.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            closeIO(fOut, stream);
        }
        return false;
    }

    public Bitmap initWatermark(Bitmap bm, int resId) {
        if (bm == null) {
            return null;
        }

        Resources res = getResources();
        Bitmap m_watermarkBitmap = BitmapFactory.decodeResource(res, resId);
        int m_bmpWidth = bm.getWidth();
        int m_bmpHeight = bm.getHeight();
        int m_wmWidth = m_watermarkBitmap.getWidth();
        int m_wmHeight = m_watermarkBitmap.getHeight();

        float drawWaterWidth = m_bmpWidth * 0.3f;
        float scale = drawWaterWidth / m_wmWidth;


        // 绘制新的bitmap
        Canvas m_newCanvas = null;
        try {
            Bitmap m_newBitmap = Bitmap.createBitmap(m_bmpWidth, m_bmpHeight, Bitmap.Config.ARGB_8888);
            m_newCanvas = new Canvas(m_newBitmap);
            m_newCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            m_newCanvas.drawBitmap(bm, 0, 0, null);

            m_watermarkBitmap = scaleWithXY(m_watermarkBitmap, scale, scale);
            m_newCanvas.drawBitmap(m_watermarkBitmap, m_bmpWidth - drawWaterWidth, 0, new Paint());

            m_newCanvas.save(Canvas.ALL_SAVE_FLAG);
            m_newCanvas.restore();
            return m_newBitmap;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }


    private void waterMarkSecond() {
        Bitmap bitmap = BitmapFactory.decodeFile(mComFile.getAbsolutePath());
        Bitmap m_watermarkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_photo_watermark);

        int m_bmpWidth = bitmap.getWidth();
        int m_bmpHeight = bitmap.getHeight();
        int m_wmWidth = m_watermarkBitmap.getWidth();
        int m_wmHeight = m_watermarkBitmap.getHeight();

        float drawWaterWidth = m_bmpWidth * 0.3f;
        float scale = drawWaterWidth / m_wmWidth;
        Bitmap icon = Bitmap.createBitmap(m_bmpWidth, m_bmpHeight, Bitmap.Config.ARGB_8888); // 建立一个空的BItMap
        Canvas canvas = new Canvas(icon);// 初始化画布绘制的图像到icon上
        Paint photoPaint = new Paint(); // 建立画笔
        photoPaint.setDither(true); // 获取跟清晰的图像采样
        photoPaint.setFilterBitmap(true);// 过滤一些
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());// 创建一个指定的新矩形的坐标
        Rect dst = new Rect(0, 0, m_bmpWidth, m_bmpHeight);// 创建一个指定的新矩形的坐标
        canvas.drawBitmap(bitmap, src, dst, photoPaint);// 将photo 缩放或则扩大到
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);// 设置画笔
//        textPaint.setTextSize(40.0f);// 字体大小
//        textPaint.setTypeface(Typeface.DEFAULT_BOLD);// 采用默认的宽度
//        textPaint.setColor(Color.RED);// 采用的颜色
        // textPaint.setShadowLayer(3f, 1,
        // 1,this.getResources().getColor(android.R.color.background_dark));//影音的设置
        int ScreenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int ScreenHeight = getWindowManager().getDefaultDisplay().getHeight();
        canvas.drawBitmap(m_watermarkBitmap, m_bmpWidth - drawWaterWidth, 0, textPaint);// 绘制上去字，开始未知x,y采用那只笔绘制
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        // image.setImageBitmap(icon);
        String savePath = mComFile.getAbsolutePath() + "_2";
        saveFile(icon, savePath);

        mImageArray.add(savePath);
    }

    public void saveFile(Bitmap bitmap, String fileName) {
        File dirFile = new File(fileName);
        // 检测图片是否存在
        if (dirFile.exists()) {
            dirFile.delete(); // 删除原图片
        }
        File myCaptureFile = new File(fileName);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            // 100表示不进行压缩，70表示压缩率为30%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void waterMarkThree() {
        Bitmap bitmap = BitmapFactory.decodeFile(mComFile.getAbsolutePath());
        if (bitmap == null) {
            return ;
        }
        Bitmap watermark = BitmapFactory.decodeResource(getResources(), R.drawable.icon_photo_watermark);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        // 需要处理图片太大造成的内存超过的问题,这里我的图片很小所以不写相应代码了
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        cv.drawBitmap(bitmap, 0, 0, null);// 在 0，0坐标开始画入src
        // 加入图片
        if (watermark != null) {
            int ww = watermark.getWidth();
            int wh = watermark.getHeight();
            Rect src = new Rect();// 图片
            Rect dst = new Rect();// 屏幕位置及尺寸
            src.left = 0; // 0,0
            src.top = 0;
            src.right = w;// 是桌面图的宽度，
            src.bottom = h;// 是桌面图的高度
            // 下面的 dst 是表示 绘画这个图片的位置
            dst.left = 0; // 绘图的起点X位置
            dst.top = 0; // 相当于 桌面图片绘画起点的Y坐标
            dst.right = ww + w - 60; // 表示需绘画的图片的右上角
            dst.bottom = wh + h - 60; // 表示需绘画的图片的右下角
            cv.drawBitmap(watermark, src, dst, null);// 在src的右下角画入水印
            src = null;
            dst = null;
        }
        // 加入文字
        String title = "第3个";
        if (title != null) {
            String familyName = "宋体";
            Typeface font = Typeface.create(familyName, Typeface.BOLD);
            TextPaint textPaint = new TextPaint();
            textPaint.setColor(Color.RED);
            textPaint.setTypeface(font);
            textPaint.setTextSize(22);
            textPaint.setAlpha(50);
            cv.drawText(title, 140, 200, textPaint);
        }
        cv.save(Canvas.ALL_SAVE_FLAG);// 保存
        cv.restore();// 存储
        watermark.recycle();
        bitmap.recycle();
        String savePath = mComFile.getAbsolutePath() + "_3";
        saveBitmap(newb, savePath, 100, Bitmap.CompressFormat.JPEG);
        mImageArray.add(savePath);
        newb.recycle();
    }

    public static void closeIO(Closeable... closeables) {
        if (null == closeables || closeables.length <= 0) {
            return;
        }
        for (Closeable cb : closeables) {
            try {
                if (null == cb) {
                    continue;
                }
                cb.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public static Bitmap scaleWithXY(Bitmap src, float scaleX,
                                     float scaleY) {
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
    }
}
