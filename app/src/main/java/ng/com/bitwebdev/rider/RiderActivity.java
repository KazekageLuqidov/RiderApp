package ng.com.bitwebdev.rider;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;

import ng.com.bitwebdev.rider.historyRecyclerView.HistoryActivity;

public class RiderActivity extends FragmentActivity implements RoutingListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private LinearLayout mCustomerInfo;
    private int status = 0;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;

    private Switch mAvailSwitch;
    private float rideDistance;

    private ImageView imgLogout, mSettings,  mHistory;
    private Button mRideStatus;
    private String customerId = "", destination;
    private LatLng destinationLatLng, pickupLatLng;

    private SupportMapFragment mapFragment;
    private boolean isLoggingOut = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        polylines = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RiderActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }


        imgLogout = (ImageView)(findViewById(R.id.logout));
        mSettings = (ImageView)(findViewById(R.id.settings));
        mHistory = (ImageView)(findViewById(R.id.history));

        mRideStatus = (Button)(findViewById(R.id.rideStatus));

        mAvailSwitch = (Switch)(findViewById(R.id.availSwitch));

        mCustomerInfo = (LinearLayout)(findViewById(R.id.customerInfo));
        mCustomerProfileImage = (ImageView)(findViewById(R.id.profileImage));
        mCustomerName = (TextView)(findViewById(R.id.customerName));
        mCustomerPhone = (TextView)(findViewById(R.id.customerPhone));
        mCustomerDestination = (TextView)(findViewById(R.id.customerDestination));


        mAvailSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    ConnectDriver();

                }else{
                    DisconnectDriver();
                }
            }
        });


        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status){
                    case 1:
                        status = 2;
                        eraseLines();
                        if(destinationLatLng.latitude != 0.0 && destinationLatLng.longitude!= 0.0){
                            getPickUpToMarker(destinationLatLng);
                        }
                        break;
                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
            }
        });

        imgLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLoggingOut = true;
                DisconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(RiderActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RiderActivity.this, HistoryActivity.class);
                intent.putExtra("CustomerorDriver", "Drivers");
                startActivity(intent);
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RiderActivity.this, DriverSettings.class);
                startActivity(intent);
                finish();
                return;

            }
        });

        getAssignedCustomer();
    }



    private void getAssignedCustomer (){
        String riderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderId).child("customerRequest").child("userRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    status = 1;
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickUp();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();


                    } else{

                   endRide();

                }
                }



            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    Marker pickUpMarker;
    private DatabaseReference assignedCustomerPickUpRef;
    private ValueEventListener assignedCustomerPickUpListener;

    private void getAssignedCustomerPickUp(){
         assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Users").child("customerRequest").child("l");
       assignedCustomerPickUpListener = assignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && customerId.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if(map.get(0)!= null){
                        LocationLat = Double.parseDouble(map.get(0).toString());
                    } if(map.get(1)!= null){
                        LocationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng pickupLatLng = new LatLng(LocationLat,LocationLng);

                   pickUpMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("PickUp Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pic_marker)));
                   getPickUpToMarker(pickupLatLng);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void getPickUpToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }

    private void getAssignedCustomerDestination (){
        String riderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderId).child("customerRequest").child("destination");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("destination") != null) {
                         destination = map.get("destination").toString();
                        mCustomerDestination.setText("destination" + destination);

                    } else {
                        mCustomerDestination.setText("Destination: -- ");
                    }
                    double destinationLat = 0.0;
                    double destinationLng = 0.0;
                    if(map.get("destinationLat")!= null){
                        destinationLat = Double.valueOf( map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng")!= null){
                        destinationLng = Double.valueOf( map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }



            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
       DatabaseReference mSettingsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mSettingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){

                        mCustomerName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!= null){

                        mCustomerPhone.setText( map.get("phone").toString());;
                    }
                    if(map.get("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endRide(){
        mRideStatus.setText("Picked Customer");
        eraseLines();

        String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(user_Id).child("customerRequest");
        riderRef.removeValue();

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("customerRequest");

        GeoFire geoFire = new GeoFire(myRef);
        geoFire.removeLocation(customerId);
        customerId = "";
        rideDistance = 0;

        if(pickUpMarker != null){
            pickUpMarker.remove();
        }
        if(assignedCustomerPickUpListener != null) {
            assignedCustomerPickUpRef.removeEventListener(assignedCustomerPickUpListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: ");
        mCustomerProfileImage.setImageResource(R.mipmap.profile_image);


    }



    private void recordRide(){
        String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(user_Id).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        String requestId = historyRef.push().getKey();
        riderRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", user_Id);
        map.put("timestamp", getCurrentStamp());
        map.put("customer", customerId);
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        map.put("distance", rideDistance);
        map.put("rating", 0);
        historyRef.child(requestId).updateChildren(map);


    }

    private Long getCurrentStamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RiderActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
        if(getApplicationContext() != null){
            if(!customerId.equals("")){
                rideDistance += mLastLocation.distanceTo(location)/1000;

            }

            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(9));
            String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference riderAvailable = FirebaseDatabase.getInstance().getReference("riderAvailable");
            DatabaseReference driverWorking = FirebaseDatabase.getInstance().getReference("driverWorking");
            GeoFire geoFireAvailable = new GeoFire(riderAvailable);
            GeoFire geoFireWorking = new GeoFire(driverWorking);

            switch (customerId){
                case "":

                    geoFireWorking.removeLocation(user_Id);
                    geoFireAvailable.setLocation(user_Id, new GeoLocation(location.getLatitude(),location.getLongitude()));
                break;
        default:
            geoFireAvailable.removeLocation(user_Id);
            geoFireWorking.setLocation(user_Id, new GeoLocation(location.getLatitude(),location.getLongitude()));
        break;
    }




    }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void ConnectDriver(){

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RiderActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void DisconnectDriver(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("riderAvailable");

        GeoFire geoFire = new GeoFire(myRef);
        geoFire.removeLocation(user_Id);

    }

    final int LOCATION_REQUEST_CODE = 1;
    @Override

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

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark};



    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onRoutingCancelled() {

    }

    private void eraseLines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
