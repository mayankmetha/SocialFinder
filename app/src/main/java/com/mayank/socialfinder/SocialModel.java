package com.mayank.socialfinder;

public class SocialModel {

    String url;
    int status;
    String platform;
    String details;

    public SocialModel(String url, int status, String platform, String details) {
        this.url = url;
        this.status = status;
        this.platform = platform;
        this.details = details;

    }

    public String getUrl() {
        return url;
    }

    public int getStatus() {
        return status;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDetails() {
        return details;
    }

    public boolean modelExist(SocialModel tmpModel) {
        return tmpModel.getUrl().equals(this.url);
    }
}