package com.example.creativecart_app.activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.R;
import com.example.creativecart_app.adapters.AdapterImagePicked;
import com.example.creativecart_app.databinding.ActivityLaunchProductBinding;
import com.example.creativecart_app.models.ModelImagePicked;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LaunchProductActivity extends AppCompatActivity {

    //View Binding
    private ActivityLaunchProductBinding binding;

    //init/setup progressDialog to show, while adding/updating Ads
    private ProgressDialog progressDialog;
    private static final String TAG = "ADS_CREATED_TAG";

    //Firebase Auth for Auth related task
    private FirebaseAuth firebaseAuth;

    //Image Uri to hold the Uri of image (Picked/Captured using Gallery/Camera)
    private Uri imageUri =null;

    //List of images (Picked/Captured using Gallery/Camera or from internet)
    private ArrayList<ModelImagePicked> imagePickedArrayList;
    private AdapterImagePicked adapterImagePicked;
    private boolean isEditMode=false;
    private String adsIdForEditing="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //init View Binding, activity_launch_product.xml=ActivityAdCreateBinding
        binding = ActivityLaunchProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init/setup progressDialog to show, while adding/updating Ads
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait..");
        progressDialog.setCanceledOnTouchOutside(false);

        //Firebase Auth for Auth related task
        firebaseAuth = FirebaseAuth.getInstance();

        //Setup and set the categories adapter to the CategoryFragment Input Filed i.e categoryAct
        ArrayAdapter<String> adapterCategories = new ArrayAdapter<>(this, R.layout.row_category_act, Utils.categories);
        binding.categoryAct.setAdapter(adapterCategories);

        //Setup and set the condition adapter to the Condition Input Filed i.e conditionAct
        ArrayAdapter<String> adapterCondition = new ArrayAdapter<>(this, R.layout.row_condition_act, Utils.condition);
        binding.conditonAct.setAdapter(adapterCondition);

        Intent intent=getIntent();
        isEditMode=intent.getBooleanExtra("isEditMode",false);

        Log.d(TAG, "onCreate: isEditMode "+isEditMode);

        if (isEditMode){

            //Edit Ads model: Get the Ads id for editing the Ads
            adsIdForEditing=intent.getStringExtra("adsId");

            //Function call to load Ads details by using Ad id
            loadAdsDetails();

            //change the toolbarTitleTv and postAdBtn button text
            binding.toolbarTitleTv.setText("Update Product");
            binding.postAdBtn.setText("Update Product");
        }else {
            //new Ads Menu: change toolbarTitleTv and postAdBtn button text
            binding.toolbarTitleTv.setText("Launch Product");
            binding.postAdBtn.setText("Launch Product");
        }

        //init imagePickedArrayList
        imagePickedArrayList = new ArrayList<>();
        //loadImages
        loadsImages();

        //Handle toolbarBackBtn click, to go back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        //Handle toolbarAddImageBtn click, show image add option (Gallery/Camera)
        binding.toolbarAddImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickOptions();
            }
        });

        //Handled locationAct click, launch MapActivity to pick location from map
        binding.locationAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LaunchProductActivity.this, MapActivity.class);
                locationPickerActivityResultLauncher.launch(intent);
            }
        });

        binding.postAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }


    private ActivityResultLauncher<Intent> locationPickerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    Log.d(TAG, "onActivityResult: ");
                    
                    //get result of location pick from MapActivity
                    if (result.getResultCode()==Activity.RESULT_OK){
                        
                         Intent data=result.getData();
                         
                         if (data!=null){
                             latitude=data.getDoubleExtra("latitude",0.0);
                             longitude=data.getDoubleExtra("longitude",0.0);
                             address=data.getStringExtra("address");

                             Log.d(TAG, "onActivityResult: latitude "+latitude);
                             Log.d(TAG, "onActivityResult: longitude "+longitude);
                             Log.d(TAG, "onActivityResult: address "+address);

                             binding.locationAct.setText(address);
                         }
                         
                    }else {
                        Log.d(TAG, "onActivityResult: Cancelled..");
                        Utils.toast(LaunchProductActivity.this,"Cancelled..");
                    }
                }
            }
    );


    private void loadsImages() {
        Log.d(TAG, "loadsImages: ");

        //init setup adapterImagePicked to set it RecyclerView i.e imagesRv, Param:1 is Context, Param:2 is Images list to show in Recycler
        adapterImagePicked = new AdapterImagePicked(this, imagePickedArrayList,adsIdForEditing);
        //set the adapter to the RecyclerView i.e imageRv
        binding.imageRv.setAdapter(adapterImagePicked);
    }

    private void showImagePickOptions() {
        Log.d(TAG, "showImagePickOptions: ");
        /*init popup menu
        param#1  is context and
        Param#2 is Anchor View for this popup. Popup will appear below the anchor if there is room, or above if there is not room
        param#3 is id of the Ads
         */
        PopupMenu popupMenu = new PopupMenu(this, binding.toolbarAddImageBtn);

        popupMenu.getMenu().add(Menu.NONE, 1, 2, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get id of the menu item clicked in popup menu
                int itemId = menuItem.getItemId();

                //check which item id is click from popup menu. 1-Camera and 2-Gallery
                if (itemId == 1) {
                    //Camera is click, we need to check if we have permission of camera,Gallery before launching Camera to capture image

                    if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.TIRAMISU){
                        //Device version is TIRAMISU or above. We only need Camera Permission
                        String[] cameraPermission = new String[]{Manifest.permission.CAMERA};
                        requestCameraPermission.launch(cameraPermission);
                    }else {
                        String[] cameraPermission =new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestCameraPermission.launch(cameraPermission);
                    }

                } else if (itemId == 2) {
                    //Gallery is click, we need to check if we have permission of storage before lauching Gallery to pick image

                    if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.TIRAMISU){
                        //Device version is TIRAMISU or above. We don't need Storage Permission to launch Gallery
                        pickImageGallery();
                    }else {
                        //Device version is below TIRAMISU. We need Storage permission to launch Gallery
                        String storagePermission =Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        requestStoragePermission.launch(storagePermission);
                    }

                }
                return true;
            }
        });
    }

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted " + isGranted);

                    //Let's check permission is granted or not
                    if (isGranted) {
                        //Storage Permission is granted, we can now launch Gallery to pick up image
                        pickImageGallery();

                    }else {
                        //Storage Permission is not granted, we can't launch Gallery to pick up image
                        Utils.toast(LaunchProductActivity.this,"Storage Permission Is Denied..");
                    }

                }
            }
    );

    private ActivityResultLauncher<String[]> requestCameraPermission =registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: ");
                    Log.d(TAG, "onActivityResult: "+result.toString());

                    //Let's check if permission is granted or not
                    boolean areAllGranted = true;

                    for (Boolean isGranted : result.values()){
                        areAllGranted= areAllGranted && isGranted;
                    }

                    if (areAllGranted){
                        //All permission Camera,Storage are granted, we can now launch Camera to Capture images
                        pickImageCamera();
                    }else {
                        //All permission Camera,Storage are denied, we cann't launch Camera to Capture images
                        Utils.toast(LaunchProductActivity.this,"Camera or Gallery or both permission denied..");
                    }
                }
            }
    );
    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");

        Intent intent=new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");

        ContentValues contentValues=new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"TEMPORARY_IMAGE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"TEMPORARY_IMAGE_DESCRIPTION");

        imageUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");

                    //check if the image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK){

                        //get data from result param
                        Intent data=result.getData();
                        //get Uri of the image picked
                        imageUri=data.getData();


                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        //timestamp will be used as a id of the image picked
                        String timestamp=""+Utils.getTimestap();
                        //Setup model for images, param:1 is id, param:2 is imageUri, param:3 is imageUrl,fromInternet
                        ModelImagePicked modelImagePicked=new ModelImagePicked(timestamp,imageUri,null,false);
                        imagePickedArrayList.add(modelImagePicked);
                        //reload Images
                        loadsImages();
                    }else {
                        //Cancelled
                        Log.d(TAG, "onActivityResult: Cancelled....!");
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    Log.d(TAG, "onActivityResult: ");

                    //check if the image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK){



                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);

                        //timestamp will be used as a id of the image picked
                        String timestamp=""+Utils.getTimestap();

                        //Setup model for images, param:1 is id, param:2 is imageUri, param:3 is imageUrl,fromInternet
                        ModelImagePicked modelImagePicked=new ModelImagePicked(timestamp,imageUri,null,false);
                        imagePickedArrayList.add(modelImagePicked);

                        //reload Images
                        loadsImages();
                    }else {
                        //Cancelled
                        Log.d(TAG, "onActivityResult: Cancelled....!");
                    }
                }
            }
    );

    private String brand="";
    private String category="";
    private String condition="";
    private String address="";
    private String price="";
    private String title="";
    private String description="";
    private double latitude=0;
    private double longitude=0;

    private void validateData(){
        Log.d(TAG, "validateData: ");

        brand=binding.brandEt.getText().toString().trim();
        category=binding.categoryAct.getText().toString().trim();
        condition=binding.conditonAct.getText().toString().trim();
        address=binding.locationAct.getText().toString().trim();
        price=binding.priceEt.getText().toString().trim();
        title= binding.titleEt.getText().toString().trim();
        description=binding.descriptionEt.getText().toString().trim();

        if (brand.isEmpty()){
            //no brand entered in brandEt, show error in brandEt and focus
            binding.brandEt.setError("Enter Brand");
            binding.brandEt.requestFocus();
        } else if (category.isEmpty()) {
            //no category entered in categoryEt, show error in categoryEt and focus
            binding.categoryAct.setError("Choose CategoryFragment");
            binding.categoryAct.requestFocus();
        } else if (condition.isEmpty()) {
            //no condition entered in conditionAct, show error in conditionAct and focus
            binding.conditonAct.setError("Choose Condition");
            binding.conditonAct.requestFocus();
        }  else if (address.isEmpty()) {
            //no location choose in locationAct, show error in locationAct and focus
              binding.locationAct.setError("Choose Location");
              binding.locationAct.requestFocus();
        }  else if (title.isEmpty()) {
            //no title entered in titleEt, show error in titileEt and focus
            binding.titleEt.setError("Provide Title");
            binding.titleEt.requestFocus();
        } else if (description.isEmpty()) {
            //no description entered in descriptionEt, show error in descriptionEt and focus
            binding.descriptionEt.setError("Provide Description");
            binding.descriptionEt.requestFocus();
        } else if (imagePickedArrayList.isEmpty()) {
            Utils.toast(this,"Pick At-least 3 images");
        } else {
            //all data is validate, we can now process further now
            if (isEditMode){
                updateAds();
            }else {
                postAds();
            }
        }

    }

    private void updateAds() {
        progressDialog.setMessage("Updating Ad");
        progressDialog.dismiss();


        //setup data to add in firebase database
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("brand",""+brand);
        hashMap.put("category",""+category);
        hashMap.put("condition",""+condition);
        hashMap.put("address",""+address);
        hashMap.put("price",""+price);
        hashMap.put("title",""+title);
        hashMap.put("description",""+description);
        hashMap.put("latitude",+latitude);
        hashMap.put("longitude",+longitude);

        //DB path to update Ads. Ads-->AdsId-->DataToUpdate
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adsIdForEditing)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Ads data updated success
                        progressDialog.dismiss();

                        //start uploading images piked for Ads
                        uploadImageStorage(adsIdForEditing);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {//////////////////////////////////////
                        //Ads data update failed
                        Log.e(TAG, "onFailure: ",e);
                        Utils.toast(LaunchProductActivity.this,"Failed To Update Ad Due To "+e.getMessage());
                    }
                });

    }

    private void postAds() {
        Log.d(TAG, "postAds: ");

        //show progressDialog
        progressDialog.setMessage("Publishing Ads");
        progressDialog.show();

        long timestamp =Utils.getTimestap();

        //firebase database Ads reference to store new ads
        DatabaseReference refAds= FirebaseDatabase.getInstance().getReference("Ads");
        //key id from the reference to used as a Ads id
        String keyId=refAds.push().getKey();

        //setup data to add in firebase database
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("id",""+keyId);
        hashMap.put("uid",""+firebaseAuth.getUid());
        hashMap.put("brand",""+brand);
        hashMap.put("category",""+category);
        hashMap.put("condition",""+condition);
        hashMap.put("address",""+address);
        hashMap.put("price",""+price);
        hashMap.put("title",""+title);
        hashMap.put("description",""+description);
        hashMap.put("status",""+Utils.ADS_STATUS_AVAILABLE);
        hashMap.put("timestamp",+timestamp);
        hashMap.put("latitude",+latitude);
        hashMap.put("longitude",+longitude);

        //set data to firebase database Ads-->AdsId-->AdsDataJSON
        refAds.child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Ads Published Successfully..");

                        uploadImageStorage(keyId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ",e);
                        progressDialog.dismiss();
                        Utils.toast(LaunchProductActivity.this,"Failed to published Ads due to "+e.getMessage());
                    }
                });
    }

    private void uploadImageStorage(String adsId) {
        Log.d(TAG, "uploadImageStorage: ");

        //there are multiple images in the imagePickedArrayList , loop to upload all
        for (int i=0; i<imagePickedArrayList.size(); i++) {

            //get model from the current position of the imagePickedArrayList
            ModelImagePicked modelImagePicked = imagePickedArrayList.get(i);

            //upload images only if picked from gallery/camera

            if (!modelImagePicked.isFromInternet()){           /////////////////////////////////////

                //for name of the images in firebase database
                String imageName=modelImagePicked.getId();

                //path and name of the images in firebase database
                String filePathAndName="Ads/"+imageName;

                int imageIndexForProgress=i+1;

                //StorageReference with filePathAndName
                StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathAndName);

                storageReference.putFile(modelImagePicked.getImageUri())
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                //calculate the current progress of the image being uploaded
                                double progress=(100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                //setup progress dialog message on basis of current progress. e.g Uploading 1 of the 10 images.. progress 95%
                                String message = "Uploading " + imageIndexForProgress + " Of " + imagePickedArrayList.size() + "images...\nProgress " + (int)progress + "%";

                                Log.d(TAG, "onProgress: Message "+message);

                                //show progress
                                progressDialog.setMessage(message);
                                progressDialog.show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                Log.d(TAG, "onSuccess: ");

                                //image uploaded, get Uri of the uploaded images
                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful());
                                Uri uploadedImageUrl=uriTask.getResult();

                                if (uriTask.isSuccessful()){

                                    HashMap<String,Object> hashMap =new HashMap<>();
                                    hashMap.put("id",""+modelImagePicked.getId());
                                    hashMap.put("imageUrl",""+uploadedImageUrl);

                                    //add in firebase database. Ads-->AdsId-->imageId-->ImageData
                                    DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Ads");
                                    ref.child(adsId).child("Images")
                                            .child(imageName)
                                            .updateChildren(hashMap);
                                }

                                progressDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: ",e);
                                progressDialog.dismiss();
                            }
                        });

            }

        }
    }

    private void loadAdsDetails(){
        Log.d(TAG, "loadAdsDetails: ");

        //Ads DB path do get the Ads details, Ads-->AdsId
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adsIdForEditing)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //get the Ads details from the Firebase DB, spelling should be same as in Firebase DB
                        String brand=""+ snapshot.child("brand").getValue();
                        String category=""+ snapshot.child("category").getValue();
                        String condition=""+ snapshot.child("condition").getValue();
                        latitude = (Double) snapshot.child("latitude").getValue();
                        longitude = (Double) snapshot.child("longitude").getValue();
                        String address= ""+ snapshot.child("address").getValue();
                        String price= ""+ snapshot.child("price").getValue();
                        String  title= ""+ snapshot.child("title").getValue();
                        String description= ""+ snapshot.child("description").getValue();

                        //set data to UI View (From Firebase)
                        binding.brandEt.setText(brand);
                        binding.categoryAct.setText(category);
                        binding.conditonAct.setText(condition);
                        binding.locationAct.setText(address);
                        binding.priceEt.setText(price);
                        binding.titleEt.setText(title);
                        binding.descriptionEt.setText(description);

                        //load the Ads images. Ads-->Adsid-->images
                        DatabaseReference referenceImages=snapshot.child("Images").getRef();
                        referenceImages.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                //might be multiple images so loop to get all
                                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                                    String id = ""+dataSnapshot.child("id").getValue();
                                    String imageUrl = ""+dataSnapshot.child("imageUrl").getValue();

                                    ModelImagePicked modelImagePicked=new ModelImagePicked(id,null,imageUrl,true);
                                    imagePickedArrayList.add(modelImagePicked);

                                }

                                loadsImages();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}