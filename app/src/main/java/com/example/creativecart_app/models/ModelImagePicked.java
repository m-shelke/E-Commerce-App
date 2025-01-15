package com.example.creativecart_app.models;

import android.net.Uri;

public class ModelImagePicked {

    //veriable
    String id="";
    Uri imageUri=null;
    String getImageUrl=null;
    boolean fromInternet=false; //this model class will be used to show image (picked/taken from Gallery/Camera-false or from Firebase-true) in LaunchProductActivity

    //Empty Constructor required for Firebase DB
    public ModelImagePicked() {
    }

    //Constructor with all param
    public ModelImagePicked(String id, Uri imageUri, String getImageUrl, boolean fromInternet) {
        this.id = id;
        this.imageUri = imageUri;
        this.getImageUrl = getImageUrl;
        this.fromInternet = fromInternet;
    }

    //Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public String getGetImageUrl() {
        return getImageUrl;
    }

    public void setGetImageUrl(String getImageUrl) {
        this.getImageUrl = getImageUrl;
    }

    public boolean isFromInternet() {
        return fromInternet;
    }

    public void setFromInternet(boolean fromInternet) {
        this.fromInternet = fromInternet;
    }
}
