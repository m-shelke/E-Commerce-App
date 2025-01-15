package com.example.creativecart_app.Fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.creativecart_app.activity.ChangePasswordActivity;
import com.example.creativecart_app.activity.DeleteAccountActivity;
import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.activity.MainActivity;
import com.example.creativecart_app.R;
import com.example.creativecart_app.activity.ProfileEditActivity;
import com.example.creativecart_app.databinding.FragmentAccountsBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountsFragment extends Fragment {

    private FragmentAccountsBinding binding;
    private FirebaseAuth firebaseAuth;
    private Context mContext;
    private ProgressDialog progressDialog;
    private static final String TAG="ACCOUNT_TAG";

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init Context for this Fragment class
        mContext=context;
        super.onAttach(context);
    }

    public AccountsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding=FragmentAccountsBinding.inflate(LayoutInflater.from(mContext),container,false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //init/setup ProgressDialog to while Account Verification
        progressDialog=new ProgressDialog(mContext);
        progressDialog.setTitle("Please Wait..");
        progressDialog.setCanceledOnTouchOutside(false);

        //get instance of the firebase auth for Auth related task
        firebaseAuth=FirebaseAuth.getInstance();

        loadMyInfo();


        //handle logoutBtn click, logout user and start MainActivity
        binding.logOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //logout user
                firebaseAuth.signOut();

                //start MainActivity
                startActivity(new Intent(mContext, MainActivity.class));
                getActivity().finishAffinity();
            }
        });

        binding.editProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
           startActivity(new Intent(mContext, ProfileEditActivity.class));
            }
        });

        //Handle changepasswordBtn click, start ChangePasswordActivity
        binding.changePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, ChangePasswordActivity.class));
            }
        });

//        handling referredBtn button event
        binding.refferedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT,"Reffred this App for Shop,com.example.creativecart_app");
                startActivity(Intent.createChooser(intent,"Thank You.."));
            }
        });

        //Handle verifyAccounyBtn click, start VerifyAccountActivity
        binding.verifyAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyAccount();
            }
        });

        //Handle deleteAccountBtn click, Start DeleteAccountActivity
        binding.deleteAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              startActivity(new Intent(mContext, DeleteAccountActivity.class));
              getActivity().finishAffinity(); //remove all activities from back-stack because, we will delete user and it's data, so it may produced null exception if we dont remove it
            }
        });

    }

    private void loadMyInfo() {
        //Reference of the current User Info in Firebase Realtime Database to get the User Info
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get User Info, spelling shoud be as in Firebase realtime database
                        String dob=""+snapshot.child("dob").getValue();
                        String email=""+snapshot.child("email").getValue();
                        String name=""+snapshot.child("name").getValue();
                        String phoneNumber=""+snapshot.child("phoneNumber").getValue();
                        String profileImageUrl=""+snapshot.child("profileImageUrl").getValue();
                        String phoneCode=""+snapshot.child("phoneCode").getValue();
                        String timestap=""+snapshot.child("timestap").getValue();
                        String userType=""+snapshot.child("userType").getValue();

                        //Concatenate phone code and phone number to make full phone number
                        String phone=phoneCode+phoneNumber;

                        //to avoid null or format exception
                        if (timestap.equals("null")){
                            timestap="0";
                        }
                        //format timestamp to dd/mm/yyyy
                        String formatedDate= Utils.formatTimestampDate(Long.parseLong(timestap));

                        //set data to UI
                        binding.emailTV.setText(email);
                        binding.nameTv.setText(name );
                      //  binding.dobTv.setText(dob);
                        binding.phoneTv.setText(phone);
                        binding.memberSinceTv.setText(formatedDate);

                        //check user type i.e Email/Phone/Google. In case of Phone and Google Account is already Verified, but in case of Email account user have to verify
                        if (userType.equals("Email")){

                            //User type is Email, have to check if verified or not
                            boolean isVerified=firebaseAuth.getCurrentUser().isEmailVerified();

                            if (isVerified){
                                //verified, hide the Verify Account Option
                                binding.verifyAccountBtn.setVisibility(View.GONE);
                                binding.verificationTv.setText("Verified");
                            }else {
                                //not verified, show the Verify Account Option
                                binding.verifyAccountBtn.setVisibility(View.VISIBLE);
                                binding.verificationTv.setText("Not Verified");
                            }
                        }else {
                            //User type is Google or Phone, no need to check if verified or not as it's already verified and hide Verify Account Option
                            binding.verifyAccountBtn.setVisibility(View.GONE);
                            binding.verificationTv.setText("Verified");
                        }

                        try {

                            RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);


                            Glide.with(mContext)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .apply(requestOptions)
                                    .listener(new RequestListener<Drawable>() {

                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
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
                        //error
                    }
                });
    }

    private void verifyAccount() {
        Log.d(TAG, "verifyAccount: ");
        //show progressDialog
        progressDialog.setMessage("Sending Account Verification Instruction To Your Email ");
        progressDialog.show();
        //send account/email verification instruction to the registered email
        firebaseAuth.getCurrentUser().sendEmailVerification()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    //instruction send, check email, sometimes it goes to the spam folder so if not into inbox please check the spam folder
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: ");
                        progressDialog.dismiss();
                        Utils.toast(mContext,"Account Verification Instruction Send To Your Email ");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to send instruction
                        Log.e(TAG, "onFailure: ",e);
                        progressDialog.dismiss();
                        Utils.toast(mContext,"Failed Due To "+e.getMessage());
                    }
                });
    }
}