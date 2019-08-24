package ng.com.bitwebdev.rider;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private ImageView mCustomer, mRider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCustomer = (ImageView) findViewById(R.id.userId);
        mRider = (ImageView) findViewById(R.id.riderId);

        startService(new Intent(MainActivity.this, onDriverStop.class));

        mRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(MainActivity.this, DriverLogin.class);
                startActivity(in);
                finish();
                return;
            }
        });
          mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(MainActivity.this, UserLogin.class);
                startActivity(in);
                finish();
                return;
            }
        });


    }
}
