package com.example.findflower;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.aip.imageclassify.AipImageClassify;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class ResultActivity extends AppCompatActivity {
    private static final String APP_ID = "20246010";
    private static final String API_KEY = "4cSE9s3rKSn3ejB5u5g0juLn";
    private static final String SECRET_KEY = "9bIAW9XCInCE5A5LbtKiodBMY15YFOyd";//百度识别api访问密钥
    private Context context = ResultActivity.this;
    private String pic_path; //识别目标图片地址
    private Runnable runnable = new Runnable() {
        @Override
        public void run() { //联网获取结果子线程。访问网络需要单独在线程内进行
            // TODO: http request.
            getFlowerInfo();
        }
    };
    private JSONArray result; //识别结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏显示
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_result);

        //从上一个Activity获取图片位置数据
        Intent i =getIntent();
        pic_path = i.getStringExtra("path");

        //返回按钮监听
        Button rtn_button = findViewById(R.id.rtn_button);
        rtn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ResultActivity.this.finish();
            }
        });

        //开启联网获取结果子线程
        Thread getInfo = new Thread(runnable);
        getInfo.start();

        //保证获取结果线程执行完后返回主线程
        try {
            getInfo.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //返回主线程显示结果
        setFlowerInfo();
    }

    /**
     * 重写退出函数，在离开界面时删除临时图片
     * **/
    @Override
    public void finish() {
        File temp_img = new File(pic_path);
        temp_img.delete();

        //调用超类退出函数。若无则无法正确退出
        super.finish();
    }

    /**
     * 调用api获取识别结果
     * **/
    public void getFlowerInfo(){
        AipImageClassify client = new AipImageClassify(APP_ID, API_KEY, SECRET_KEY);

        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<>();
        options.put("baike_num", "5");

        // 参数为本地路径
        JSONObject res = client.plantDetect(pic_path, options);

        //从api传回的结果中提取结果信息
        try {
            result = res.getJSONArray("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示识别结果
     * **/
    public void setFlowerInfo(){
        TextView name_ch = findViewById(R.id.name_ch);
        TextView score_txt = findViewById(R.id.score_txt);
        TextView description_txt = findViewById(R.id.description_txt);

        ImageView info_pic = findViewById(R.id.info_pic);
        String imgUrl;

        try {
            //显示裁剪后的目标图片
            ImageView flw_pic = findViewById(R.id.flw_pic);
            Glide.with(context).load(pic_path)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .into(flw_pic);

            //依次设置结果名 准确度 简介内容
            name_ch.setText(result.getJSONObject(0).getString("name"));
            String s = result.getJSONObject(0).getString("score");
            score_txt.setText(String.valueOf(Math.round(Double.parseDouble(s) * 1000) / 10.0));

            JSONObject baike_info = result.getJSONObject(0).getJSONObject("baike_info");
            s = "\t\t\t\t" + baike_info.getString("description");
            description_txt.setText(s);

            //显示结果示例图片
            imgUrl = baike_info.getString("image_url");
            Glide.with(context).load(imgUrl)
                    .into(info_pic);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
