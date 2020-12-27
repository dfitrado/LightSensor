package com.example.sensor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    Button b1;
    ImageView iv;
    TextView tv;
    private static final int kodekamera = 222;
    private static final int MY_PERMISSIONS_REQUEST_WRITE = 223;
    private SensorManager mSensorManager;
    private Sensor mSensorLight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askWritePermission();
        setContentView(R.layout.activity_main);

        b1 = (Button)findViewById(R.id.btnAmbil);
        iv = (ImageView)findViewById(R.id.imageView);
        tv = (TextView) findViewById(R.id.textSensor);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(it, kodekamera);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case (kodekamera):
                    try {
                        prosesSensor();
                        prosesKamera(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }
    private void prosesKamera(Intent datanya) throws IOException {
        Bitmap bm;
        bm = (Bitmap)datanya.getExtras().get("data");
        iv.setImageBitmap(bm);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
        uploadImageUsingRetrofit(bm);
        Toast.makeText(this, "Data telah Terload ke ImageView ", Toast.LENGTH_SHORT).show();
    }
    private void prosesSensor() {
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        String sensor_error = getResources().getString(R.string.error_no_sensor);
        if (mSensorLight == null) {
            tv.setText(sensor_error);
        }

        Toast.makeText(this, "Data sensor telah Terload ", Toast.LENGTH_SHORT).show();
    }
    private void askWritePermission(){
        if(Build.VERSION.SDK_INT >= 23){
            int cameraPermission = this.checkSelfPermission(Manifest.permission.CAMERA);
            if(cameraPermission != PackageManager.PERMISSION_GRANTED){
                this.requestPermissions(new String[]{
                        Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_WRITE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mSensorLight != null) {
            mSensorManager.registerListener(this, mSensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float currentValue = event.values[0];
        switch (sensorType) {
            // Event came from the light sensor.
            case Sensor.TYPE_LIGHT:
                tv.setText(String.valueOf(currentValue));
                // tv.setText("" + event.values[0]);
                break;
            default:
                // do nothing
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }

    public interface MyImageInterface {
        String IMAGEURL = "http://192.168.0.104/tugasSensor/";
        @FormUrlEncoded
        @POST("uploaddata.php")
        Call<String> getImageData(
                @Field("name") String name,
                @Field("image") String image
        );
        Call<String> getStringScalar(
                @Field("sensor") String sensor
        );
    }

    private void uploadImageUsingRetrofit(Bitmap bitmap){

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        String image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        String name = String.valueOf(Calendar.getInstance().getTimeInMillis());
        String sensor = String.valueOf(R.id.textSensor);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MyImageInterface.IMAGEURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        MyImageInterface myImageInterface = retrofit.create(MyImageInterface.class);

        Call<String> call2 = myImageInterface.getImageData(name,image);
        call2.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Toast.makeText(MainActivity.this, "Image Uploaded Successfully!!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Response not successful "+response.toString(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error occurred!", Toast.LENGTH_SHORT).show();
            }
        });
        Call<String> call = myImageInterface.getStringScalar(sensor);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Toast.makeText(MainActivity.this, "Image Uploaded Successfully!!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Response not successful "+response.toString(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error occurred!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}