package com.produce.leosa.ncuapp;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.eclipsesource.v8.V8;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends BaseActivity {
    private String url_safecode = "http://jwc101.ncu.edu.cn/jsxsd/verifycode.servlet"; // 验证码
    private String url_Login = "http://jwc101.ncu.edu.cn/jsxsd/xk/LoginToXk"; // 登录
    private Map<String, String> cookie;
    private ImageView imageView;
    private Button loginBt;
    private EditText verin,userin,pwdin;
    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initComp();
        initFunc();
    }

    /**
     * 初始化各个组件
     */
    private void initComp() {
        imageView=(ImageView)findViewById(R.id.verimg);
        verin=(EditText)findViewById(R.id.verin);
        loginBt=(Button) findViewById(R.id.loginbt);
        userin=(EditText)findViewById(R.id.userin);
        pwdin=(EditText)findViewById(R.id.pwdin);
    }

    /**
     * 设置各项组件的功能
     */
    private void initFunc() {
        //region 添加消息接收，改变验证码
        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==0x123){
                    imageView.setImageBitmap((Bitmap)msg.obj);
                }
            }
        };
        //endregion
        //region 登录点击事件，使用Jsoup进行网络连接,链接时使用请求验证码产生的cookie
        loginBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    InputStream is=getAssets().open("encode.js");   //获取用户名与密码加密的js代码
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    try {
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }

                        V8 runtime = V8.createV8Runtime();      //使用J2V8运行js代码并将编码结果返回
                        final String encodename = runtime.executeStringScript(sb.toString()
                                + "encodeInp('"+userin.getText().toString()+"');\n");
                        final String encodepwd=runtime.executeStringScript(sb.toString()+"encodeInp('"+pwdin.getText().toString()+"');\n");
                        runtime.release();
                        (new Thread(){
                            @Override
                            public void run() {
                                Map<String, String> data = new HashMap<String, String>();   //进行Post的参数
                                data.put("encoded", encodename+"%%%"+encodepwd);
                                data.put("RANDOMCODE", verin.getText().toString());
                                Connection connect = Jsoup.connect(url_Login)
                                        .header("Accept",
                                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                                        .userAgent("Mozilla").method(Connection.Method.POST).data(data).timeout(3000);
                                for (Map.Entry<String, String> entry : cookie.entrySet()) { //使用获取验证码时生成的cookie
                                    connect.cookie(entry.getKey(), entry.getValue());
                                }
                                Connection.Response response = null;
                                try {
                                    response = connect.execute();
                                    Pattern p=Pattern.compile("<title>(.*)</title>");
                                    Matcher m =p .matcher(response.body()) ;
                                    if( m. find()) {
                                        System.out.println(m.group(1)); //通过response的title是否为"学生个人中心"来进行判断是否登录成功
                                        Log.e("Test",m.group(1));
                                        if((m.group(1).toString()).equals("学生个人中心")){
                                            //跳转时，将登录的cookie信息传递到下一个Activity
                                            Intent intent=new Intent(MainActivity.this,InfoActivity.class);
                                            MapIntent mapIntent=new MapIntent();
                                            mapIntent.setMap(cookie);
                                            Bundle bundle=new Bundle();
                                            bundle.putSerializable("cookiemap",mapIntent);
                                            intent.putExtras(bundle);
                                            startActivity(intent);
                                        }else{

                                        }
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        //endregion
        //region 验证码点击事件与验证码图片修改
        new PicThread().start();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PicThread().start();
            }
        });
        //endregion
    }

    class PicThread extends Thread{
        @Override
        public void run() {
            Connection.Response response = null;
            try {
                response = Jsoup.connect(url_safecode).ignoreContentType(true) // 获取图片需设置忽略内容类型
                        .userAgent("Mozilla").method(Connection.Method.GET).timeout(3000).execute();
                cookie = response.cookies();
                byte[] bytes = response.bodyAsBytes();
                Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                Message message=new Message();
                message.what=0x123;
                message.obj=bitmap;
                handler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    int getViewId() {
        return R.layout.activity_main;
    }


}
