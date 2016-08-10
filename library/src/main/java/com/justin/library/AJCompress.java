package com.justin.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class AJCompress {

    public static final int FIRST_LEVEL = 1;
    public static final int SECOND_LEVEL = 2;

    private static final String TAG = "AJCompress";
    public static String DEFAULT_DISK_CACHE_DIR = "ajcompress_diskcache";

    private final File mCacheDir;

    private OnCompressListener mCompressListener;
    private File mFile;
    private int mLevel = FIRST_LEVEL;

    private int mQuality = 100;
    private boolean mIsAutoQuality = true;

    // 单位为kb
    private final int SIZE_LEVEL_60 = 60;
    private final int SIZE_LEVEL_80 = 80;
    private final int SIZE_LEVEL_100 = 100;
    private final int SIZE_LEVEL_120 = 120;
    private final int SIZE_LEVEL_150 = 150;
    private final int SIZE_LEVEL_200 = 200;
    private final int SIZE_LEVEL_300 = 300;
    private final int SIZE_LEVEL_500 = 500;

    /**
     * 构造函数
     * Create a AJCompress object.
     * @param cacheDir
     */
    AJCompress(File cacheDir) {
        mCacheDir = cacheDir;
    }

    /**
     * 返回压缩图片保存路径
     * Returns a directory with a default cache path in the private cache directory of the application to use to store
     * retrieved image.
     *
     * @param context A context.
     * @see #getPhotoCacheDir(Context, String)
     */
    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, AJCompress.DEFAULT_DISK_CACHE_DIR);
    }

    /**
     * 返回压缩图片保存路径
     * Returns a directory with the given path in the private cache directory of the application to use to store
     * retrieved image.
     *
     * @param context A context.
     * @param cacheName The name of the subdirectory in which to store the cache.
     * @see #getPhotoCacheDir(Context)
     */
    public static File getPhotoCacheDir(Context context, String cacheName) {
        File cacheDir = context.getCacheDir();
        if (cacheDir != null) {
            File result = new File(cacheDir, cacheName);
            if (result != null && result.exists()) {
                if (result.isDirectory()) {
                    return result;
                } else if (result.isFile()) {
                    result.delete();
                    if (result.mkdirs()) {
                        return result;
                    }
                }
            }
        }
        if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "default disk cache dir is null");
        }
        return null;
    }

    /**
     * 创建一个压缩器实例
     * Returns a directory with a default path in the private cache directory of the application to use to store
     * retrieved image.
     *
     * @param context A context.
     * @see #getPhotoCacheDir(Context, String)
     */
    public static AJCompress create(Context context) {
        return new AJCompress(AJCompress.getPhotoCacheDir(context));
    }

    /**
     * 开始压缩图片
     * @return
     */
    public AJCompress run() {
        if (mCompressListener != null) {
            mCompressListener.onStart();
        }

        this.asObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (mCompressListener != null) mCompressListener.onError(throwable);
                    }
                })
                .onErrorResumeNext(Observable.<File>empty())
                .filter(new Func1<File, Boolean>() {
                    @Override
                    public Boolean call(File file) {
                        return file != null;
                    }
                })
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        if (mCompressListener != null) mCompressListener.onSuccess(file);
                    }
                });

        return this;
    }

    /**
     * 设置需要压缩的图片
     * @param file
     * @return
     */
    public AJCompress loadFile(File file) {
        mFile = file;
        return this;
    }

    /**
     * 设置压缩监听器
     * @param listener
     * @return
     */
    public AJCompress setOnCompressListener(OnCompressListener listener) {
        mCompressListener = listener;
        return this;
    }

    /**
     * 设置压缩级别
     * @param level
     * @return
     */
    public AJCompress setLevel(int level) {
        mLevel = level;
        return this;
    }

    /**
     * 设置压缩质量
     * @param quality
     * @return
     */
    public AJCompress setQuality(int quality) {
        if (quality <= 0) {
            mQuality = 0;
        } else if (quality >= 100) {
            mQuality = 100;
        }

        mQuality = quality;
        mIsAutoQuality = false;
        return this;
    }

    public Observable<File> asObservable() {
        if (mLevel == SECOND_LEVEL) {
            return Observable.just(secondCompress(mFile));
        } else if (mLevel == FIRST_LEVEL) {
            return Observable.just(firstCompress(mFile));
        } else {
            return Observable.empty();
        }
    }

    /**
     * 不使用线程进行压缩出来,使用此函数需要配合线程一起使用.
     * @return 要缩后的图片地址
     */
    public File compress() {
        if (mLevel == SECOND_LEVEL) {
            return secondCompress(mFile);
        }else if (mLevel == FIRST_LEVEL) {
            return firstCompress(mFile);
        } else {
            return null;
        }
    }

    /**
     * 二级压缩规则
     * @param file
     * @return
     */
    private File secondCompress(File file) {
        try {
            int minSize = SIZE_LEVEL_60;
            int longSide = 720;
            int shortSide = 1280;

            String filePath = file.getAbsolutePath();
            String thumbFilePath = mCacheDir.getAbsolutePath() + "/" + System.currentTimeMillis();
            long size = 0;
            long maxSize = file.length() / 5;

            int angle = getImageSpinAngle(filePath);
            int[] imgSize = getImageSize(filePath);
            int width = 0, height = 0;
            if (imgSize[0] <= imgSize[1]) {
                double scale = (double) imgSize[0] / (double) imgSize[1];
                if (scale <= 1.0 && scale > 0.5625) {
                    width = imgSize[0] > shortSide ? shortSide : imgSize[0];
                    height = width * imgSize[1] / imgSize[0];
                    size = minSize;
                } else if (scale <= 0.5625) {
                    height = imgSize[1] > longSide ? longSide : imgSize[1];
                    width = height * imgSize[0] / imgSize[1];
                    size = maxSize;
                }
            } else {
                double scale = (double) imgSize[1] / (double) imgSize[0];
                if (scale <= 1.0 && scale > 0.5625) {
                    height = imgSize[1] > shortSide ? shortSide : imgSize[1];
                    width = height * imgSize[0] / imgSize[1];
                    size = minSize;
                } else if (scale <= 0.5625) {
                    width = imgSize[0] > longSide ? longSide : imgSize[0];
                    height = width * imgSize[1] / imgSize[0];
                    size = maxSize;
                }
            }

            return compress(filePath, thumbFilePath, width, height, angle, size);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 一级压缩规则
     * @param file
     * @return
     */
    private File firstCompress(File file) {
        try {
            String filePath = file.getAbsolutePath();
            String thumbPath = mCacheDir.getAbsolutePath() + "/" + System.currentTimeMillis();

            double size;

            int angle = getImageSpinAngle(filePath);
            int[] imgSize = getImageSize(filePath);
            int width = imgSize[0];
            int height = imgSize[1];
            int thumbW = width % 2 == 1 ? width + 1 : width;
            int thumbH = height % 2 == 1 ? height + 1 : height;

            int tmpWidth = thumbW > thumbH ? thumbH : thumbW;
            int tmpHeight = thumbW > thumbH ? thumbW : thumbH;

            double scale = ((double) tmpWidth / tmpHeight);

            if (scale <= 1 && scale > 0.5625) {
                if (height < 1664) {
                    size = (width * height) / Math.pow(1664, 2) * SIZE_LEVEL_150;
                    size = size < SIZE_LEVEL_80 ? SIZE_LEVEL_80 : size;
                } else if (height >= 1664 && height < 4990) {
                    thumbW = width / 2;
                    thumbH = height / 2;
                    size = (thumbW * thumbH) / Math.pow(2495, 2) * SIZE_LEVEL_300;
                    size = size < SIZE_LEVEL_80 ? SIZE_LEVEL_80 : size;
                } else if (height >= 4990 && height < 10240) {
                    thumbW = width / 4;
                    thumbH = height / 4;
                    size = (thumbW * thumbH) / Math.pow(2560, 2) * SIZE_LEVEL_300;
                    size = size < SIZE_LEVEL_120 ? SIZE_LEVEL_120 : size;
                } else {
                    int multiple = height / 1280 == 0 ? 1 : height / 1280;
                    thumbW = width / multiple;
                    thumbH = height / multiple;
                    size = (thumbW * thumbH) / Math.pow(2560, 2) * SIZE_LEVEL_300;
                    size = size < SIZE_LEVEL_120 ? SIZE_LEVEL_120 : size;
                }
            } else if (scale <= 0.5625 && scale > 0.5) {
                int multiple = height / 1280 == 0 ? 1 : height / 1280;
                thumbW = width / multiple;
                thumbH = height / multiple;
                size = (thumbW * thumbH) / (1440.0 * 2560.0) * SIZE_LEVEL_200;
                size = size < SIZE_LEVEL_120 ? SIZE_LEVEL_120 : size;
            } else {
                int multiple = (int) Math.ceil(height / (1280.0 / scale));
                thumbW = width / multiple;
                thumbH = height / multiple;
                size = ((thumbW * thumbH) / (1280.0 * (1280 / scale))) * SIZE_LEVEL_500;
                size = size < SIZE_LEVEL_120 ? SIZE_LEVEL_120 : size;
            }

            return compress(filePath, thumbPath, thumbW, thumbH, angle, (long) size);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取图片的长和宽
     * obtain the image's width and height
     *
     * @param imagePath the path of image
     */
    public int[] getImageSize(String imagePath) {
        int[] res = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        BitmapFactory.decodeFile(imagePath, options);

        res[0] = options.outWidth;
        res[1] = options.outHeight;

        return res;
    }

    /**
     * 获取缩略图
     * obtain the thumbnail that specify the size
     *
     * @param imagePath the target image path
     * @param width     the width of thumbnail
     * @param height    the height of thumbnail
     * @return {@link Bitmap}
     */
    private Bitmap compress(String imagePath, int width, int height) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            int outH = options.outHeight;
            int outW = options.outWidth;
            int inSampleSize = 1;

            if (outH > height || outW > width) {
                int halfH = outH / 2;
                int halfW = outW / 2;

                while ((halfH / inSampleSize) > height && (halfW / inSampleSize) > width) {
                    inSampleSize *= 2;
                }
            }

            options.inSampleSize = inSampleSize;

            options.inJustDecodeBounds = false;

            int heightRatio = (int) Math.ceil(options.outHeight / (float) height);
            int widthRatio = (int) Math.ceil(options.outWidth / (float) width);

            if (heightRatio > 1 || widthRatio > 1) {
                if (heightRatio > widthRatio) {
                    options.inSampleSize = heightRatio;
                } else {
                    options.inSampleSize = widthRatio;
                }
            }
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(imagePath, options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获得图片的旋转角度
     * obtain the image rotation angle
     *
     * @param path path of target image
     */
    private int getImageSpinAngle(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 指定参数压缩图片
     * create the thumbnail with the true rotate angle
     *
     * @param largeImagePath the big image path
     * @param thumbFilePath  the thumbnail path
     * @param width          width of thumbnail
     * @param height         height of thumbnail
     * @param angle          rotation angle of thumbnail
     * @param size           the file size of image
     */
    private File compress(String largeImagePath, String thumbFilePath, int width, int height, int angle, long size) {
        Bitmap thbBitmap = null;
        try {
            thbBitmap = compress(largeImagePath, width, height);

            thbBitmap = rotatingImage(angle, thbBitmap);

            return saveImage(thumbFilePath, thbBitmap, size);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (thbBitmap != null && !thbBitmap.isRecycled()) {
                thbBitmap.recycle();
            }
        }

        return null;
    }

    /**
     * 旋转图片
     * rotate the image with specified angle
     *
     * @param angle  the angle will be rotating 旋转的角度
     * @param bitmap target image               目标图片
     */
    private static Bitmap rotatingImage(int angle, Bitmap bitmap) {
        //rotate image
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        //create a new image
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 保存图片到指定路径
     * Save image with specified size
     *
     * @param filePath the image file save path 储存路径
     * @param bitmap   the image what be save   目标图片
     * @param size     the file size of image   期望大小
     */
    private File saveImage(String filePath, Bitmap bitmap, long size) {
        Log.e(TAG, "-----1----size:" + size);

        File result = new File(filePath.substring(0, filePath.lastIndexOf("/")));

        if (!result.exists() && !result.mkdirs()) return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int quality = mQuality;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        if (mIsAutoQuality) {
            int totalLength = stream.toByteArray().length;
            quality = getCompressQuality((double) totalLength / 1024);
            Log.e(TAG, "-----2----size:" + size + "----length:" + stream.toByteArray().length);
            while (stream.toByteArray().length / 1024 > size) {
                stream.reset();
                quality -= 6;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                Log.e(TAG, "-----3----size:" + size + "----length:" + stream.toByteArray().length + "---options:" + quality);
                Log.e(TAG, "-----4----size:" + size + "----length/totalLength:" + (double) stream.toByteArray().length / (double) totalLength);
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            fos.write(stream.toByteArray());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new File(filePath);
    }

    private int getCompressQuality(double length) {
        Log.e(TAG, "length:" + length);
        if (mIsAutoQuality) {
            if (length > 3072) {
                return 30;
            } else if (length > 2048) {
                return 36;
            } else if (length > 1400) {
                return 46;
            } else if (length > 1024) {
                return 50;
            } else if (length > 800) {
                return 70;
            }
        }

        return mQuality;
    }
}