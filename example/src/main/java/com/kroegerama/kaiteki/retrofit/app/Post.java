package com.kroegerama.kaiteki.retrofit.app;

import com.google.gson.annotations.SerializedName;

public class Post {

    @SerializedName("id")
    private Integer id = null;

    @SerializedName("userId")
    private Integer userId = null;

    @SerializedName("title")
    private String title = null;

    @SerializedName("body")
    private String body = null;

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
