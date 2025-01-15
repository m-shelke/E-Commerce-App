package com.example.creativecart_app.Fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.creativecart_app.adapters.AdapterAds;
import com.example.creativecart_app.databinding.FragmentShoplistBinding;
import com.example.creativecart_app.models.ModelAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CardShopListFragment extends Fragment {
    //View Binding
    private @NonNull FragmentShoplistBinding binding;

    //TAG to show logs in Logcat
    private static final String TAG="FAV_TAG";

    //Context of this fragment class
    private Context mContext;

    //Firebase for Firebase Auth related task
    private FirebaseAuth firebaseAuth;

    //ArrayList to hold Ads list by current logged in users to show in RecyclerView
    private ArrayList<ModelAds> adsArrayList;

    //Adapter class instance to set to RecyclerView to show Ads list
    private AdapterAds adapterAds;
    public CardShopListFragment() {
        // Required empty public constructor
    }  

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the Context for this fragment class
        mContext=context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate/bind the layout for this fragment
        binding = FragmentShoplistBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Firebase for Firebase Auth related task
        firebaseAuth = FirebaseAuth.getInstance();

        //function call to load Ads by current logged in user
        loadAds();

        //addTextChangedListener to searchEt to search Ads using Filter applied in Adapter class
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    try {
                        //this function is called whenever user type a letter, search based on whatever user typed
                        String query = charSequence.toString();
                        adapterAds.getFilter().filter(query);
                    } catch (Exception e) {
                        Log.e(TAG, "onTextChanged: ", e);
                    }

            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void loadAds() {
        Log.d(TAG, "loadAds: ");

        //init adsArrayList before starting adding data into it
        adsArrayList = new ArrayList<>();

        //Firebase DB listener to get the Id of  Ads added to favorites by current logged in user. e.g. Users-->Uid-->Favorites-->FAV_ADS_DATA
        DatabaseReference favreference = FirebaseDatabase.getInstance().getReference("Users");
        favreference.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear adsArrayList each time starting adding data into it
                        adsArrayList.clear();

                        //load favorites Ads ids
                        for (DataSnapshot dataSnapshot: snapshot.getChildren()){

                            //get id of the Ads. e.g Users-->Favorites-->AdsId
                            String adsId = ""+dataSnapshot.child("adsId").getValue();
                            Log.d(TAG, "onDataChange: adsId "+adsId);

                            //Firebase DB listener to load Ads details based on id of the Ads, we just get
                            DatabaseReference adsreference = FirebaseDatabase.getInstance().getReference("Ads");
                            adsreference.child(adsId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            try {
                                                //prepare ModelAds with all data from Firebase DB
                                                ModelAds modelAds = snapshot.getValue(ModelAds.class);

                                                Log.d(TAG, "onDataChange modelAds: "+modelAds.getTitle());

                                                //adding prepare model to Arraylist
                                                adsArrayList.add(modelAds);
                                                Log.d(TAG, "onDataChange adsArrayList: "+adsArrayList);

                                            }catch (Exception e){
                                                Log.e(TAG, "onDataChange: ",e);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }

                        //sometimes Fav Ads were not loading due to nested DB listener because we are getting data using 2 paths so added some delay e.g half second
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                               adapterAds = new AdapterAds(mContext,adsArrayList);
                               binding.adsRv.setAdapter(adapterAds);
                            }
                        },500);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}