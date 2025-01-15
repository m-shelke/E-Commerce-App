package com.example.creativecart_app.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.R;
import com.example.creativecart_app.adapters.AdapterImageSlider;
import com.example.creativecart_app.databinding.ActivityProductDetailsBinding;
import com.example.creativecart_app.models.ModelAds;
import com.example.creativecart_app.models.ModelImageSlider;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ProductDetailsActivity extends AppCompatActivity {

    //View binding
    private ActivityProductDetailsBinding binding;

    //TAG for logs in LogCat
    private static final String TAG = "ADS_DETAILS_TAG";

    //Firebase for Auth related task
    private FirebaseAuth firebaseAuth;

    //Ads Id, will get from Intent
    private String adsId ="";

    //adsLongitude and adsLatitude of the Ads to view it on map
    private double adsLatitude=0;
    private double adsLongitude=0;

    //to load seller info, chats with seller and call, SMS
    private String sellerId=null;
    private String sellerPhone=null;

    //Hold the Ads favorites state by current Users
    private boolean favorite=false;

    //list of the Ads images show in the Slider
    private ArrayList<ModelImageSlider> imageSliderArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);
        //init View Binding ActivityAdsDetailsBinding = activity_product_details.xml
        binding = ActivityProductDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Handle some UI View in start. We will show the Edit, Delete options if the user is Ads Owner. We will show call, chat , SMS options if users is not Ads owner
        binding.toolbarEditBtn.setVisibility(View.GONE);
        binding.toolbarDeleteBtn.setVisibility(View.GONE);
      //  binding.chatBtn.setVisibility(View.GONE);
        binding.callBtn.setVisibility(View.GONE);
        binding.smsBtn.setVisibility(View.GONE);

        //get the Id of the Ads (as we passed in AdapterAds class while starting this activity)
        adsId = getIntent().getStringExtra("adsId");

        Log.d(TAG, "onCreate: adsId"+adsId);

        //Firebase for Auth related task
        firebaseAuth = FirebaseAuth.getInstance();

        //if the user-logged-in then check is the Ads in favorites of the user
        if (firebaseAuth.getCurrentUser() != null){
            checkIsFavorite();
        }

        loadAdsDetails();
        loadImages();

        //Hanlde the toolbarBackBtn click, to go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //Handle toolbarDeleteBtn click, to delete the Ads
        binding.toolbarDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Alert dialog to confirm if the user really wants to delete the Ads
                MaterialAlertDialogBuilder materialAlertDialogBuilder=new MaterialAlertDialogBuilder(ProductDetailsActivity.this);
                materialAlertDialogBuilder.setTitle("Delete Product")
                        .setMessage("Are You Sure You Want To Delete This Product..!!")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Delete clicked, delete Ads
                                deleteAds();
                            }
                        })
                        .setNegativeButton("CANCLE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancel click, dismiss the dialog
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        });

        //Handle toolbarEditBtn clicked, start AdsCreateActivity to edit this Ads
        binding.toolbarEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editOption();
            }
        });

        //Handle the toolbarFavBtn click, add/remove favorites
        binding.toolbarFavBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (favorite){
                    //This Ad is in favorites of the current user, remove from favorites
                    Utils.removeFromFavorite(ProductDetailsActivity.this,adsId);
                }else {
                    //this Ad is not in favorites of current user, add to favorites
                    Utils.addToFavorite(ProductDetailsActivity.this,adsId);
                }
            }
        });

        //Handle sellerProfileCv click, start SellerProfileActivity
        binding.sellerProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ProductDetailsActivity.this, SellerProfileActivity.class);
                intent.putExtra("sellerId",sellerId);
                startActivity(intent);
            }
        });

       // Handle chatBtn click, to start chatting with seller
//        binding.chatBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });

        //Handle callBtn click, buyer can call directly to seller
        binding.callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.callIntent(ProductDetailsActivity.this,sellerPhone);
            }
        });

        //Handle smsBtn click, though which buyer can do message to seller
        binding.smsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Utils.smsIntent(ProductDetailsActivity.this,sellerPhone);
            }
        });

        // //Handle mapBtn click, open map and measure the distance from current address
        binding.mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.mapIntent(ProductDetailsActivity.this,adsLatitude,adsLongitude);
            }
        });
    }
    private void editOption() {
        Log.d(TAG, "editOption: ");

        //init/setup popup menu
        PopupMenu popupMenu=new PopupMenu(this,binding.toolbarEditBtn);

        //Add menu item to PopupMenu with param Group ID,Item ID,Order,Title
        popupMenu.getMenu().add(Menu.NONE,0,0,"Edit");
        popupMenu.getMenu().add(Menu.NONE,1,1,"Mark As Sold");

        //show Popup menu
        popupMenu.show();

        //Handle setOnMenuItemClickListener item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId=item.getItemId();

                if (itemId==0){

                    //Edit clicked, start AdsCreateActivity with Ads Id and isEditMode as true
                    Intent intent=new Intent(ProductDetailsActivity.this, LaunchProductActivity.class);
                    intent.putExtra("isEditMode",true);
                    intent.putExtra("adsId",adsId);
                    startActivity(intent);
                }else if (itemId==1){
                    //Mark as sold
                    showMarkAsSoldDialog();
                }
                return true;
            }
        });
    }



    private void showMarkAsSoldDialog(){

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this);
        materialAlertDialogBuilder.setTitle("Marl As Sold")
                .setMessage("Are You Sure, You Want To Mark This Ads As Sold..?")
                .setPositiveButton("Sold", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "onClick: Sold Click");

                        //setup info to update in the existing Ads i.e. mark as sold by setting the value of the status to SOLD
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("status", ""+ Utils.ADS_STATUS_SOLD);

                        //Ads DB path to update its available/sold status. Ads-->AdsId
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Ads");
                        reference.child(adsId)
                                .updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        //Success
                                        Log.d(TAG, "onSuccess: Mark As Sold");
                                        Utils.toast(getApplicationContext(),"Mark As Sold");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //Failed
                                        Log.e(TAG, "onFailure: ",e);
                                        Utils.toast(ProductDetailsActivity.this,"Failed To Mark As Sold Due To "+e.getMessage());
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "onClick: Cancle Click");
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    private void loadAdsDetails(){
        Log.d(TAG, "loadAdsDetails: ");

        //Ads DB path to get the Ads details. Ads-->AdsId
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adsId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        try {
                            //setup model from Firebase DataSnapshot
                            ModelAds modelAds = snapshot.getValue(ModelAds.class);

                            //get the data from model
                            sellerId= modelAds.getUid();
                            String title = modelAds.getTitle();
                            String description = modelAds.getDescription();
                            String address = modelAds.getAddress();
                            String condition = modelAds.getCondition();
                            String category=modelAds.getCategory();
                            String price = modelAds.getPrice();
                            adsLatitude=modelAds.getLatitude();
                            adsLongitude=modelAds.getLongitude();

                            long timestamp=modelAds.getTimestamp();

                            //format date time e.g timestamp to dd//mm/yyyy
                            String formattedDate=Utils.formatTimestampDate(timestamp);

                            if (sellerId.equals(firebaseAuth.getUid())){
                                //Ads is created by currently sign-in user , so should able to edit and delete the Ads
                                binding.toolbarEditBtn.setVisibility(View.VISIBLE);
                                binding.toolbarDeleteBtn.setVisibility(View.VISIBLE);

                                //Should not able to chat,call and message to himself
                              //  binding.chatBtn.setVisibility(View.GONE);
                                binding.callBtn.setVisibility(View.GONE);
                                binding.smsBtn.setVisibility(View.GONE);
                                binding.sellerProfileLabelTv.setVisibility(View.GONE);
                                binding.sellerProfileCv.setVisibility(View.GONE);
                            }else {
                                //Ads is created by currently sign-in user , so should not able to edit and delete the Ads
                                binding.toolbarEditBtn.setVisibility(View.GONE);
                                binding.toolbarDeleteBtn.setVisibility(View.GONE);

                                //should able to chat,call and message (to ads creator) and view seller Profile
                              //  binding.chatBtn.setVisibility(View.VISIBLE);
                                binding.callBtn.setVisibility(View.VISIBLE);
                                binding.smsBtn.setVisibility(View.VISIBLE);
                                binding.sellerProfileLabelTv.setVisibility(View.VISIBLE);
                                binding.sellerProfileCv.setVisibility(View.VISIBLE);
                            }
                            //set data to UI View
                            binding.titleTv.setText(title);
                            binding.descriptionTv.setText(description);
                            binding.addessTv.setText(address);
                            binding.categoryTv.setText(category);
                            binding.conditionTv.setText(condition);
                            binding.priceTv.setText(price);
                            binding.dateTv.setText(formattedDate);

                            //function call, load seller info e.g. profile image,name,member since
                            loadSellerDetails();

                        }catch (Exception e){
                            Log.e(TAG, "onDataChange: ",e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void loadSellerDetails() {
        Log.d(TAG, "loadSellerDetails: ");

        //DB path to load seller info. Users-->sellerId
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(sellerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //get data
                        String phoneCode=""+snapshot.child("phoneCode").getValue();
                        String phoneNumber=""+snapshot.child("phoneNumber").getValue();
                        String name=""+snapshot.child("name").getValue();
                        String profileImageUrl=""+snapshot.child("profileImageUrl").getValue();
//                        long timestamp=(Long) snapshot.child("timestamp").getValue();

                        //format date time. e.g. timestamp to dd/mm/yyyy
 //                        String formattedDate=Utils.formatTimestampDate(timestamp);

                        //phone number of the seller
                        sellerPhone=phoneCode +""+phoneNumber;

                        //set data to UI
                        binding.sellerNameTv.setText(name);
 //                       binding.memberSinceTv.setText(formattedDate);

                        try {
                            Log.d(TAG, "onDataChange: ");

                            RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);


                            Glide.with(ProductDetailsActivity.this)
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



//                            Glide
//                                    .with(ProductDetailsActivity.this).load(profileImageUrl)
//                                    .placeholder(R.drawable.baseline_downloading_24)
//                                    .into(binding.sellerProfileIv);

                        }catch (Exception e){
                            Log.e(TAG, "onDataChange: ",e);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: ");

                    }
                });
    }


    private void checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: ");

        //DB path to check if Ads is in Favorites of the current user. Users-->uid-->Favorites-->AdsId
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Users");
        reference1.child(firebaseAuth.getUid()).child("Favorites").child(adsId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //if snapshot exists (value is true) means the Ads is in favorite of the current user otherwise no
                        favorite = snapshot.exists();

                        Log.d(TAG, "onDataChange: favorites " + favorite);

                        //check if favorites or not to set image of favBtn accordingly
                        if (favorite) {
                            //Favorites, set image ic_fav_yes to button favBtn
                            binding.toolbarFavBtn.setImageResource(R.drawable.ic_fav_yes);
                        } else {
                            //Not Favorites, set ic_fav_no to button favBtm
                            binding.toolbarFavBtn.setImageResource(R.drawable.ic_fav_no);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }




    private void loadImages(){
        Log.d(TAG, "loadImages:  ");

        //init list before starting adding data into it
        imageSliderArrayList=new ArrayList<>();

        //DB path to load the Ads images. Ads-->AdsId-->Images
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adsId).child("Images")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //clear list before starting adding data into it
                        imageSliderArrayList.clear();

                        //there might be multiple images, load it to load all
                        for (DataSnapshot snapshot1: snapshot.getChildren()){

                            //prepare model (spelling in model class should be same as in firebase)
                            ModelImageSlider modelImageSlider=snapshot1.getValue(ModelImageSlider.class);

                            //load the prepare model to list
                            imageSliderArrayList.add(modelImageSlider);
                        }

                        //setup adapter and set to viewpager i.e imageSlider
                        AdapterImageSlider adapterImageSlider=new AdapterImageSlider(ProductDetailsActivity.this,imageSliderArrayList);
                        binding.imageSliderVp.setAdapter(adapterImageSlider);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {


                    }
                });
    }

    private void deleteAds(){
        Log.d(TAG, "deleteAds: ");

        //DB path to delete the Ads. Ads-->AdsId
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adsId)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Success
                        Log.d(TAG, "onSuccess: ");
                        Utils.toast(ProductDetailsActivity.this,"Deleted");

                        //finished activity and go back
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failure
                        Log.d(TAG, "onFailure: ");
                        Utils.toast(ProductDetailsActivity.this,"Failed To Delete Due To "+e.getMessage());
                    }
                });
    }
}