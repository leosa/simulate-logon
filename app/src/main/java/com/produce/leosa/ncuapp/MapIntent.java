package com.produce.leosa.ncuapp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Leosa on 2018/2/20.
 * 自定义的可序列化的类，用于进行Activity间传递Map
 */

public class MapIntent implements Serializable {
    public Map<String,String> map;

    public MapIntent() {
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }
}
