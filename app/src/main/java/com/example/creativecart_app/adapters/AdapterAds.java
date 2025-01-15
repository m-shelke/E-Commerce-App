package com.example.creativecart_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.creativecart_app.LoginOptionActivity.FilterAds;
import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.R;
import com.example.creativecart_app.activity.ProductDetailsActivity;
import com.example.creativecart_app.databinding.RowAdBinding;
import com.example.creativecart_app.models.ModelAds;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;

public class AdapterAds extends  RecyclerView.Adapter<AdapterAds.HolderAds> implements Filterable {

    //View Binding
    private RowAdBinding binding;
    private static final String TAG="ADAPTER_ADS_TAG";

    //Firebase Auth for auth related task
    private FirebaseAuth firebaseAuth;

    //Context of activity/fragment from where instance of AdapterAds class is created
    private Context context;

    //ArrayList: The list of the Ads
    public ArrayList<ModelAds> adsArrayList;
    private ArrayList<ModelAds> filterList;
    private FilterAds filter;

  /*  Constructor:
     @param: Context the context of the activity/fragment from where instances of AdapterCategory class is created
      @param:adsArrayList: The list of the Ads*/

    public AdapterAds(Context context, ArrayList<ModelAds> adsArrayList) {
            this.context = context;
        this.adsArrayList = adsArrayList;
        this.filterList=adsArrayList;

        //get instance of the firebase for auth related task
        firebaseAuth=FirebaseAuth.getInstance();
    }



    @NonNull
    @Override
    public HolderAds onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //inflate/bind the row_ads.xml
        binding=RowAdBinding.inflate(LayoutInflater.from(context),parent,false);
        return new HolderAds(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderAds holder, int position) {
        //get data from the particular position of list and set to the UI View row_ads.xml and Handle Click
        ModelAds modelAds=adsArrayList.get(position);
        Log.d(TAG, "onBindViewHolder: "+adsArrayList.get(position));

        String description=modelAds.getDescription();
        String title=modelAds.getTitle();

      //  String address =modelAds.getAddress();
        String condition=modelAds.getCondition();
        String price=modelAds.getPrice();
        long timestamp=modelAds.getTimestamp();
        String formatedDate= Utils.formatTimestampDate(timestamp);

        //Fuction call: load first image from available image of Ads e.g. if there are 5 image od Ads, load first image
        loadAdsFirstImage(modelAds,holder);

        //If the User is logged in then check that, If the Ads is in Favorite of current User
        if (firebaseAuth.getCurrentUser() != null){
            checkIsFavorite(modelAds, holder);
        }

        //set data to UI View of row_ads.xml
        holder.titleTv.setText(title);
       // holder.descriptionTv.setText(description);
      //  holder.addressTv.setText(address);
        holder.conditonTv.setText(condition);
        holder.priceTv.setText(price);
        holder.dateTv.setText(formatedDate);

        //Handle itemView.setOnClickListener click, Open the ProductDetailsActivity, also pass the id of the Ads to intent to load the details
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(context, ProductDetailsActivity.class);
                intent.putExtra("adsId",modelAds.getId());
                context.startActivity(intent);
            }
        });


        //Handled favBtn click, add/remove the Ads to/from favorite of current user
       holder.favBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {

                boolean favorite = modelAds.isFavorite();
                //Check if Ads is in favorites of current user or not - true/false
                if (favorite){
                   //this Ads is in favorite of current user, remove from favorite
                   Utils.removeFromFavorite(context,modelAds.getId());
                }else {
                    //this Ads is not in favorite of current user, add to favorite
                    Utils.addToFavorite(context,modelAds.getId());
                }
            }
        });

    }

    public void checkIsFavorite(ModelAds modelAds, HolderAds holderAds) {

        //DB path to check, if Ads is in favorites of current user. Users-->Uid-->Favorites-->AdsId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(modelAds.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //if snapshot exists (value is true) means the Ads is in favorite of current users, otherwise no
                        boolean favorite = snapshot.exists();

                        //set the value (true/false) to model
                        modelAds.setFavorite(favorite);

                        //check if favorite or not to set images of favBtn accordingly
                        if (favorite){
                            //Favorite, set image ic_fav_yes to the favBtn
                            holderAds.favBtn.setImageResource(R.drawable.ic_fav_yes);
                        }else {
                            //not favorite, set ic_fav_no to favBtn

                            holderAds.favBtn.setImageResource(R.drawable.ic_fav_no);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadAdsFirstImage(ModelAds modelAds, HolderAds holder) {
        Log.d(TAG, "loadAdsFirstImage: stage 1");
        //load first image from available image of Ads, e.g. if there are 5 image on Ads, load first image

        //Ads id to get image of it
        String adsID=modelAds.getId().trim();
       // String imgURL = modelAds.getpUrl();

        Log.d(TAG, "loadAdsFirstImage: stage 2 :: got model id:"+adsID);

        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Ads");
        Log.d(TAG, "loadAdsFirstImage: stage 3 :: Got DB Reference : "+reference);

        DatabaseReference refChild = reference.child(adsID);
        Log.d(TAG, "loadAdsFirstImage: stage 3 :: Got DB Child Reference : " + refChild + " :: with id :"+modelAds.getId() );


        refChild.child("Images").limitToFirst(1).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //This will return only 1 image as we have used query, .limitToFirst(1)
                        Log.d(TAG, "loadAdsFirstImage: stage 4 :: got snapshot : "+ snapshot );
                        for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                            //get Url of images
                            String imageUrl=""+ dataSnapshot.child("imageUrl").getValue();
                            Log.d(TAG, "loadAdsFirstImage: stage 5 ");
                            Log.d(TAG, "onDataChange: imageUrl: "+imageUrl);

                            //get image to Image View i.e imageIv
                            try {
                                Log.d(TAG, "onDataChange image loaded with url: "+ imageUrl);
                                Log.d(TAG, "onDataChange image loaded with url: "+ imageUrl);

                                RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

                                Glide.with(context)
                                        .load(imageUrl)
                                        .override(Target.SIZE_ORIGINAL) // This will load the image with its original size
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
                                        .into(holder.imageIv);



                            }catch (Exception e){
                                Log.e(TAG, "onDataChange: at  Profile activity:::: ",e);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
        });

    }

    @Override
    public int getItemCount() {
        //return size of the list
        return adsArrayList.size();
    }

   @Override
    public Filter getFilter() {
        //init the filter obj only if it is null
        if (filter==null){

            filter=new FilterAds(this,filterList);

        }
        return filter;
    }


    class HolderAds extends RecyclerView.ViewHolder{

        //UI View of the row_ads.xml
        ShapeableImageView imageIv;
        TextView titleTv,descriptionTv,addressTv,conditonTv,priceTv,dateTv;
        ImageButton favBtn;

        public HolderAds(@NonNull View itemView) {
            super(itemView);

            //init UI View of row_ads.xml
            imageIv=binding.imageIv;
            titleTv=binding.titleTv;
           // descriptionTv=binding.descriptionTv;
           // addressTv=binding.addressTv;
            conditonTv=binding.conditonTv;
            priceTv=binding.priceTv;
            dateTv=binding.dateTv;
            favBtn=binding.favBtn;

        }
    }
}
