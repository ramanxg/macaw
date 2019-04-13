package com.example.macaw;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.Http;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PIC_REQUEST = 1337;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap currentImage;
    private TextView tv;
    private String languageResult = "";
    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        frameLayout = (FrameLayout)findViewById(R.id.frameLayout);
        imageView = (ImageView)findViewById(R.id.imageView);
        camera = Camera.open();
        showCamera = new ShowCamera(this, camera, imageView);
        frameLayout.addView(showCamera);

        Button testbutton = (Button) findViewById(R.id.buttonTest);
        testbutton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                labelImage();
        }

        });
    }

    public void labelImage()
    {
        //ImageView imageview = (ImageView) findViewById(R.id.imageView);
        Bitmap b = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

        FirebaseVisionImage image = imageFromBitmap(b);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer();
        tv = findViewById(R.id.textView);

        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                tv.setText("Success!");
                                processTextBlock(firebaseVisionText);

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        tv.setText("Unsuccessful");
                                    }
                                });



    }

    private void processTextBlock(FirebaseVisionText result) {
        // [START mlkit_process_text_block]
        String resultText = result.getText();
        for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionText.Line line: block.getLines()) {
                String lineText = line.getText();
                Float lineConfidence = line.getConfidence();
                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (FirebaseVisionText.Element element: line.getElements()) {
                    System.out.println(element.toString());
                    String elementText = element.getText();
                    Float elementConfidence = element.getConfidence();
                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                    String languageCode = getLanguage(elementText);
                    TextView t = findViewById(R.id.textView);
                    String translated = null;
                    try {
                        translated = translate(elementText, languageCode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    t.setText(t.getText() + elementText + " " + languageCode + " " + translated);
                    overlayText(elementCornerPoints, translated, elementFrame);
                }
            }
        }
        // [END mlkit_process_text_block]
    }

    private void overlayText(Point[] elementCornerPoints, String translated, Rect elementFrame)
    {
        //System.out.println(elementCornerPoints.toString());
        System.out.println(translated);
        System.out.println(elementFrame.toString());
    }


    public String getLanguage(String text)
    {
        //final String[] languageResult = {""};
        FirebaseLanguageIdentification languageIdentifier =
                FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String languageCode) {
                                languageResult = languageCode;
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be loaded or other internal error.
                                System.out.println("this failed");

                            }
                        });
        return languageResult;
    }

    private String translate(String text, String code) throws IOException {
        String stringUrl = "https://translation.googleapis.com/language/translate/v2";
        URL url = null;
        URLConnection uc = null;
        try {
            url = new URL(stringUrl);
            uc = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uc.setRequestProperty("Content-Type", "application/json");


        String api_key = "Bearer ya29.c.ElrrBj3E_pFl4F-DC3qwMNlLkc0eD0s2lDijFaR4lfo9qiG5Yvgk-YtE2u_VX-sSDL3QhpAXSwHP13V3UnaUDHulCrikUYdjj8K5_KQf2scwo68ZqmoLACxZC4U";

        uc.setRequestProperty("Authorization", api_key);
        uc.setDoInput(true);
        uc.setDoOutput(true);
        HttpURLConnection http = (HttpURLConnection) uc;
        http.setRequestMethod("POST");
        String out = "{'q': '" + text + "', 'source': '" + code + "', 'target': 'en', 'format': 'text'}";
        int length = out.length();

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try(OutputStream os = http.getOutputStream()) {
            os.write(out.getBytes("UTF-8"));
        }
        System.out.println(http.getResponseCode());
        StringBuilder content;

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(http.getInputStream()))) {

            String line;
            content = new StringBuilder();

            while ((line = in.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        }

        return content.toString();

    }

    private FirebaseVisionImage imageFromBitmap(Bitmap bitmap) {
        // [START image_from_bitmap]
        return FirebaseVisionImage.fromBitmap(bitmap);
        // [END image_from_bitmap]
    }
}
