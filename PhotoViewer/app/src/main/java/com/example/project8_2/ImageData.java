package com.example.project8_2;

import android.graphics.Bitmap;

public class ImageData {
    private int id;  // 게시물 ID 필드 추가
    private String title;
    private String text;
    private Bitmap image;

    public ImageData(int id, String title, String text, Bitmap image) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public Bitmap getImage() {
        return image;
    }
}
