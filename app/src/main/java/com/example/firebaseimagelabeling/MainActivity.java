package com.example.firebaseimagelabeling;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.firebaseimagelabeling.Helper.InternetCheck;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button btnDetect;
    AlertDialog waitingDialog;

    @Override
    protected void onResume(){
        super.onResume();
        cameraView.start();
    }

// After capturing image blackout error.
/*@Override
    protected void onPause(){
        super.onPause();
        cameraView.stop();
    }
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (CameraView) findViewById(R.id.camera_view);
        btnDetect = (Button) findViewById(R.id.btn_detect);

        waitingDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Gae San joong")
                .setCancelable(false).build();

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                waitingDialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);
                //cameraView.stop(); -- black out error

                // Pass on Image for Firebase MLKit
                runDetector(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        btnDetect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                cameraView.start();
                cameraView.captureImage();
            }
        });

    }

    private void runDetector(Bitmap bitmap) {
        Log.d("EDMTERROR","here2");

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        new InternetCheck(new InternetCheck.Consumer() {
            @Override
            public void accept(boolean internet) {
                // if (internet)  <- original ,, always use on device 400 labels.
                if(false){
                    // if there is internet,,,, use CLOUD
                    FirebaseVisionCloudDetectorOptions options =
                            new FirebaseVisionCloudDetectorOptions.Builder().setMaxResults(1) // Get 1 result with highest confidence Threshold
                        .build();
                    FirebaseVisionCloudLabelDetector detector =
                            FirebaseVision.getInstance().getVisionCloudLabelDetector(options);

                    detector.detectInImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionCloudLabel> firebaseVisionCloudLabels) {
                                    processDataResultCloud(firebaseVisionCloudLabels);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("EDMTERROR",e.getMessage());
                                }
                            });


                }else{
                    FirebaseVisionLabelDetectorOptions options =
                            new FirebaseVisionLabelDetectorOptions.Builder()
                                    .setConfidenceThreshold(0.3f) // confidence 0.8 original
                                    .build();

                    FirebaseVisionLabelDetector detector =
                            FirebaseVision.getInstance().getVisionLabelDetector(options);

                    detector.detectInImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionLabel> firebaseVisionLabels) {
                                    processDataResult(firebaseVisionLabels);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("EDMTERROR",e.getMessage());
                                }
                            });
                }

            }
        });

    }

    private void processDataResultCloud(List<FirebaseVisionCloudLabel> firebaseVisionCloudLabels) {
        for(FirebaseVisionCloudLabel label : firebaseVisionCloudLabels){
            Toast.makeText(this, "Cloud result :" + label.getLabel(), Toast.LENGTH_SHORT).show();
        }
        if(waitingDialog.isShowing())
            waitingDialog.dismiss();
    }

    private void processDataResult(List<FirebaseVisionLabel> firebaseVisionCloudLabels) {
        for(FirebaseVisionLabel label : firebaseVisionCloudLabels) {
            Toast.makeText(this, "On-device result :" + label.getLabel(), Toast.LENGTH_SHORT).show();
            Log.d("Labeling tags", label.getLabel() + " " + label.getConfidence());
        }
        if(waitingDialog.isShowing())
            waitingDialog.dismiss();
        Log.d("Labeling tags", " ");
    }
}