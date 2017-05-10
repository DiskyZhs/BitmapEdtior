package cn.com.zhs.bitmapeditordemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Bitmap编辑封装工具类(你可能需要申请读写外部存储权限)
 * Created by ZhangHaoSong on 2017/03/25 0025.
 */

public class BitmapEditorForRxjava {
    /**
     * 上下文
     */
    private Context context;

    /**
     * 基础的Bmp
     */
    private Bitmap srcBmp;

    /**
     * 处理完毕的Bitmap
     */
    private Bitmap resultBmp;

    /**
     * 输入的Bitmap的格式
     */
    private BitmapDataSource mSource;

    /**
     * 期望的Size
     */
    private Size desireSize;

    /**
     * 期望的宽高比
     */
    private float desireRatio = 0;

    /**
     * 是否保持src的宽高比
     */
    private boolean keepSrcRatio = false;

    /**
     * 当遇到分辨率缩放时，缩放后的分辨率是大于给定分辨率还是小于给定分辨率
     */
    private boolean isLargerResolution;

    /**
     * 质量衰减间隔（缩放质量时每次缩减的质量间隔）
     */
    private int qualityInterval = 10;

    /**
     * 设置处理后图片的大小限制（通过修改Bitmap的质量）
     */
    private int bmpLimitedSize = -1;

    /**
     * 设置颜色格式
     */
    private int colorMode = 0;

    /**
     * 是否异步处理所有图像编辑
     */
    private boolean isAsync = false;

    /**
     * 处理监听器
     */
    private BitmapEditorListener mListener;

    /**
     * 使用Rxjava，异步处理时
     */
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    /**
     * 颜色格式rgb565
     */
    public static final int COLOR_MODE_RGB565 = 1;

    /**
     * 颜色格式argb8888
     */
    public static final int COLOR_MODE_ARGB8888 = 2;

    /**
     * 构造函数
     */
    private BitmapEditorForRxjava() {
    }

    /**
     * 初始化Editor
     */
    public static BitmapEditorForRxjava init() {
        return new BitmapEditorForRxjava();
    }

    /**
     * 给BitmapEditor赋予Bmp
     *
     * @param filePtah
     * @return
     */
    public BitmapEditorForRxjava from(String filePtah) {
        mSource = new BitmapDataSource();
        mSource.mode = BitmapDataSourceMode.FROM_FILE;
        mSource.file = new File(filePtah);
        return this;
    }

    /**
     * 图片资源来自文件
     *
     * @param file
     * @return
     */
    public BitmapEditorForRxjava from(File file) {
        mSource = new BitmapDataSource();
        mSource.mode = BitmapDataSourceMode.FROM_FILE;
        mSource.file = file;
        return this;
    }

    /**
     * 图片资源来data
     *
     * @param bmpData
     * @return
     */
    public BitmapEditorForRxjava from(byte[] bmpData) {
        mSource = new BitmapDataSource();
        mSource.mode = BitmapDataSourceMode.FROM_DATA;
        mSource.data = bmpData;
        return this;
    }

    /**
     * 图片资源来Buffer
     *
     * @param bmpBuffer
     * @param offset
     * @param length
     * @return
     */
    public BitmapEditorForRxjava from(ByteBuffer bmpBuffer, int offset, int length) {
        mSource = new BitmapDataSource();
        mSource.mode = BitmapDataSourceMode.FROM_BUFFER;
        mSource.data = new byte[length];
        bmpBuffer.get(mSource.data, offset, length);
        return this;
    }

    /**
     * 图片资源来Buffer
     *
     * @param bmpBuffer
     * @return
     */
    public BitmapEditorForRxjava from(ByteBuffer bmpBuffer) {
        return from(bmpBuffer, 0, bmpBuffer.remaining());
    }

    /**
     * 图片资源来Bmp
     *
     * @param bmp
     * @return
     */
    public BitmapEditorForRxjava from(Bitmap bmp) {
        mSource = new BitmapDataSource();
        mSource.mode = BitmapDataSourceMode.FROM_BITMAP;
        mSource.bmp = bmp;
        return this;
    }

    /**
     * 图片资源来资源文件
     *
     * @param resId
     * @return
     */
    public BitmapEditorForRxjava from(int resId, Context context) {
        this.context = context;
        mSource = new BitmapDataSource();
        mSource.mode = BitmapDataSourceMode.FROM_RES;
        mSource.resId = resId;
        return this;
    }

    /**
     * 设置监听
     *
     * @param listener
     * @return
     */
    public BitmapEditorForRxjava listener(BitmapEditorListener listener) {
        mListener = listener;
        return this;
    }

    /**
     * 设置分辨率，直接将原有Bitmap进行缩放，不考虑给定的长宽比直接缩放
     *
     * @param width
     * @param height
     * @return
     */
    public BitmapEditorForRxjava paserResolution(int width, int height) {
        desireSize = new Size(width, height);
        keepSrcRatio = false;
        return this;
    }

    /**
     * 保证给定长宽比的情况下，进行分辨率缩放
     *
     * @param ratio              width/height
     * @param isLargerResolution 设置缩放后的分辨率 是比需要设置的分辨率大还是小
     * @return
     */
    public BitmapEditorForRxjava paserResolutionKeepRatio(int width, int height, float ratio, boolean isLargerResolution) {
        desireSize = new Size(width, height);
        this.desireRatio = ratio;
        this.isLargerResolution = isLargerResolution;
        keepSrcRatio = false;
        return this;
    }

    /**
     * 保证所给定长宽比的情况下，进行分辨率缩放
     */
    public BitmapEditorForRxjava paserResolutionKeepRatio(int width, int height) {
        desireSize = new Size(width, height);
        keepSrcRatio = true;
        return this;
    }

    /**
     * 设置长宽比,一次来缩放
     */
    public BitmapEditorForRxjava setDesireRatio(float desireRatio) {
        this.desireRatio = desireRatio;
        return this;
    }

    /**
     * 设置按照长宽比缩放的时候大于Size还是小于Size
     *
     * @param isLargerResolution
     * @return
     */
    public BitmapEditorForRxjava setResolutionLarger(boolean isLargerResolution) {
        this.isLargerResolution = isLargerResolution;
        return this;
    }


    /**
     * 通过改变质量,来限制Bitmap的大小(只对Jpeg有效，因为PNG为无损，无法压缩)
     *
     * @param bmpSize
     */
    public BitmapEditorForRxjava limitSize(int bmpSize) {
        this.bmpLimitedSize = bmpSize;
        Log.e("BitmapEditor", "bmpLimitedSize = " + bmpLimitedSize);
        return this;
    }

    /**
     * 设置Bitmap颜色格式（ARGB_8888，RGB_565）
     */
    public BitmapEditorForRxjava setColorMode(int colorMode) {
        this.colorMode = colorMode;
        return this;
    }

    /**
     * 设置是否为异步处理Bitmap
     */
    public BitmapEditorForRxjava async() {
        this.isAsync = true;
        return this;
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回结果请设置回调，在回调中取得
     */
    public void asBmp() {
        if (isAsync) {
            asBmpAsync();
        } else {
            asBmpSync();
        }
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回结果请设置回调，在回调中取得
     */
    public void asFile(String filePath) {
        if (isAsync) {
            asFileAsync(filePath);
        } else {
            asFileSync(filePath);
        }
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回结果请设置回调，在回调中取得
     */
    public void asByteArray() {
        if (isAsync) {
            asByteArrayAsync();
        } else {
            asByteArraySync();
        }
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回结果请设置回调，在回调中取得(异步处理/非阻塞)
     */
    public void asBmpAsync() {
        mCompositeSubscription.add(Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        asBmpSync();
                        subscriber.onNext("bitmapFinish");
                    }
                })
                        .subscribeOn(Schedulers.computation())
                        .subscribe(new Action1<String>() {
                                       @Override
                                       public void call(String s) {
                                           Log.e("BitmapEditor", "s");
                                       }
                                   },
                                new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        if (mListener != null) {
                                            mListener.OnError(throwable);
                                        }
                                    }
                                })
        );
        mCompositeSubscription.unsubscribe();
        if (mListener != null) {
            mListener.onComplete();
        }
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回结果请设置回调，在回调中取得(异步处理/非阻塞)
     */
    public void asFileAsync(String filePath) {
        final String file = filePath;
        mCompositeSubscription.add(Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        asFileSync(file);
                        subscriber.onNext("bitmapFinish");
                    }
                })
                        .subscribeOn(Schedulers.computation())
                        .subscribe(new Action1<String>() {
                                       @Override
                                       public void call(String s) {
                                           Log.e("BitmapEditor", "s");
                                       }
                                   },
                                new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        if (mListener != null) {
                                            mListener.OnError(throwable);
                                        }
                                    }
                                })
        );
        mCompositeSubscription.unsubscribe();
        if (mListener != null) {
            mListener.onComplete();
        }
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回结果请设置回调，在回调中取得(异步处理/非阻塞)
     */
    public void asByteArrayAsync() {
        mCompositeSubscription.add(Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        asByteArraySync();
                        subscriber.onNext("bitmapFinish");
                    }
                })
                        .subscribeOn(Schedulers.computation())
                        .subscribe(new Action1<String>() {
                                       @Override
                                       public void call(String s) {
                                           Log.e("BitmapEditor", "s");
                                       }
                                   },
                                new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        if (mListener != null) {
                                            mListener.OnError(throwable);
                                        }
                                    }
                                })
        );
        mCompositeSubscription.unsubscribe();
        if (mListener != null) {
            mListener.onComplete();
        }
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回最终处理后的Bmp(同步处理/阻塞)
     */
    public Bitmap asBmpSync() {
        try {
            long startPts = System.currentTimeMillis();
            //判断条件
            checkEditorReady();
            if (mListener != null) {
                mListener.onEditorStart();
            }
            //将不同数据源的图片数据获取成为srcBitmap,根据设置的颜色格式
            srcBmp = getSrcBmpFromDataSource(colorMode);
            //处理分辨率缩放
            resultBmp = setResolution(srcBmp, desireRatio, desireSize, isLargerResolution);
            //质量压缩处理
            resultBmp = getBmpResultForBitmap(resultBmp, bmpLimitedSize);
            if (mListener != null) {
                mListener.onEditorEnd(resultBmp, System.currentTimeMillis() - startPts);
            }
            return resultBmp;
        } catch (Exception e) {
            if (mListener != null) {
                mListener.OnError(e);
            }
        }
        if (mListener != null) {
            mListener.onComplete();
        }
        return resultBmp;
    }

    /**
     * 根据设置参数来开始处理Bitmap，返回最终处理结果写入File(同步处理/阻塞)
     */
    public File asFileSync(String filePath) {
        try {
            long startPts = System.currentTimeMillis();
            //判断条件
            checkEditorReady();
            if (mListener != null) {
                mListener.onEditorStart();
            }
            //将不同数据源的图片数据获取成为srcBitmap,根据设置的颜色格式
            srcBmp = getSrcBmpFromDataSource(colorMode);
            Log.e("BitmapEditor", "srcBmp length =" + srcBmp.getByteCount());
            //处理分辨率缩放
            resultBmp = setResolution(srcBmp, desireRatio, desireSize, isLargerResolution);
            Log.e("BitmapEditor", "resultBmp length =" + resultBmp.getByteCount());
            //质量压缩处理
            File file = getFileResultForBitmap(resultBmp, bmpLimitedSize, filePath);
            if (mListener != null) {
                mListener.onEditorEnd(file, System.currentTimeMillis() - startPts);
            }
            return file;
        } catch (Exception e) {
            if (mListener != null) {
                mListener.OnError(e);
            }
        }
        if (mListener != null) {
            mListener.onComplete();
        }
        return null;
    }


    /**
     * 根据设置参数来开始处理Bitmap，返回最终处理结果生成数据(同步处理/阻塞)
     */
    public byte[] asByteArraySync() {
        try {
            long startPts = System.currentTimeMillis();
            //判断条件
            checkEditorReady();
            if (mListener != null) {
                mListener.onEditorStart();
            }
            //将不同数据源的图片数据获取成为srcBitmap,根据设置的颜色格式
            srcBmp = getSrcBmpFromDataSource(colorMode);
            //处理分辨率缩放
            resultBmp = setResolution(srcBmp, desireRatio, desireSize, isLargerResolution);
            //质量压缩处理
            byte[] data = getByteArrayResultForBitmap(resultBmp, bmpLimitedSize);
            if (mListener != null) {
                mListener.onEditorEnd(data, System.currentTimeMillis() - startPts);
            }
            return data;
        } catch (Exception e) {
            if (mListener != null) {
                mListener.OnError(e);
            }
        }
        if (mListener != null) {
            mListener.onComplete();
        }
        return null;
    }

    /**
     * Bitmap处理过程监听
     */
    public interface BitmapEditorListener {
        /**
         * 处理开始
         */
        void onEditorStart();

        /**
         * Bitmap 处理完毕
         *
         * @param data     处理结果Bitmap
         * @param timeCost 处理所花时间
         */
        void onEditorEnd(Bitmap data, long timeCost);

        /**
         * Bitmap 处理完毕存储为文件
         *
         * @param file     处理结果写入文件
         * @param timeCost 处理所花时间
         */
        void onEditorEnd(File file, long timeCost);

        /**
         * Bitmap 处理完毕
         *
         * @param data     处理结果Byte数组
         * @param timeCost 处理所花时间
         */
        void onEditorEnd(byte[] data, long timeCost);

        /**
         * Bitmap 处理出错
         *
         * @param e 错误异常
         */
        void OnError(Throwable e);

        /**
         * 整个编辑过程完毕（如果是异步处理则进行了线程的释放）
         */
        void onComplete();
    }

    /**
     * 长宽尺寸
     */
    public class Size {
        int width;
        int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return "Size{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    /**
     * Bitmap数据源
     */
    private class BitmapDataSource {
        BitmapDataSourceMode mode;
        File file;
        byte[] data;
        Bitmap bmp;
        int resId;

        @Override
        public String toString() {
            return "BitmapDataSource{" +
                    "mode=" + mode +
                    ", file=" + file +
                    ", data=" + Arrays.toString(data) +
                    ", bmp=" + bmp +
                    ", resId=" + resId +
                    '}';
        }
    }

    /**
     * bitmap 存储或者输出的数据源的格式
     */
    private enum BitmapDataSourceMode {
        FROM_FILE, FROM_DATA, FROM_BUFFER, FROM_BITMAP, FROM_RES
    }

    /**
     * 没有设置数据源异常
     */
    public class NoSourceException extends Exception {
        public NoSourceException() {
            super("you should set dataSource,for editor");
        }
    }

    /**
     * 检测Editor的上下文以及Source是否设置
     *
     * @throws NoSourceException
     */
    private void checkEditorReady() throws NoSourceException {
        //暂时未想好怎么处理
        if (mSource == null) {
            throw new NoSourceException();
        }
    }

    /**
     * 获取Source Bmp，根据设定的颜色格式
     */
    private Bitmap getSrcBmpFromDataSource(int colorModel) {
        //根据颜色格式设置Bitmap option
        BitmapFactory.Options option = new BitmapFactory.Options();
        if (colorModel == BitmapEditorForRxjava.COLOR_MODE_ARGB8888) {
            option.inPreferredConfig = Bitmap.Config.ARGB_8888;
        } else if (colorModel == BitmapEditorForRxjava.COLOR_MODE_RGB565) {
            option.inPreferredConfig = Bitmap.Config.RGB_565;
        } else { //未设置颜色格式
            option = null;
        }
        Bitmap data = null;
        Log.e("BitmapEditor", "getSrcBmpFromDataSource option =" + option);
        //解析DataSource转化为Bmp
        switch (mSource.mode) {
            case FROM_FILE:
                if (option == null)
                    data = BitmapFactory.decodeFile(mSource.file.getAbsolutePath());
                else
                    data = BitmapFactory.decodeFile(mSource.file.getAbsolutePath(), option);
                break;
            case FROM_DATA:
                if (option == null)
                    data = BitmapFactory.decodeByteArray(mSource.data, 0, mSource.data.length);
                else
                    data = BitmapFactory.decodeByteArray(mSource.data, 0, mSource.data.length, option);
                break;
            case FROM_BUFFER:
                if (option == null)
                    data = BitmapFactory.decodeByteArray(mSource.data, 0, mSource.data.length);
                else
                    data = BitmapFactory.decodeByteArray(mSource.data, 0, mSource.data.length, option);
                break;
            case FROM_BITMAP:
                data = mSource.bmp;
                break;
            case FROM_RES:
                if (option == null)
                    data = BitmapFactory.decodeResource(context.getResources(), mSource.resId);
                else
                    data = BitmapFactory.decodeResource(context.getResources(), mSource.resId, option);
                break;
            default:
                data = mSource.bmp;
                break;
        }
        return data;
    }

    /**
     * 根据设定的条件进行分辨率的缩放
     * Resolution
     *
     * @param src          源图片
     * @param ratio        设定的宽高比
     * @param desireSize   设定的Size
     * @param isLargerSize 缩放大于/小于期望的Size
     * @return
     */
    private Bitmap setResolution(Bitmap src, float ratio, Size desireSize, boolean isLargerSize) {
        //获取原始Size以及Ratio
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        float srcRatio = srcWidth / srcHeight;
        //是否保持原有的宽高比
        if (keepSrcRatio) {
            ratio = srcRatio;
        }
        Log.e("BitmapEditor", "getSrcBmpFromDataSource srcWidth =" + srcWidth);
        Log.e("BitmapEditor", "getSrcBmpFromDataSource srcHeight =" + srcHeight);
        //不需要做分辨率变换
        if (desireSize == null && (ratio == 0 || ratio == srcRatio)) { //未设置期望Size且宽高比未设置或者与Src宽高比相等
            return src;
        }
        if (desireSize.width == srcWidth && desireSize.height == srcHeight && ratio == srcRatio) {
            return src;
        }
        //计算输出Bitmap的分辨率
        int destHeight;
        int destWidth;
        float destRatio;
        if (desireSize != null) {
            destWidth = desireSize.width;
            destHeight = desireSize.height;
            destRatio = destWidth / destHeight;
        } else {
            destWidth = srcWidth;
            destHeight = srcHeight;
            destRatio = srcRatio;
        }
        //调整分辨率
        if (ratio > 0 && ratio != destRatio) {
            if (ratio > destRatio) { //期望的宽大于目标宽
                if (isLargerSize) {
                    //放大width
                    destWidth = (int) (destHeight * ratio);
                } else {
                    //缩小Height
                    destHeight = (int) (destWidth / ratio);
                }
            } else { //期望的宽小于目标的宽
                if (isLargerSize) {
                    destHeight = (int) (destWidth / ratio);
                } else {
                    destWidth = (int) (destHeight * ratio);
                }
            }
        }
        Log.e("BitmapEditor", "getSrcBmpFromDataSource destWidth =" + destWidth);
        Log.e("BitmapEditor", "getSrcBmpFromDataSource destHeight =" + destHeight);
        //根据调整后的分辨率进行缩放
        return src.createScaledBitmap(src, destWidth, destHeight, false);
    }

    /**
     * 判定Size条件并返回Bmp
     */
    private Bitmap getBmpResultForBitmap(Bitmap src, int size) {
        Log.e("BitmapEditor", "getBmpResultForBitmap size =" + size);
        if (size < 0) {
            return src;
        } else {
            return getBmpFromBOS(limitSize(src, size));
        }
    }

    /**
     * 判定Size条件并返回File
     */
    private File getFileResultForBitmap(Bitmap src, int size, String filePath) throws IOException {
        Log.e("BitmapEditor", "getFileResultForBitmap size =" + size);
        return getFileFromBOS(limitSize(src, size), filePath);
    }

    /**
     * 判定Size条件并返回Byte数组
     */
    private byte[] getByteArrayResultForBitmap(Bitmap src, int size) {
        Log.e("BitmapEditor", "getByteArrayResultForBitmap size =" + size);
        return getByteArrayFromBOS(limitSize(src, size));
    }

    /**
     * 通过削减图片质量来缩减bmp的内存(获取压缩过的ByteBuffer)
     *
     * @param src
     * @param size
     */
    private ByteArrayOutputStream limitSize(Bitmap src, int size) {
        //控制图片大小
        int DEFAULT_POST_PHOTO_QUALITY = 100;
        ByteArrayOutputStream dataByte = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, DEFAULT_POST_PHOTO_QUALITY, dataByte); //(质量压缩）（有效）
        if (size > 0) {
            Log.e("saveBmpAsFile", "dataByte length start=" + dataByte.toByteArray().length / 1024);
            while (dataByte.toByteArray().length > size) {//循环判断如果压缩后图片是否大于1000kb,大于继续压缩
                Log.e("saveBmpAsFile", "dataByte length recycle=" + dataByte.toByteArray().length / 1024);
                if (DEFAULT_POST_PHOTO_QUALITY <= qualityInterval) {
                    qualityInterval = (int) (Math.ceil(qualityInterval / 2));
                }
                dataByte.reset();//重置dataByte即清空dataByte
                DEFAULT_POST_PHOTO_QUALITY = DEFAULT_POST_PHOTO_QUALITY - qualityInterval;//每次-qualityInterval质量
                src.compress(Bitmap.CompressFormat.JPEG, DEFAULT_POST_PHOTO_QUALITY, dataByte);//这里压缩options%，把压缩后的数据存放到baos中
            }
        }
        Log.e("saveBmpAsFile", "dataByte DEFAULT_POST_PHOTO_QUALITY=" + DEFAULT_POST_PHOTO_QUALITY);
        Log.e("saveBmpAsFile", "dataByte length end=" + dataByte.toByteArray().length / 1024);
        //生成ResultBmp
        return dataByte;
    }

    private Bitmap getBmpFromBOS(ByteArrayOutputStream bos) {
        return BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size());
    }

    private byte[] getByteArrayFromBOS(ByteArrayOutputStream bos) {
        return bos.toByteArray();
    }

    private File getFileFromBOS(ByteArrayOutputStream bos, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        //写入文件
        bos.writeTo(fos);
        fos.close();
        return file;
    }
}
