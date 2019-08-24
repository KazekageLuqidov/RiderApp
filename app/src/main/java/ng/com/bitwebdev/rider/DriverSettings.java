package ng.com.bitwebdev.rider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettings extends AppCompatActivity {


    private EditText mNameField, mNumberField, mCarField;
    private Button mBackbtn, mConfirmbtn;

    private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mSettingsRef;

    private String userId;
    private String mName;
    private String mCar;
    private String mService;
    private String mprofileImageUrl;
    private String mPhone;
    private Uri resultUri;

    private RadioGroup mRadioGroup;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        mNameField = (EditText)(findViewById(R.id.name));
        mCarField = (EditText)(findViewById(R.id.car));
        mNumberField = (EditText)(findViewById(R.id.Phone));

        mRadioGroup = (RadioGroup)(findViewById(R.id.radioGroup));

        mBackbtn = (Button)(findViewById(R.id.Backbtn));
        mProfileImage = (ImageView) (findViewById(R.id.profileImage));
        mConfirmbtn = (Button)(findViewById(R.id.Confirmbtn));

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        mSettingsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(userId);
        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        mConfirmbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInfo();
            }
        });

        mBackbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

    }


    private void getUserInfo(){
        mSettingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone")!= null){
                        mPhone = map.get("phone").toString();
                        mNumberField.setText(mPhone);
                    }
                    if(map.get("car")!= null){
                        mCar = map.get("car").toString();
                        mCarField.setText(mCar);
                    }
                    if(map.get("service")!= null){
                        mService = map.get("service").toString();
                        switch (mService){
                            case "Atom":
                                mRadioGroup.check(R.id.Atom);
                                break;
                            case "Optimus":
                                mRadioGroup.check(R.id.Optimus);
                                break;
                        }
                    }
                    if(map.get("profileImageUrl")!= null){
                        mprofileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mprofileImageUrl).into(mProfileImage);
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInfo (){

        mName = mNameField.getText().toString();
        mPhone = mNumberField.getText().toString();
        mCar = mCarField.getText().toString();

        int selectId = mRadioGroup.getCheckedRadioButtonId();
        final RadioButton radioButton = (RadioButton) findViewById(selectId);

        if(radioButton.getText() == null){
            return;
        }
        mService = radioButton.getText().toString();



        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("car", mCar);
        userInfo.put("service", mService);
        mSettingsRef.updateChildren(userInfo);

        if(resultUri != null){
            StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte [] data = baos.toByteArray();
            UploadTask uploadTask = filepath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;

                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl", downloadUrl.toString());
                    mSettingsRef.updateChildren(newImage);
                    finish();
                    return;


                }
            });
        }else {

            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(imageUri);
        }
    }
}

