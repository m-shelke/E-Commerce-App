package com.example.creativecart_app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.creativecart_app.activity.MapActivity;
import com.example.creativecart_app.adapters.AdapterAds;
import com.example.creativecart_app.adapters.AdapterCategory;
import com.example.creativecart_app.models.ModelAds;
import com.example.creativecart_app.models.ModelCategory;
import com.example.creativecart_app.LoginOptionActivity.RvListenerCategory;
import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.databinding.FragmentHomeBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment  {

    //View Binding
    private FragmentHomeBinding binding;

    //TAG to show in LogCat
    private static final String TAG="HOME_TAG";
    static final int MAX_DISTANCE_TO_LOAD_ADS_KM=10;

    //Context for this fragment class
    private Context mContext;
    private ArrayList<ModelAds> adsArrayList;
    private AdapterAds adapterAds;
    private SharedPreferences locationSp;

    private double currentLatitude=0.0;
    private double currentLongitude=0.0;
    private String currentAddress="";

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the context for this fragment class
        mContext=context;
        super.onAttach(context);
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding=FragmentHomeBinding.inflate(LayoutInflater.from(mContext),container,false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //init the SharedPreferences param1: is the name of the SharedPreferences file, param2: is the mode of SharedPreferences
         locationSp =mContext.getSharedPreferences("LOCATION_SP",Context.MODE_PRIVATE);
         //get the currentLatitude,currentLongitude,currentAddress from the SharedPreferences. In next step we will pick these info from map and save in it
         currentLatitude=locationSp.getFloat("CURRENT_LATITUDE",0.0F);
         currentLongitude=locationSp.getFloat("CURRENT_LONGITUDE",0.0f);
         currentAddress=locationSp.getString("CURRENT_ADDRESS","");

         //if current location is not 0 i.e location is picked
        if(currentLatitude!=0.0 && currentLongitude !=0.0){
            binding.locationTv.setText(currentAddress);
        }
        //function call,loadCategories();
          loadCategoris();
        //function call,  loadAds();
          loadAds("All");

          binding.searchEt.addTextChangedListener(new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

              }

              @Override
              public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                  Log.d(TAG, "beforeTextChanged: Query "+charSequence);

                  try {
                      String query =charSequence.toString();
                      adapterAds.getFilter().filter(query);
                  }catch (Exception e){
                      Log.e(TAG, "onTextChanged: ",e);
                  }
              }

              @Override
              public void afterTextChanged(Editable editable) {

              }
          });


          binding.locationCv.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Intent intent=new Intent(mContext, MapActivity.class);
                  locationPickerActivityResult.launch(intent);
              }
          });
    }

    private ActivityResultLauncher<Intent> locationPickerActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    //check if from map, location is pick or not
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Log.d(TAG, "onActivityResult: RESULT_OK");

                        Intent data = result.getData();

                        if (data != null){
                            Log.d(TAG, "onActivityResult: Location Picked");

                            //get location info from client
                            currentLatitude=data.getDoubleExtra("latitude",0.0);
                            currentLongitude=data.getDoubleExtra("longitude",0.0);
                            currentAddress=data.getStringExtra("address");

                            //save location info to SharedPreferences so when we launch app, next time we don't need to pick again
                            locationSp.edit()
                                    .putFloat("CURRENT_LATITUDE",Float.parseFloat(""+currentLatitude))
                                    .putFloat("CURRENT_LONGITUDE",Float.parseFloat(""+currentLongitude))
                                    .putString("CURRENT_ADDRESS",currentAddress)
                                    .apply();

                            //set the picked Address
                            binding.locationTv.setText(currentAddress);

                            //After picking Address reload all ads again based on newly picked location
                            loadAds("All");

                        }
                    }else {
                        Log.d(TAG, "onActivityResult: Canceled");
                        Utils.toast(mContext,"Canceled");
                    }
                }
            }
    );


    private void loadCategoris() {

        //init categoryArrayList
        ArrayList<ModelCategory> categoryArrayList= new ArrayList<>();

        //ModelCategory instance to show all products
      //  ModelCategory modelCategory=new ModelCategory("All",R.drawable.cate_expand);
      //  categoryArrayList.add(modelCategory);

        //Get categories from Utils class and in categoryArrayList
        for (int i=0;i< Utils.categories.length;i++){

            //ModelCategory category instance to get/hold category from current index
            ModelCategory modelCategory1=new ModelCategory(Utils.categories[i],Utils.categoryIcon[i]);
            //add modelCategory1 to categoryArrayList
            categoryArrayList.add(modelCategory1);
        }

        //inti/setup AdapterCategory
        AdapterCategory adapterCategory=new AdapterCategory(mContext, categoryArrayList, new RvListenerCategory() {
            @Override
            public void onCategoryClick(ModelCategory modelCategory) {
                loadAds(modelCategory.getCategory());
            }
        });
        //set adapter to the RecyclerView i.e categoriesRv
       // binding.categoriesRv.setAdapter(adapterCategory);
    }

    private void loadAds(String category){
        Log.d(TAG, "loadAds: CategoryFragment "+category);

        //init adsArrayList before starting adding data into it
        adsArrayList =new ArrayList<>();
        //Firebase DB listener to load Ads base on CategoryFragment and Distance
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Ads");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //clear adsArrayList each time starting adding data into it
                adsArrayList.clear();
                //load Ads list
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    //Prepare ModelAds with all data from Firebase DB
                    ModelAds modelAds=dataSnapshot.getValue(ModelAds.class);
                    //Function call with return value as distances in kilometer
                    double distance = calculateDistanceKm(modelAds.getLatitude(),modelAds.getLongitude());
                    Log.d(TAG, "onDataChange: Distance "+distance);

                    //filter
                    if (category.equals("All")){

                        //CategoryFragment all is selected, now check distance if is<= required e.g. 10km then show
                        if (distance <= MAX_DISTANCE_TO_LOAD_ADS_KM){
                            //The distance is <= required e.g. 10km. Add to list
                            adsArrayList.add(modelAds);
                        }
                    }else {
                              //some category is selected. e.g. Furniture
                        if (modelAds.getCategory().equals(category)){
                            //the distance is <= required e.g. 10km. Add to list
                            if (distance <= MAX_DISTANCE_TO_LOAD_ADS_KM){
                                adsArrayList.add(modelAds);
                            }
                        }
                    }

                }

                adapterAds=new AdapterAds (mContext,adsArrayList);
                binding.adsRv.setAdapter(adapterAds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private double calculateDistanceKm(double adslatitude, double adslongitude) {
        Log.d(TAG, "calculateDistanceKm: CurrentLatitude"+currentLatitude);
        Log.d(TAG, "calculateDistanceKm: CurrentLongitude"+currentLongitude);
        Log.d(TAG, "calculateDistanceKm: Adslatitude "+adslatitude);
        Log.d(TAG, "calculateDistanceKm: Adslongitude "+adslongitude);

        //source location i.e. user's current location
        Location startPoint = new Location(LocationManager.NETWORK_PROVIDER);
        startPoint.setLatitude(currentLatitude);
        startPoint.setLongitude(currentLongitude);

        //Destination location i.e Ads location
        Location endPoint=new Location(LocationManager.NETWORK_PROVIDER);
        endPoint.setLatitude(adslatitude);
        endPoint.setLongitude(adslongitude);

        //calculate distance
        double distanceInMeters=startPoint.distanceTo(endPoint);
        double distanceInKm=distanceInMeters/1000000000;

        return distanceInKm;
    }
}