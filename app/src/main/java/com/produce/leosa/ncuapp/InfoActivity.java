package com.produce.leosa.ncuapp;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoActivity extends BaseActivity {
    private Map<String,String> cookie;
    private Handler mhandler;
    private List<String> sem_data;
    private Spinner spinner;
    private ListView scoreList;
    private ArrayAdapter<String> arr_adapter;
    private List<Map<String,Object>>  scoredata;
    private String sem_url="http://jwc101.ncu.edu.cn/jsxsd/kscj/cjcx_query";
    private String score_url="http://jwc101.ncu.edu.cn/jsxsd/kscj/cjcx_list";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        //region 获取cookie
        //获得意图
        Intent intent=getIntent();
        //得到数据集
        Bundle bundle=intent.getExtras();
        //获得自定义类
        MapIntent mapIntent= (MapIntent) bundle.get("cookiemap");
        cookie=mapIntent.getMap();
        //endregion
        initComp();
        initFunc();
    }

    private void initComp() {
        scoredata=new ArrayList<>();
        spinner=(Spinner)findViewById(R.id.sem_spinner);
        sem_data=new ArrayList<>();
        scoreList=(ListView)findViewById(R.id.scoreList);
    }

    private void initFunc() {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i!=0){
                    ScoreThread scoreThread=new ScoreThread();
                    scoreThread.setSemname(sem_data.get(i));
                    scoreThread.start();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mhandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==0x121){
                    Log.e("sem",sem_data.toString());
                    //为spinner设置适配器，显示可选的学期信息
                    arr_adapter= new ArrayAdapter<String>(InfoActivity.this,android.R.layout.simple_spinner_item,sem_data);
                    spinner.setAdapter(arr_adapter);

                }
                if(msg.what==0x122){
                    //为ListView添加适配器，显示成绩
                    SimpleAdapter adapter=new SimpleAdapter(InfoActivity.this,scoredata,R.layout.score_item,new String[]{"name","score"},new int[]{R.id.classname,R.id.classscore});
                    scoreList.setAdapter(adapter);
                }
            }
        };
        (new SemThread()).start();

    }

    class SemThread extends Thread{
        @Override
        public void run() {
            Connection connect = Jsoup.connect(sem_url)
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .userAgent("Mozilla").method(Connection.Method.GET).timeout(3000);
            for (Map.Entry<String, String> entry : cookie.entrySet()) { //使用获取验证码时生成的cookie
                connect.cookie(entry.getKey(), entry.getValue());
            }
            Connection.Response response = null;
            try {
                response = connect.execute();
                Pattern p1=Pattern.compile("<select id=\"kksj\" name=\"kksj\" style=\"width: 170px;\">(.*?)</select>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher m1=p1.matcher(response.body());
                if(m1.find()){
                    //Log.e("sem",response.body());
                    Pattern p=Pattern.compile("<option value=\"(.*)\">(.*)</option>");
                    Matcher m =p .matcher(m1.group(1)) ;
                    while(m.find()){
                        sem_data.add(m.group(2));
                    }
                    Message message=new Message();
                    message.what=0x121;
                    mhandler.sendMessage(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 线程，使用jsoup与正则来获取成绩信息
     */
    class ScoreThread extends Thread{
        private String semname;

        public void setSemname(String semname) {
            this.semname = semname;
        }

        @Override
        public void run() {
            Map<String, String> data = new HashMap<String, String>();
            data.put("kksj",semname);
            data.put("xsfs","all");
            data.put("kcxz","");
            data.put("kcmc","");
            Connection connect = Jsoup.connect(score_url)
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .userAgent("Mozilla").method(Connection.Method.POST).data(data).timeout(5000);
            for (Map.Entry<String, String> entry : cookie.entrySet()) { //使用获取验证码时生成的cookie
                connect.cookie(entry.getKey(), entry.getValue());
            }
            Connection.Response response = null;
            try {
                response = connect.execute();
                Pattern p=Pattern.compile("<td align=\"left\">(.*?)</td>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Pattern p2=Pattern.compile("<a href=\"javascript:JsMod(.*?)\">(.*?)</a>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher m =p .matcher(response.body()) ;
                Matcher m2=p2.matcher(response.body());
                int i=1;
                scoredata.clear();
                while(m.find()){
                    Log.e("classname",m.group(1));
                    if(i%2==0){
                        if (m2.find()){

                            Map<String,Object> datamap=new HashMap<>();
                            datamap.put("name",m.group(1));
                            datamap.put("score",m2.group(2));
                            scoredata.add(datamap);

                        }
                    }
                    i++;

                }
                Message message=new Message();
                message.what=0x122;
                mhandler.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    int getViewId() {
        return R.layout.activity_info;
    }
}
