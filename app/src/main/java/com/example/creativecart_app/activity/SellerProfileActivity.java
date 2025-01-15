package com.example.creativecart_app.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.creativecart_app.adapters.AdapterAds;
import com.example.creativecart_app.databinding.ActivitySellerProfileBinding;
import com.example.creativecart_app.models.ModelAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SellerProfileActivity extends AppCompatActivity {

    //View Binding
    private ActivitySellerProfileBinding binding;

    //TAG for logs in logcat
    private String TAG="ADS_SELLER_PROFILE";

    //seller ID, will get from internet
    private String sellerId="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);
        //init View Binding activity_seller_profile.xml=ActivityAdsSellerProfileBinding
        binding=ActivitySellerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get the seller id of the Ads (as we passed in AdsDetailActivity class while starting this Activity)
        sellerId=getIntent().getStringExtra("sellerId");
        Log.d(TAG, "onCreate: sellerId: "+sellerId);

        loadSellerDetails();
        loadAds();

        //handle toolbarBackBtn click, to go back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    private void loadSellerDetails() {
        Log.d(TAG, "loadSellerDetails: ");

        //DB path to load seller info. Users-->sellerUid
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Useres");
        reference.child(sellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String name=""+ snapshot.child("name").getValue();
                        String profileImageUrl=""+ snapshot.child("profileImageUrl").getValue();
//                        long timestamp=(Long) snapshot.child("timestamp").getValue();

                        //format date time e.g timestamp to dd/mm/yyyy
 //                       String formattedDate= Utils.formatTimestampDate(timestamp);

                        //set data to UI Views
                        binding.sellerNameTv.setText(name);
 //                       binding.sellerMemberSinceTv.setText(formattedDate);

                        try {

                            RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

                            Glide.with(SellerProfileActivity.this)
                                    .load(profileImageUrl)
                                    .apply(requestOptions)
                                    .listener(new RequestListener<Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                            Log.e("GlideError", "Load failed", e);
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                            return false;
                                        }
                                    })
                                    .into(binding.sellerProfileIv);


                        }catch (Exception e){
                            Log.e(TAG, "onDataChange: ",e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadAds() {
        Log.d(TAG, "loadAds: ");

        //init adsArraylist before starting adding data into it
        ArrayList<ModelAds> adsArrayList=new ArrayList<>();

        //Firebase database listener to load Ads of the seller using orderByChild query
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Ads");
        reference.orderByChild("uid").equalTo(sellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //clear adsArraylist each time starting adding data into it
                        adsArrayList.clear();

                        //load Ads list
                        for (DataSnapshot dataSnapshot: snapshot.getChildren()){

                            try {
                                //prepare ModelAds with all data from Firebase DB
                                ModelAds modelAds=dataSnapshot.getValue(ModelAds.class);
                                //add the prepare ModelAds to list
                                adsArrayList.add(modelAds);
                            }catch (Exception e){
                                Log.e(TAG, "onDataChange: ",e);
                            }
                        }

                        //init/setup AdapterAds and set to RecyclerView i.e adsRV
                        AdapterAds adapterAds=new AdapterAds(SellerProfileActivity.this,adsArrayList);
                        binding.adsRv.setAdapter(adapterAds);

                        //set Ads count
                        String adsCount=""+adsArrayList.size();
                        binding.publishedAdsCountTv.setText(adsCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}