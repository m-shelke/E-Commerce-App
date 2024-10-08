package com.example.creativecart_app.LoginOptionActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.example.creativecart_app.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

//A class that will contain static function,constant variable that we will be used in whole application
public class Utils {

    public static final String ADS_STATUS_AVAILABLE="AVAILABLE";
    public static final String ADS_STATUS_SOLD="SOLD";

    //Categories arrays of the Ads
    public static final String[] categories={

            "Mobiles",
            "Laptop",
            "Electronics",
            "Vehicle",
            "Home Decor",
            "Beauty",
            "Books",
            "Sports",
            "Animals",
            "Hobbies",
            "Agriculture"
    };

    //Categories icon array of Ads
    public static final int[] categoryIcon={
            R.drawable.cate_phone,
            R.drawable.cate_laptop,
            R.drawable.cate_electric,
            R.drawable.cate_vehicle,
            R.drawable.cate_furniture,
            R.drawable.girl2,
            R.drawable.cate_book,
            R.drawable.cate_sports,
            R.drawable.cate_pets,
            R.drawable.cate_hobby,
           R.drawable.cate_agri,
    };

    //Condition arrays of the Ads
    public static final String[] condition={"New","Used","Refurbished"};

    /* A Function to show Toast
    * @param context the context of the activity/fragment from where this function will be called
    * @param message the message to show in the Toast.*/
   public   static void toast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_LONG).show();
    }


    /* A Function to get current timestap
    * @return Return the current timestamp as long datatype  */
    public  static long getTimestap(){
        return System.currentTimeMillis();
    }


    /* A Function to show toast @param timestamp the timestamp of type Long that we need to format to
    dd/mm/yyyy @return timestamp formatted to  date dd/mm/yyyy
     */
    public static String formatTimestampDate(Long timestamp){
        Calendar calendar=Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);

        String date= DateFormat.format("dd/mm/yyyy",calendar).toString();
        return date;
    }

    /* Add Ads to Favorite
    @param1: Context the context of the Activity/Fragment from where this function will be called
    @param2: The I'd of the Ads to be added to Favorite of the current user
      */
    public static void addToFavorite(Context context,String adsId){
        //We can add, only if user is logged in
        //1) check if user is logged in
        FirebaseAuth firebaseAuth=FirebaseAuth.getInstance();
        if (firebaseAuth == null){
            //not logged in, we can't add to favorite
            Utils.toast(context,"You're Not Logged In..!");
        }else {
            //logged in, can add to favorite
            // get timestamp
            long timestamp = Utils.getTimestap();

            //setup data to Ads to Firebase DB
            HashMap<String,Object> hashMap = new HashMap<>();
            hashMap.put("adsId",adsId);
            hashMap.put("timestamp",timestamp);

            //Add data to DB. Users --> Uid--> Favorite-->AdsId-->Favorite
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(adsId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //Success
                            Utils.toast(context,"Added to Favorites ...!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Failed
                            Utils.toast(context,"Failed To Added To Favorite Due To "+e.getMessage());
                        }
                    });
        }
    }


    /* Remove Ads to Favorite
    @param1: Context the context of the Activity/Fragment from where this function will be called
    @param2: The I'd of the Ads to be remove from Favorite of the current user
      */
    public static void removeFromFavorite(Context context,String adsId){
        //We can remove, only if user is logged in
        //1) check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        //not logged in, we can't remove from favorite
        if (firebaseAuth.getUid() == null){

            Utils.toast(context,"You're Not Logged In...!");
        }else {
            //logged in, can remove from favorite
            // Remove data from DB.Users-->Uid-->Favorite-->adsId
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(adsId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //Success
                            Utils.toast(context,"Remove From Favorite..");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Failed
                            Utils.toast(context,"Failed To Remove From Favorite Due To "+e.getMessage());
                        }
                    });
        }
    }

    /*Launch Call Intent with phone number
     @param1: Context the context of activity/fragment from where this will be called
     @param2: the phone number that will be open in call intent
     */
    public static void callIntent(Context context,String phone){

        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"+Uri.encode(phone)));
        context.startActivity(intent);
    }


    /*Launch SMS Intent with phone number
     @param1: Context the context of activity/fragment from where this will be called
     @param2: the phone number that will be open in SMS intent
     */
    public static void smsIntent(Context context,String phone){

        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"+Uri.encode(phone)));
        context.startActivity(intent);
    }

    /*Launch Google Map with input location
     @param1: Context the context of activity/fragment from where this will be called
     @param2: latitude the latitude of the location to be shown in google map
     @param2: longitude the longitude of the location to be shown in google map
     */

    public static void mapIntent(Context context,double latitude, double longitude){
        //Create a Uri from on intent string. Use the result to create on Intent.

        Uri gmmIntentUri=Uri.parse("http://maps.google.com/maps?daddr=" + latitude +","+longitude);

        //Create an Intent from ggmIntentUri. set the action to ACTION_VIEW
        Intent mapIntent=new Intent(Intent.ACTION_VIEW,gmmIntentUri);

        //Make the Intent Explicit by setting the Google Maps Package
        mapIntent.setPackage("com.google.android.apps.maps");

        //Attempt to start an activity that can handle the Intent e.g. Google Map
        if (mapIntent.resolveActivity(context.getPackageManager()) !=null){
            //Google Map installed, start
            context.startActivity(mapIntent);
        }else {
            //Google Map not installed, can't start
            Utils.toast(context,"Google Map Not Installed..!");
        }

    }
}
