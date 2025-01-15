package com.example.creativecart_app.Fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.creativecart_app.adapters.AdapterAds;
import com.example.creativecart_app.databinding.FragmentCardWishlistBinding;
import com.example.creativecart_app.models.ModelAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CartWishListFragment extends Fragment {

    //View Binding
    private @NonNull FragmentCardWishlistBinding binding;

    //Context for this fragment class
    private Context mContext;

    //TAG to show log in Logcat
    private static final String TAG="MY_ADS_TAG";

    //ArrayList to hold Ads list by current logged in users to show in RecyclerView
    private ArrayList<ModelAds> adsArrayList;

    //Firebase for Firebase Auth related task
    private FirebaseAuth firebaseAuth;

    //Adapter class instance to set to RecyclerView to show Ads list
    private AdapterAds adapterAds;

    public CartWishListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the Contextn for this fragment class
        mContext=context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout (fragment_card_wishlist.xml) for this fragment
        binding= FragmentCardWishlistBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Firebase for Firebase Auth related task
        firebaseAuth =FirebaseAuth.getInstance();

        //function call to load Ads by current logged in user
        loadAds();

        //addTextChangedListener to searchEt to search Ads using Filter applied in Adapter class
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                //this function is called whenever user type a letter, search based on whatever user typed
                try {

                    String query = charSequence.toString();
                    adapterAds.getFilter().filter(query);
                }catch (Exception e){
                    Log.e(TAG, "onTextChanged: ",e);
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

        //Firebase DB listener to load Ads by currently logged in user. i.e. show only those Ads whose key id is equal to the uid of the currently logged in user
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Ads");
      //  reference.orderByChild("uid").equalTo(firebaseAuth.getUid())
                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //clear adsArrayList each time starting adding data into it
                        adsArrayList.clear();

                        //load Ads list
                        for (DataSnapshot dataSnapshot: snapshot.getChildren()){

                            try {
                                //prepare ModelAds with all data from Firebase DB
                                ModelAds modelAds = dataSnapshot.getValue(ModelAds.class);
                                //add prepared modelAds to adsArrayList
                                adsArrayList.add(modelAds);
                            }catch (Exception e){
                                Log.e(TAG, "onDataChange: ",e);
                            }
                        }

                        //init/setup AdapterAds class and  set to recyclerview
                        adapterAds = new AdapterAds(mContext,adsArrayList);
                        binding.adsRv.setAdapter(adapterAds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


}