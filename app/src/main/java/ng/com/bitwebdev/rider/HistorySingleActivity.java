package ng.com.bitwebdev.rider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {


    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private TextView mRideDistance, mRideDate, mRideLocation;
    private TextView mUserPhone, mUserName;
    private ImageView mUserImage;

    private RatingBar mRatingBar;
    private Button mPay;


    private LatLng pickUpLatLng, destinationLatLng;

    private boolean customerPaid = false;

    private String distance;
    private double rideCost;

    private String rideId, currentUserId, customerId, userDriverCustomer, driverId;
    private DatabaseReference historyRideDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);


        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);


        polylines = new ArrayList<>();

        mMapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        mMapFragment.getMapAsync(this);

        mRideDate = (TextView)(findViewById(R.id.rideDate));
        mRideDistance = (TextView)(findViewById(R.id.rideDistance));
        mRideLocation = (TextView)(findViewById(R.id.rideLocation));
        mUserPhone = (TextView)(findViewById(R.id.userPhone));
        mUserName = (TextView)(findViewById(R.id.userName));
        mPay = (findViewById(R.id.pay));


        mRatingBar = (RatingBar)(findViewById(R.id.ratingBar));

        mUserImage = (ImageView)(findViewById(R.id.userImage));

        rideId = getIntent().getExtras().getString("rideId");
        historyRideDB = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideOrientation();



    }

    private void getRideOrientation() {
        historyRideDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverCustomer = "Drivers";
                                getUserInformation("Customers", customerId);
                            }
                        }
                    }
                }
                if(dataSnapshot.exists()){
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)){
                                userDriverCustomer = "Customers";

                                getUserInformation("Riders", driverId);
                                getCustomerRelatedObject();
                            }
                        }
                        if(child.getKey().equals("timestamp")){
                            mRideDate.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }

                        if(child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }

                        if(child.getKey().equals("customerPaid")){
                            customerPaid = true;
                        }

                        if(child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            mRideDistance.setText(distance.substring(0, Math.min(distance.length(), 5)) + "km");
                            rideCost = Double.valueOf(distance)* 0.5;
                        }


                        if(child.getKey().equals("destination")){
                            mRideLocation.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if(child.getKey().equals("location")){

                            pickUpLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(destinationLatLng != new LatLng(0,0)){
                                getPickUpToMarker();
                            }


                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getCustomerRelatedObject() {
        mRatingBar.setVisibility(View.VISIBLE);
        mPay.setVisibility(View.VISIBLE);



        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideDB.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDB = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverId).child("rating");
                mDriverRatingDB.child(rideId).setValue(rating);
            }
        });

        if(customerPaid){
            mPay.setEnabled(false);
        }else{
            mPay.setEnabled(true);
        }

        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Payment();
            }
        });
    }

    private int PAYPAL_REQUEST_CODE = 1;
    private static PayPalConfiguration config = new PayPalConfiguration().environment(PayPalConfiguration.ENVIRONMENT_SANDBOX).clientId(PayPalConfig.PAYPAL_CLIENT_ID);
    


    private void Payment() {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(rideCost),"USD", "Rider Payment", PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
        startActivityForResult(intent, PAYPAL_REQUEST_CODE);

    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PAYPAL_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){

                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirmation != null){
                    try {
                        JSONObject jsonObject = new JSONObject(confirmation.toJSONObject().toString());
                        String paymentResponse = jsonObject.getJSONObject("response").getString("state");
                        if(paymentResponse.equals("approved")){
                            Toast.makeText(getApplicationContext(), "Payment Successful", Toast.LENGTH_LONG).show();
                            historyRideDB.child("customerPaid").setValue(true);
                            mPay.setEnabled(true);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }else {
                Toast.makeText(getApplicationContext(), "Transaction Unsuccessful", Toast.LENGTH_LONG).show();
            }
        }
    }







    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    private void getUserInformation(String otherDriverCustomer, String otherUserId) {
        DatabaseReference otherHistoryDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherDriverCustomer).child(otherUserId);
        otherHistoryDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){
                        mUserName.setText(map.get("name").toString());

                    }
                    if(map.get("phone")!= null){
                        mUserPhone.setText(map.get("phone").toString());

                    }
                    if(map.get("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mUserImage);

                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private String getDate(Long timestamp) {

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd:MM:yyyy  hh:mm", cal).toString();

        return  date;


    }
    private void getPickUpToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickUpLatLng, destinationLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickUpLatLng);
        builder.include(destinationLatLng);

        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickUpLatLng).title("Pick UP").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pic_marker)));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));


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
