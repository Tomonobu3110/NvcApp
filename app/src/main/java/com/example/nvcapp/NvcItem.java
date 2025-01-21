package com.example.nvcapp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class NvcItem {
    private String event;
    private String observation;
    private String feelings;
    private String needs;
    private String request;

    // コンストラクタとゲッター/セッター
    public NvcItem(String event, String observation, String feelings, String needs, String request) {
        this.event = event;
        this.observation = observation;
        this.feelings = feelings;
        this.needs = needs;
        this.request = request;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static NvcItem fromJson(String json) {
        Gson gson = new Gson();
        Type nvcType = new TypeToken<NvcItem>() {}.getType();
        return gson.fromJson(json, nvcType);
    }
    // ゲッターとセッター
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }

    public String getFeelings() { return feelings; }
    public void setFeelings(String feelings) { this.feelings = feelings; }

    public String getNeeds() { return needs; }
    public void setNeeds(String needs) { this.needs = needs; }

    public String getRequest() { return request; }
    public void setRequest(String request) { this.request = request; }
}
