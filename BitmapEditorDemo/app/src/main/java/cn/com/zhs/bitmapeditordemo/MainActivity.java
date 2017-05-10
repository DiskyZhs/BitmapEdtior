package cn.com.zhs.bitmapeditordemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    private EditText widthEdit;
    private EditText heightEdit;
    private EditText ratioEdit;
    private EditText sizeEdit;
    private RadioGroup inputGroup;
    private RadioButton inputResRadio;
    private RadioButton inputFileRadio;
    private RadioGroup outputGroup;
    private RadioButton outputBmpRadio;
    private RadioButton outputFileRadio;
    private Button processBtn;
    private TextView infoText;

    //图片来源
    private String inputPicPath = Environment.getExternalStorageDirectory() + File.separator + "test.jpg";
    private int inputPicRes = R.mipmap.test;
    private String outputPicPath = Environment.getExternalStorageDirectory() + File.separator + "editResult.jpg";

    //参数
    int width = 0;
    int height = 0;
    float ratio = 0;
    int size = 0;
    InputMode input = InputMode.RES;
    OutputMode output = OutputMode.FILE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initView
        widthEdit = (EditText) findViewById(R.id.width_set_edit);
        heightEdit = (EditText) findViewById(R.id.height_set_edit);
        ratioEdit = (EditText) findViewById(R.id.ratio_set_edit);
        sizeEdit = (EditText) findViewById(R.id.size_set_edit);
        inputGroup = (RadioGroup) findViewById(R.id.input_radio_group);
        outputGroup = (RadioGroup) findViewById(R.id.output_radio_group);
        processBtn = (Button) findViewById(R.id.process_btn);
        infoText = (TextView) findViewById(R.id.info_text);
        inputResRadio = (RadioButton) findViewById(R.id.input_from_res_radio);
        inputFileRadio = (RadioButton) findViewById(R.id.input_from_file_radio);
        outputBmpRadio = (RadioButton) findViewById(R.id.output_to_bitmap_radio);
        outputFileRadio = (RadioButton) findViewById(R.id.output_to_file_radio);

        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                process();
                //Rxjava 异步处理
//                Observable.create(new Observable.OnSubscribe<String>() {
//
//                    @Override
//                    public void call(Subscriber<? super String> subscriber) {
//                        process();
//                        subscriber.onNext("process finish");
//                        subscriber.onCompleted();
//                    }
//                }).subscribeOn(Schedulers.newThread())
//                        .subscribe(new Action1<String>() {
//                            @Override
//                            public void call(String s) {
//                                Log.e("test", "process success s = " + s);
//                            }
//                        }, new Action1<Throwable>() {
//                            @Override
//                            public void call(Throwable throwable) {
//                                Log.e("test", "process fail throwable = " + throwable.toString());
//                            }
//                        });


            }
        });
        inputGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (i == R.id.input_from_res_radio) {
                    input = InputMode.RES;
                } else {
                    input = InputMode.FILE;
                }
            }
        });
        outputGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (i == R.id.output_to_bitmap_radio) {
                    output = OutputMode.BMP;
                } else {
                    output = OutputMode.FILE;
                }
            }
        });
    }

    private void process() {
        if (String.valueOf(widthEdit.getText()).length() > 0) {
            width = Integer.parseInt(String.valueOf(widthEdit.getText()));
        }
        if (String.valueOf(heightEdit.getText()).length() > 0) {
            height = Integer.parseInt(String.valueOf(heightEdit.getText()));
        }
        if (String.valueOf(ratioEdit.getText()).length() > 0) {
            ratio = Float.parseFloat(String.valueOf(ratioEdit.getText()));
        }
        if (String.valueOf(sizeEdit.getText()).length() > 0) {
            size = Integer.parseInt(String.valueOf(sizeEdit.getText())) * 1024;
        }

        Log.e("test", "process width =" + width);
        Log.e("test", "process height =" + height);
        Log.e("test", "process ratio =" + ratio);
        Log.e("test", "process size =" + size);
        //初始化BitmapEditor
        BitmapEditor bitmapEditor = BitmapEditor.init();
        //input
        if (input == InputMode.RES) {
            bitmapEditor.from(inputPicRes, this);
            Log.e("test", "process from res");
        } else {
            bitmapEditor.from(inputPicPath);
            Log.e("test", "process from file");
        }
        //listener
        bitmapEditor.listener(new BitmapEditor.BitmapEditorListener() {
            @Override
            public void onEditorStart() {

            }

            @Override
            public void onEditorEnd(Bitmap data, long timeCost) {
                Log.e("test", "process onEditorEnd bmp timeCost = " + timeCost);
                Toast.makeText(MainActivity.this, "图像处理完成 as bitmap", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditorEnd(File file, long timeCost) {
                Log.e("test", "process onEditorEnd file timeCost = " + timeCost);
                Toast.makeText(MainActivity.this, "图像处理完成 as file", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditorEnd(byte[] data, long timeCost) {
                Log.e("test", "process onEditorEnd data timeCost = " + timeCost);
                Toast.makeText(MainActivity.this, "图像处理完成 as data", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void OnError(Exception e) {
                Log.e("test", "process OnError Exception = " + e.toString());
                Toast.makeText(MainActivity.this, "图像处理出错 Exception = " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        //分辨率
        if (width > 0 || height > 0) {
            Log.e("test", "process paserResolution");
            //ratio
            if (ratio > 0) {
                bitmapEditor.paserResolution(width, height);
                bitmapEditor.paserResolutionKeepRatio(width, height, ratio, false);
                Log.e("test", "process setRatio");
            }
        }
        //size
        if (size > 0) {
            bitmapEditor.limitSize(size);
            Log.e("test", "process limitSize");
        }
        //output
        if (output == OutputMode.BMP) {
            bitmapEditor.asBmp();
            Log.e("test", "process asBmp");
        } else {
            bitmapEditor.asFile(outputPicPath);
            Log.e("test", "process asFile");
        }
    }

    private enum InputMode {
        RES, FILE
    }

    private enum OutputMode {
        BMP, FILE
    }
}
