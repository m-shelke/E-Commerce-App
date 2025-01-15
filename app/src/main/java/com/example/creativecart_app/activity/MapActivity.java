package com.example.creativecart_app.activity;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.creativecart_app.LoginOptionActivity.Utils;
import com.example.creativecart_app.R;
import com.example.creativecart_app.databinding.ActivityMainBinding;
import com.example.creativecart_app.databinding.ActivityMapBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    //View Binding
    private ActivityMapBinding binding;

    //TAG for logs in LogCat
    private static final String TAG="LOCATION_PICKER_TAG";

    private static final int DEFAULT_ZOOM=15;
    private GoogleMap mMap=null;

    //Current place picker
    private PlacesClient mPlacesClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //The geographical location where the device is currently located. That is, the last-known location retrieved by the Fused Location Provider
    private Location mLastKnownLocation =null;
    private Double selectedLatitude=null;
    private Double selectedLongitude=null;
    private String selectedAddress="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity_map.xml=ActivityLocationPickerBinding
        binding=ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Hide the doneLl for now. We will show, when user select or search location
        binding.doneLl.setVisibility(View.GONE);

        //Obtain the SupportFragment and get notified, when the is ready to used
        SupportMapFragment mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
         mapFragment.getMapAsync(this);

        Spinner spinner = findViewById(R.id.spinnerMapType);
        ArrayList<String> arrayListSpinner = new ArrayList<>();

        arrayListSpinner.add("Normal");
        arrayListSpinner.add("Hybrid");
        arrayListSpinner.add("Satelite");
        arrayListSpinner.add("Terrain");
        arrayListSpinner.add("Nome");

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0){
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }else if (position == 1){
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else if (position == 2) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else if (position == 3) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getApplicationContext(),"Default Mode Of Map",Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,arrayListSpinner);
        spinner.setAdapter(adapter);

         //initialized the Place Client
        Places.initialize(this,getString(R.string.google_map_api_key));

        //Create a new PlacesClient instance
        mPlacesClient= Places.createClient(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //initialized the AutocompleteSupportFragment to search place on map
        AutocompleteSupportFragment autocompleteSupportFragment=(AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        //List of location fields, we need in search result. e.g Place.Field.ID,Place.Field.NAME,Place.Field.ADDRESS,Place.Field.LAT_LNG
        Place.Field[] placeList = new Place.Field[]{Place.Field.ID,Place.Field.NAME,Place.Field.ADDRESS,Place.Field.LAT_LNG};

        //set the location field to the autocompleteSupportFragment
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(placeList));

        //Listen for places selection
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                //Exception occurred while searching or selecting location
                Log.d(TAG, "onError: Status "+status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {

                //Place selected. The param "places" contains all the fields that we set as list. Now return the request location field to the requesting Activity/fragment
                String id=place.getId();
                String title= place.getName();
                LatLng latLng=place.getLatLng();

                assert latLng != null;

                selectedLatitude=latLng.latitude;
                selectedLongitude=latLng.latitude;
                selectedAddress= place.getAddress();

                Log.d(TAG, "onPlaceSelected: ID "+id);
                Log.d(TAG, "onPlaceSelected: Title "+title);
                Log.d(TAG, "onPlaceSelected: Latitude "+selectedLatitude);
                Log.d(TAG, "onPlaceSelected: Longitude "+selectedLongitude);
                Log.d(TAG, "onPlaceSelected: Address "+selectedAddress);

                addMarker(latLng,title,selectedAddress);
            }
        });

        //Handle toolbarBackBtn click, to go back
     /*   binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             onBackPressed();
            }

        });*/



    //Handle toolbarGpsBtn click, if GPS enabled get and show user current location
//        binding.toolbarGPSBtn.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//
//                //check if location enabled
//                if (isGPSEnabled()){
//                    requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
//                }else {
//                    //GPS/Location not enabled
//                    Utils.toast(MapActivity.this,"Location is not on..! Turn it on to show current location ");
//                }
//            }
//        });

        //Handle doneLl click, get the selected location back to requesting activity/fragment class
        binding.doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //put data to intent to get in previous activity
                Intent intent=new Intent();
                intent.putExtra("latitude",selectedLatitude);
                intent.putExtra("longitude",selectedLongitude);
                intent.putExtra("address",selectedAddress);
                setResult(RESULT_OK,intent);
                //finish this activity
                finish();
            }
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: ");

        mMap=googleMap;

        //prompt the user for permission
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        //Handle mMap click, get latitude,longitude when of where user click on map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {

                //get latitude and longitude from the param latlng
                selectedLatitude=latLng.latitude;
                selectedLongitude=latLng.longitude;

                Log.d(TAG, "onMapClick: selectedLatitude "+selectedLatitude);
                Log.d(TAG, "onMapClick: selectedLongitude "+selectedLongitude);

                //function call to get the address details from the LatLng
                addressFromLatLng(latLng);
            }
        });

    }

    @SuppressLint("MissingPermission")
    private ActivityResultLauncher<String> requestLocationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {

                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: ");

                    //Let's check if from permission dialog user have granted the permission or denies the result is in isGranted as true/false
                    if (isGranted){
                        //Enable google map GPS button, to set current location on map
                        mMap.setMyLocationEnabled(true);
                        pickCurrentPlace();
                    }else {
                        //User denied permission, so we can't pick location
                        Utils.toast(MapActivity.this,"Permission Denied...");
                    }
                }
            }
    );


    private void addressFromLatLng(LatLng latLng){
        Log.d(TAG, "addressFromLatlng: ");

        //inite Geocoder class to get the address from LatLng
        Geocoder geocoder=new Geocoder(this);

        try {
            //get maximum 1 result (Address) from the list of available address list of addresses on the bases of Latitude and Longitude we passes
            List<Address> addressesList=geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);

            //get Address object from the list address-list of type List<Address>
            Address address=addressesList.get(0);

            //Get the address details
            String addressLine=address.getAddressLine(0);
            String countryName=address.getCountryName();
            String adminArea=address.getAdminArea();
            String subAdminArea=address.getSubAdminArea();
            String locality=address.getLocality();
            String subLocality=address.getSubLocality();
            String postalCode=address.getPostalCode();

            //save address in selectedAddress variable
            selectedAddress=""+addressLine;

            //add marker on map
            addMarker(latLng,""+subLocality,""+addressLine);

        }catch (Exception e){
            Log.e(TAG, "addressFromLatlng: ",e);
        }
    }

    /* This function will be called only, if the location permission granted.
    * We will only check if map object is not null then proceed to show the location on map */
    private void pickCurrentPlace(){
        Log.d(TAG, "pickCurrentPlace: ");
        if (mMap==null){
            return;
        }

        detectAndShowDeviceLocationMap();
    }

    //Get the current location of the device, and position the map camera
    @SuppressLint("MissingPermission")
    private void detectAndShowDeviceLocationMap(){


        //Get best and most recent location of the device, which may null in the rare cases, when location is not available
        try {
            Task<Location> locationResult =mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if (location!=null){

                        //location got, save that location in mLastKnownLocation
                        mLastKnownLocation=location;
                        //get the Latitude and Longitude
                        selectedLatitude=location.getLatitude();
                        selectedLongitude=location.getLongitude();

                        Log.d(TAG, "onSuccess: selectedLatitude "+selectedLatitude);
                        Log.d(TAG, "onSuccess: selectedLongitude "+selectedLongitude);

                        //setup latLng from selectedLatitude and selectedLongitude
                        LatLng latLng=new LatLng(selectedLatitude,selectedLongitude);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_ZOOM));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

                        //function call to retrieve address from the LatLng
                        addressFromLatLng(latLng);
                    }else {
                        Log.d(TAG, "onSuccess: Location is null ");
                    }

                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Exception occur while getting location
                            Log.e(TAG, "onFailure: ",e);
                        }
                    });

        }catch (Exception e){
            Log.e(TAG, "detectAndShowDeviceLocationMap: ",e);
        }

    }

    //Check if GPS/Location is enabled or not
   private boolean isGPSEnabled(){
        //init LocationManager
       LocationManager locationManager=(LocationManager) getSystemService(LOCATION_SERVICE);

       //boolean variable to value as true or false
       boolean gpsEnabled=false;
       boolean networkEnabled=false;

       //check if GPS_PROVIDER enabled
       try {
           gpsEnabled=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
       }catch (Exception e){
           Log.e(TAG, "isGPSEnabled: ",e);
       }

       //check if NETWORK_PROVIDER enabled
       try {
           networkEnabled=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
       }catch (Exception e){
           Log.e(TAG, "isGPSEnabled: ",e);
       }
       //reture results
       return !(!gpsEnabled && !networkEnabled);
   }




    /* Add marker on Map after Searching/Picking location
    *
    * @param lating: Lating of the location picked
    * @param title: Title of the location picked
    * @param address: Address of the location picked*/
    private void addMarker(LatLng latLng, String title, String address) {
        Log.d(TAG, "addMarker: Latitude "+latLng.latitude);
        Log.d(TAG, "addMarker: Longitude "+latLng.longitude);
        Log.d(TAG, "addMarker: Title "+title);
        Log.d(TAG, "addMarker: Address "+address);

        //Clear map before adding new marker. As we need one Location marker on the map, so if there is already one clear it before adding new
        mMap.clear();

        try {
            //setup markerOptions with LagLng,title,complete address
            MarkerOptions markerOptions=new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(""+title);
            markerOptions.snippet(""+address);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

            //add marker to the map and move camera to the newly added marker
            mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_ZOOM));

            //Show the doneLl, so user can go back (with selected location) to the activity/fragment class that is requesting location
            binding.doneLl.setVisibility(View.VISIBLE);
            //set selected location complete address
            binding.selectedPlaceTv.setText(address);

        }catch (Exception e){
            Log.e(TAG, "addMarker: ",e);
        }
    }
}