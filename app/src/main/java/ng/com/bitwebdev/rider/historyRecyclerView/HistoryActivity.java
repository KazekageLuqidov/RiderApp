package ng.com.bitwebdev.rider.historyRecyclerView;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import ng.com.bitwebdev.rider.R;

public class HistoryActivity extends AppCompatActivity {


    private RecyclerView mRecyclerHistory;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;

    private TextView mBalance;
    private double Balance = 0.0;

    private String CustomerorDriver, userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);


        mRecyclerHistory = (RecyclerView) (findViewById(R.id.historyRecycler));
        mRecyclerHistory.setNestedScrollingEnabled(false);
        mRecyclerHistory.setHasFixedSize(true);

        mBalance = findViewById(R.id.balance);

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mRecyclerHistory.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(DataContent(), HistoryActivity.this);
        mRecyclerHistory.setAdapter(mHistoryAdapter);


        CustomerorDriver = getIntent().getExtras().getString("CustomerorDriver");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryId();

        if (CustomerorDriver.equals("Drivers")){
            mBalance.setVisibility(View.VISIBLE);
        }


    }


    private void getUserHistoryId() {
        DatabaseReference historyDB = FirebaseDatabase.getInstance().getReference().child("Users").child(CustomerorDriver).child(userId).child("history");
        historyDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history: dataSnapshot.getChildren()){
                        fetchRideInformation(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void fetchRideInformation(String Infokey) {
        DatabaseReference InfoDB = FirebaseDatabase.getInstance().getReference().child("history").child(Infokey);
        InfoDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    String rideId = dataSnapshot.getKey();
                    Long timestamp = 0L;
                    String distance = "";
                    Double ridePrice = 0.0;


                        if(dataSnapshot.child("timestamp") != null){
                            timestamp = Long.valueOf(dataSnapshot.child("timestamp").getValue().toString());
                        }


                    if(dataSnapshot.child("customerPaid").getValue() != null && dataSnapshot.child("driverPaidOut").getValue() == null){
                            if(dataSnapshot.child("distance").getValue() != null){
                                distance = dataSnapshot.child("distance").getValue().toString();
                                ridePrice = (Double.valueOf(distance) * 0.4);
                                Balance += ridePrice;
                                mBalance.setText("Balance: " + String.valueOf(Balance)+ " $");
                            }

                    }


                    historyObject obj = new historyObject(rideId, getDate(timestamp));

                    resultHistory.add(obj);

                mHistoryAdapter.notifyDataSetChanged();

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



    private ArrayList resultHistory = new ArrayList<Object>();

    private ArrayList<historyObject> DataContent() {
        return resultHistory;
    }
}
