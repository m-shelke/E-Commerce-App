package com.example.creativecart_app.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.example.creativecart_app.Fragment.AccountsFragment;
import com.example.creativecart_app.Fragment.CardFragment;
import com.example.creativecart_app.Fragment.CategoryFragment;
import com.example.creativecart_app.Fragment.HomeFragment;
import com.example.creativecart_app.R;
import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    //instance of the BroadcastReceiver
    private BroadcastReceiver broadcastReceiver;

    ActivityMainBinding binding;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //activity_main.xml = ActivityMainBinding
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initiating BroadcastReceiver with noInternet() class
        broadcastReceiver = new noInternet();

        //For Activity start and close
        registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

//        Glide.with(this).setLogLevel(Log.VERBOSE); //setloglevel not available


        //get instance of the firebase auth for firebase auth related task
        firebaseAuth=FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser()==null){
            //user is not logged in, move to LoginActivity
           startLoginOption();
        }

        //by default (when app is open) show HomeFragment
        showHomeFragment();

        //handle bottomNv item clicks to navigiates between fragment
        binding.bottomNv.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                //get id of the menu iten clicked
                int itemId = item.getItemId();


                if(itemId== R.id.menu_home){
                    //home item clicke,show HomeFragment
                    showHomeFragment();
                    return true;

               }
                else if (itemId==R.id.menu_chats) {
                    //chat item clicked, show ChatFragment
                    if (firebaseAuth.getCurrentUser()==null){
                        Utils.toast(MainActivity.this,"Login Required...");
                        startLoginOption();
                        return false;
                    }
                    else {
                        showChatsFragment();
                        return true;
                    }

                }


               else if (itemId==R.id.menu_my_ads) {
                    //myAds item clicked, show CardFragment

                    if (firebaseAuth.getCurrentUser()==null){
                        Utils.toast(MainActivity.this,"Login Required...");
                        startLoginOption();
                        return false;
                    }else {
                        showMyAdsFragment();
                        return true;
                    }

                } else if (itemId==R.id.menu_account) {
                    //account item clicked, show AccountFragment

                    if (firebaseAuth.getCurrentUser()==null){
                        Utils.toast(MainActivity.this,"Login Required...");
                        startLoginOption();
                        return false;
                    }else {
                        showAccountFragment();
                        return true;
                    }

                }else {


                    return false;
                }
            }
        });

        //Handle sellFab click, start LaunchProductActivity to add create a new add
        binding.sellFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Edit clicked, start AdsCreateActivity with Ads Id and isEditMode as true
                Intent intent=new Intent(MainActivity.this, LaunchProductActivity.class);
                intent.putExtra("isEditMode",false);
                startActivity(intent);
            }
        });

    }

    //for destroying the activity and for unregisterReceiver
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver for passed BroadcastReceiver object
        unregisterReceiver(broadcastReceiver);
    }

    private void showHomeFragment() {
        //change toolbar textview text/title to Home
      //  binding.toolbarTitleTv.setText("Home");

        //show fragment
        HomeFragment homeFragment=new HomeFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(),homeFragment,"HomeFragment");
        fragmentTransaction.commit();
    }
    private void showChatsFragment(){
        //change toolbar textview text/title to Chats
       // binding.toolbarTitleTv.setText("Chats");
        CategoryFragment categoryFragment =new CategoryFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), categoryFragment,"CategoryFragment");
        fragmentTransaction.commit();

    }
    private void showMyAdsFragment(){
        //change toolbar textview text/title to MyAds
     //   binding.toolbarTitleTv.setText("MyAds");

        //show fragment
        CardFragment cardFragment =new CardFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), cardFragment,"CardFragment");
        fragmentTransaction.commit();

    }
    private void showAccountFragment(){
        //change toolbar textview text/title to Account
      //  binding.toolbarTitleTv.setText("Account");

        //show fragment
        AccountsFragment accountsFragment=new AccountsFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(),accountsFragment,"AccountFragment");
        fragmentTransaction.commit();
    }
    private void startLoginOption(){
        //MainActivity to LoginActivity
        startActivity(new Intent(this, LoginActivity.class));
    }
}