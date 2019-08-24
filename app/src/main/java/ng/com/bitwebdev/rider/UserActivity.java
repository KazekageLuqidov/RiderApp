package ng.com.bitwebdev.rider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.com.bitwebdev.rider.historyRecyclerView.HistoryActivity;

public class UserActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private ImageView imgLogout,  mDriverProfileImage, mSettings, mHistory;
    private Button mOrder;
    private LatLng pickupLocation;
    private String destination, requestService;

    private LinearLayout mDriverInfo;

    private LatLng destinationLatLng;

    private RatingBar mRatingBar;

    private TextView mDriverName, mDriverPhone, mDriverCar;

     private SupportMapFragment mapFragment;

    private RadioGroup mRadioGroup;

    private boolean requestBol = false;
    private Marker pickUpMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UserActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }

        destinationLatLng = new LatLng(0.0,0.0);

        mDriverInfo = (LinearLayout)(findViewById(R.id.DriverInfo));

        mDriverName = (TextView)(findViewById(R.id.driverName));
        mDriverPhone = (TextView)(findViewById(R.id.driverPhone));
        mDriverCar = (TextView)(findViewById(R.id.driverCar));


        imgLogout = (ImageView)(findViewById(R.id.logout));
        mDriverProfileImage = (ImageView)(findViewById(R.id.driverProfileImage));
        mSettings = (ImageView)(findViewById(R.id.settings));
        mHistory = (ImageView)(findViewById(R.id.history));

        mRatingBar = (RatingBar)(findViewById(R.id.ratingBar));

        mRadioGroup = (RadioGroup)(findViewById(R.id.radioGroup));
        mRadioGroup.check(R.id.Atom);


        mOrder = (Button)(findViewById(R.id.orderBtn));

        imgLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(UserActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserActivity.this, HistoryActivity.class);
                intent.putExtra("CustomerorDriver", "Customers");
                startActivity(intent);
                return;
            }
        });


        mOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(requestBol){
                    endRide();
                }

                else{

                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectId);

                    if(radioButton.getText() == null){
                        return;
                    }
                    requestService = radioButton.getText().toString();

                    requestBol=true;
                    String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("customerRequest");

                    GeoFire geoFire = new GeoFire(myRef);
                    geoFire.setLocation(user_Id, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pick Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pic_marker)));

                    mOrder.setText("Just A Sec...");
                    getCloseRider();

                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserActivity.this, CustomerSettings.class);
                startActivity(intent);
                return;
            }
        });


        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.

            }
        });
    }

        private double radius = 1.0;
        private boolean riderFound = false;
        private String riderFoundId;
        GeoQuery geoQuery;



    private void getCloseRider(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("riderAvailable");
        GeoFire geofire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geofire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();



        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!riderFound && requestBol) {
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                                Map<String, Object> Drivermap = (Map<String,Object>) dataSnapshot.getValue();
                                if (riderFound){
                                    return;
                                }
                                if(Drivermap.get("service").equals(requestService)){
                                    riderFound = true;
                                    riderFoundId = dataSnapshot.getKey();

                                    DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderFoundId).child("customerRequest");
                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("userRideId",userId );
                                    map.put("destination",destination );
                                    map.put("destinationLat",destinationLatLng.latitude );
                                    map.put("destination",destinationLatLng.longitude );

                                    riderRef.updateChildren(map);
                                    getDriverLocation();
                                    getDriverInfo();
                                    getRideEnded();

                                    mOrder.setText("Searching Driver Locarions");


                                }

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!riderFound){
                    radius++;
                    getCloseRider();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private Marker mDriverMarker;
    private  DatabaseReference riderLocationRef;
    private ValueEventListener driverLocationListener;

    private void getDriverLocation(){
        riderLocationRef = FirebaseDatabase.getInstance().getReference().child("driverWorking").child(riderFoundId).child("l");
        driverLocationListener = riderLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&& requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();

                    double LocationLat = 0;
                    double LocationLng = 0;
                    mOrder.setText("Driver Found");
                    if(map.get(0)!= null){
                        LocationLat = Double.parseDouble(map.get(0).toString());
                    } if(map.get(1)!= null){
                        LocationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(LocationLat,LocationLng);
                    if(mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    Location Loc1 = new Location("");
                    Loc1.setLatitude(pickupLocation.latitude);
                    Loc1.setLongitude(pickupLocation.longitude);

                    Location Loc2 = new Location("");
                    Loc2.setLatitude(driverLatLng.latitude);
                    Loc2.setLongitude(driverLatLng.longitude);

                    float distance = Loc1.distanceTo(Loc2);
                    if (distance<100) {
                        mOrder.setText("Driver Arrived");
                    }else{
                        mOrder.setText("Driver Found" + String.valueOf(distance));
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.dri_marker)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mSettingsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderFoundId);
        mSettingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){

                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!= null){

                        mDriverPhone.setText( map.get("phone").toString());;
                    }
                    if(map.get("car")!= null){

                        mDriverCar.setText( map.get("car").toString());;
                    }

                    if(map.get("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }

                    int ratingSum = 0;
                    float ratingTotal = 0;
                    float ratingAvr = 0;

                    for(DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingTotal++;
                    }
                    if(ratingTotal != 0){
                        ratingAvr = ratingSum/ratingTotal;
                        mRatingBar.setRating(ratingAvr);
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference RideEndRef;
    private ValueEventListener RideEndListener;


    private void getRideEnded (){
        RideEndRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderFoundId).child("customerRequest").child("userRideId");
        RideEndListener= RideEndRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){


                } else{
                    endRide();


                }
            }



            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void endRide(){
        requestBol=false;
        geoQuery.removeAllListeners();
        RideEndRef.removeEventListener(RideEndListener);

        riderLocationRef.removeEventListener(driverLocationListener);

        if(riderFoundId != null){
            DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderFoundId).child("customerRequest");
            riderRef.removeValue();
            riderFoundId=null;

        }
        riderFound = false;
        radius = 1;


        String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("customerRequest");

        GeoFire geoFire = new GeoFire(myRef);
        geoFire.removeLocation(user_Id);

        if(pickUpMarker != null){
            pickUpMarker.remove();
        }
        mOrder.setText("Call Uber");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.profile_image);

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UserActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();


    }


    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(9));

    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UserActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    final int LOCATION_REQUEST_CODE = 1;
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE: {
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }else {
                    Toast.makeText(getApplicationContext(),"Allow Permissions", Toast.LENGTH_LONG).show();
                }
                break;
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();


    }

}
