package com.example.creativecart_app.activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.creativecart_app.Fragment.AccountsFragment;
import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.R;
import com.example.creativecart_app.databinding.ActivityProfileEditBinding;
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

import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;
    private static final String TAG="PROFILE_EDIT_TAG";

    //FirebaseAuth for Auth related task
    private FirebaseAuth firebaseAuth;

    //ProgressDialog:to show while Profile Update
    private ProgressDialog progressDialog;
    private String myUserType="";
    private Uri imageUri=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity_profile_edit.xml=ActivityProfileEditBinding
        binding=ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get instance of the Firebase fo auth related task
        firebaseAuth=FirebaseAuth.getInstance();

        //progressDialog: to show while profile update
        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Please Wait..");
        progressDialog.setCanceledOnTouchOutside(false);

        loadMyInfo();

        //Handled toolbarBackBtn button click, and go back.
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //Handle profileImagePickFab click, show image pick popup menu
        binding.profileImagePickFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagePickDialog();
            }
        });

        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    String name="";
    String dob="";
    String email="";
    String phoneCode="";
    String phoneNumber="";

    //input data
    private void validateData() {
        name=binding.nameEt.getText().toString().trim();
     //   dob=binding.dobEt.getText().toString().trim();
        email=binding.emailEt.getText().toString().trim();
        phoneCode=binding.coutryCodePicker.getSelectedCountryCodeWithPlus();
        phoneNumber=binding.phoneNumberEt.getText().toString().trim();

        //Validate data
        if (imageUri==null){
            //no image to upload to storage, just update Database
            updateProfileDb(null);
        }else {
            //image need to upload storage, first upload image then update Database
            uploadProfileImageStorage();
        }
    }

    private void uploadProfileImageStorage(){
        Log.d(TAG, "uploadProfileImageStorage: ");

        //show progress
        progressDialog.setMessage("Uploading User Image..");
        progressDialog.show();

        //setup image and path e.g UserImage/profile_userid
        String filePathAndName="UserImages/"+"profile"+firebaseAuth.getUid();

        //StorageReference to upload image
        StorageReference reference= FirebaseStorage.getInstance().getReference().child(filePathAndName);
        reference.putFile(imageUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progress=(100.0*snapshot.getBytesTransferred()) /snapshot.getTotalByteCount();
                        Log.d(TAG, "onProgress: Process: "+progress);

                        progressDialog.setMessage("Uploading Profile Image. Progress: "+(int) progress + "%");
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image upload successfully, get Url of upload image
                        Log.d(TAG, "onSuccess: Uploaded..!!");

                        Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();

                        while (!uriTask.isSuccessful());
                        String uploadedImageUrl=uriTask.getResult().toString();

                        if (uriTask.isSuccessful()){
                            updateProfileDb(uploadedImageUrl);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed to upload image
                        Log.e(TAG, "onFailure: ",e);
                        progressDialog.dismiss();

                        Utils.toast(ProfileEditActivity.this,"Failed to Upload Profile Image due to "+e.getMessage());
                    }
                });
    }

    private void updateProfileDb(String imageUrl){
        //show progressDialog
         progressDialog.setMessage("Updating user info");
         progressDialog.show();

         //setup data in hashMap to update to Firebase Database
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("name",""+name);
        hashMap.put("dob",""+dob);

        if (imageUrl !=null){
            //update profileImageUrl in the Database only if uploaded image url is not null
            hashMap.put("profileImageUrl"," "+imageUrl);
        }

        //if user type is phone then allow to update email otherwise (in case of Google or Email) allow to update phone
        if (myUserType.equalsIgnoreCase("phone")){          //if User Type is phone allow to update Email not phone
            hashMap.put("email",""+email);
        } else if (myUserType.equalsIgnoreCase("email") || myUserType.equalsIgnoreCase("Google")) {
            hashMap.put("phoneCode",phoneCode);
            hashMap.put("phoneNumber",phoneNumber);
        }

        //DatabaseReference of user to update info
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //update Successully
                        Log.d(TAG, "onSuccess: Info Updated");
                        progressDialog.dismiss();

                        Utils.toast(ProfileEditActivity.this,"Profile Updted..");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //update failed
                        Log.e(TAG, "onFailure: ",e);
                        progressDialog.dismiss();

                        Utils.toast(ProfileEditActivity.this,"Failed Update user info due to "+e.getMessage());
                    }
                });

    }

    private void loadMyInfo() {
        Log.d(TAG, "loadMyInfo: ");
        //get reference of current user info in Firebase Realtime Database to get user Info
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //get User Info, spelling should be as in Firebase realtime database
               // String dob=""+snapshot.child("dob").getValue();
                String email=""+snapshot.child("email").getValue();
                String name=""+snapshot.child("name").getValue();
                String phoneCode=""+snapshot.child("phoneCode").getValue();
                String phoneNumber=""+snapshot.child("phoneNumber").getValue();
                String profileImageUrl=""+snapshot.child("profileImageUrl").getValue();
                myUserType=" "+snapshot.child("userType").getValue();

                //Concatenate phone code and phone number to make full phone number
                String phone= phoneCode+phoneNumber;

                //check User Type, if Email/Google then don't allow user to edit/update email
                if (myUserType.equalsIgnoreCase("email")|| myUserType.equalsIgnoreCase("Google")){

                    //User Type Email/Google, don't allow to edit
                    binding.emailTil.setEnabled(false);
                    binding.emailEt.setEnabled(false);
                }else {

                    //User Type is Phone, Don't allow to edit phone
                    binding.phoneNumberTil.setEnabled(false);
                    binding.phoneNumberEt.setEnabled(true);
                    binding.coutryCodePicker.setEnabled(false);

                }
                //set data to UI
                binding.emailEt.setText(email);
              //  binding.dobEt.setText(dob);
                binding.phoneNumberEt.setText(phone);
                binding.nameEt.setText(name);
                try {

                    int phoneCodeInt=Integer.parseInt(phoneCode.replace("+","")); //e.g +92---->+91
                    binding.coutryCodePicker.setCountryForPhoneCode(phoneCodeInt);

                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: ", e);
                }

                try {

                    RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

                    Glide.with(ProfileEditActivity.this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.baseline_circle_24)
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
                            .into(binding.profileIv);

                }catch (Exception e){
                    Log.e(TAG, "onDataChange: ",e);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void imagePickDialog(){
        //init popup menu param#1  is context and Param#2 is the UI View (profileImagePickFab) to above or below we need to show popup menu
        PopupMenu popupMenu=new PopupMenu(this,binding.profileImagePickFab);
        //add menu items to our popup mrnu Param#1 is GroupId,Param#2 is ItemId,Param#3 is OrderID,Param#4 Menu Item Title
       popupMenu.getMenu().add(Menu.NONE,1,1,"Camera");
       popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");

       //show popup menu
       popupMenu.show();

       //Handle popup menu item click
       popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
           @Override
           public boolean onMenuItemClick(MenuItem menuItem) {
               //get id of the menu item clicked
               int itemId=menuItem.getItemId();

               if (itemId==1){
                   //Camera is clicked we need to check if we have permission of Camera,Storage before launching Camera to capture Image
                   Log.d(TAG, "onMenuItemClick: Camera clicked, check if camera permission granted or not");

                   if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
                       //Device version is TIRAMISU or above. We only need Camera Permission
                       requestCameraPermission.launch(new String[]{Manifest.permission.CAMERA});
                   }else {
                       //Device version is below TIRAMISU. We need Camera and Storage Permission
                       requestCameraPermission.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE});
                   }
               } else if (itemId==2) {
                   Log.d(TAG, "onMenuItemClick: Check if Storage permission is granted or not");

                   if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
                        pickImageGallery();
                   }else {
                       requestsStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                   }
               }
               return true;
           }
       });
    }


   private ActivityResultLauncher<String[]> requestCameraPermission= registerForActivityResult(
           new ActivityResultContracts.RequestMultiplePermissions(),
           new ActivityResultCallback<Map<String, Boolean>>() {
               @Override
               public void onActivityResult(Map<String, Boolean> result) {

                   Log.d(TAG, "onActivityResult: "+result.toString());

                   //Let's check if permission granted or not
                   boolean areAllGranted=true;
                  for (Boolean isGranted: result.values()){
                      areAllGranted =areAllGranted && isGranted;
                  }

                  if (areAllGranted){
                      //Camera or Storage or both permission granted, we can now launch camera to capture image
                      Log.d(TAG, "onActivityResult: All Granted e.g Cameta, Storage");
                      pickImageCamera();
                  }else {
                      //Camera or Storage or both permission denied, can not camera to capture image
                      Log.d(TAG, "onActivityResult: All or Either one is denied");
                      Utils.toast(getApplicationContext(),"Camera or Storage or both permission denied");
                  }
               }

           }
   );

    private ActivityResultLauncher<String> requestsStoragePermission=registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted: "+isGranted);

                 //Let's check if permission is granted or not
                    if (isGranted){
                        //  Storage Permission granted, we can now launch Gallery to pick Image
                        pickImageGallery();
                    }else {
                        //Storage Permission denied, we can't launch  Gallery to picked Image
                        Utils.toast(ProfileEditActivity.this,"Storage Permission denied..");
                    }
                }
            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");

        //setup Content Values,MediaStore to capture high quality image using Camera Intent
        ContentValues contentValues=new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"TEMP_TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"TEMP_DESCRIPTION");

        imageUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        //Intent to launch Camera
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher=registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    //check if image capture or not
                    if (result.getResultCode()== Activity.RESULT_OK){
                        //Image Captured, we have image in imageUri as asinged in PickImageCamera()
                        Log.d(TAG, "onActivityResult: Image Capture "+imageUri);

                        //set to profileIv
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .into(binding.profileIv);
                        }catch (Exception e){
                            Log.e(TAG, "onActivityResult: ",e);
                        }
                    }else {
                        //canceled
                        Utils.toast(ProfileEditActivity.this,"Canceled..");
                    }
                }
            }
    );

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");

        //Intent to launch Image Picker e.g.Gallery
        Intent intent=new Intent(Intent.ACTION_PICK);
        //We only want to picked Image
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher=registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    //check, if image is picked or not
                    if (result.getResultCode()==Activity.RESULT_OK){
                        //get data
                        Intent data=result.getData();
                        //get Uri of image picked
                        imageUri=data.getData();
                        Log.d(TAG, "onActivityResult: Image Picked from Gallery "+imageUri);

                        //set to profileIv
                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .into(binding.profileIv);

                        }catch (Exception e){
                            Log.e(TAG, "onActivityResult: ",e);
                        }

                    }else {
                        //Canceled
                        Utils.toast(ProfileEditActivity.this,"Canceled...");
                    }

                }
            }
    );
}