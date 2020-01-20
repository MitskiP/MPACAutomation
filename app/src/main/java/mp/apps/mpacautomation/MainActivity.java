package mp.apps.mpacautomation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = ">>>";

    private TextView tvstate;
    private TextView tvbat;
    private DatabaseReference database;
    private InfraRed IR;

    private ValueEventListener cmdListener;
    private ValueEventListener batListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance().getReference("mpacautomation");

        /*
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        String msg = "token: " + token;
                        Log.d(">>>>>>>>>>", msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
        */
        /*
        FirebaseMessaging.getInstance().subscribeToTopic("comm")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "subscribed";
                        if (!task.isSuccessful()) {
                            msg = "subscribe failed";
                        }
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
        */

        IR = new InfraRed(this);
        Button off = (Button)findViewById(R.id.button_off);
        Button dry = (Button)findViewById(R.id.button_dry);
        off.setOnClickListener(this);
        dry.setOnClickListener(this);
        Button roff = (Button)findViewById(R.id.button_req_off);
        Button rdry = (Button)findViewById(R.id.button_req_dry);
        roff.setOnClickListener(this);
        rdry.setOnClickListener(this);
        Button rp1 = (Button)findViewById(R.id.button_req_ping1);
        Button rp2 = (Button)findViewById(R.id.button_req_ping2);
        rp1.setOnClickListener(this);
        rp2.setOnClickListener(this);

        tvstate = (TextView)findViewById(R.id.textview_req_state);
        tvbat = (TextView)findViewById(R.id.textView_bat);

        cmdListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.getValue(String.class);
                Log.d(TAG, "CMD changed to: " + s);
                tvstate.setText(s);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "db error...?");
                tvstate.setText("onCancelled");
            }
        };
        batListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.getValue(String.class);
                Log.d(TAG, "BAT changed to: " + s);
                tvbat.setText(s);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "db error...?");
                tvstate.setText("onCancelled");
            }
        };

        database.child("BAT").setValue("query");

        if (IR.isAvailable()) {
            startService(new Intent(this, MyService.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        database.child("CMD").addValueEventListener(cmdListener);
        database.child("BAT").addValueEventListener(batListener);
    }
    @Override
    public void onPause() {
        super.onPause();
        database.child("CMD").removeEventListener(cmdListener);
        database.child("BAT").removeEventListener(batListener);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.button_off) {
            Log.d(">>>", "off clicked");
            IR.call("OFF");
        } else if(view.getId() == R.id.button_dry) {
            Log.d(">>>", "dry clicked");
            IR.call("DRY");
        } else if(view.getId() == R.id.button_req_off) {
            Log.d(">>>", "req off clicked");
            //postFCM("OFF");
            postDB("OFF");
        } else if(view.getId() == R.id.button_req_dry) {
            Log.d(">>>", "req dry clicked");
            postDB("DRY");
        } else if(view.getId() == R.id.button_req_ping1) {
            Log.d(">>>", "ping1 clicked");
            postDB("PING1");
        } else if(view.getId() == R.id.button_req_ping2) {
            Log.d(">>>", "ping2 clicked");
            postDB("PING2");
        }
    }

    private void postDB(String s) {
        database.child("CMD").setValue(s)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        mainToast("db write successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        mainToast("db write failed");
                    }
                });
    }

    private void postFCM(String cmd) {
        // https://stackoverflow.com/questions/37978856/what-is-the-java-equivalent-to-use-https-fcm-googleapis-com-fcm-send-rest-api
        // https://stackoverflow.com/questions/6343166/how-do-i-fix-android-os-networkonmainthreadexception

        final String cmdF = cmd;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {

                    try {
                        String skey = "AAAAJQy1qGI:APA91bGmWrv7YrREuyPrw55Py-5nbTIxfq19nhS9EBXE7AMGWZYGGUgmMRrn4OWRawhJgBt7497e97M3xr2TubaVkvrcxil9WGG0vUKxR4RzHxc_y2UgwE_rOHuFdqGTGy_epyjq--cY";

                        HttpURLConnection httpcon = (HttpURLConnection) ((new URL("https://fcm.googleapis.com/fcm/send").openConnection()));
                        httpcon.setDoOutput(true);
                        httpcon.setRequestProperty("Content-Type", "application/json");
                        httpcon.setRequestProperty("Authorization", "key=" + skey);
                        httpcon.setRequestMethod("POST");
                        httpcon.connect();
                        Log.d(TAG, "Connected!");

                        //byte[] outputBytes = "{\"notification\":{\"title\": \"My title\", \"text\": \"My text\", \"sound\": \"default\"}, \"to\": \"cAhmJfN...bNau9z\"}".getBytes("UTF-8");
                        byte[] outputBytes = ("{"
                                //+ "\"registration_ids\":["
                                //+ "\"fj8_PCblTvQ:APA91bGIJe7rS8m1u0KQT8g77Gplw2ajhiep9lKlmaVNi4EnmSMrXs_PpM-4nT76IsXi2rEjQrpsEGoEJVWIsPHFksYJiorD6MZAZZgpx6srhvOHfCREMeRrwWRqpb2YxZyoY2NFkR0R\","
                                //+ "\"fkK4ONH3gN8:APA91bGUcHFiiKw2mfSsyzuEvbjS0L3o00DNrXwXAsyC0YyJGRm82hn8-6yUhX_Cva0VvvsyEkQ3Yio1cv3m2QEEfDEu6f31ve16hnXqzw4lQidC8Z73fLiMrWmXuj5UmVKs1b5lRBv7\""
                                //+ "],"
                                + "\"to\": \"/topics/comm\","
                                + "\"data\": {"
                                + "\"key1\" : \"" + android.os.Build.MODEL + "\","
                                + "\"key2\" : \"" + cmdF + "\","
                                + "}"
                                + "}").getBytes("UTF-8");
                        OutputStream os = httpcon.getOutputStream();
                        os.write(outputBytes);
                        os.close();

                        // Reading response
                        InputStream input = httpcon.getInputStream();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                            for (String line; (line = reader.readLine()) != null;) {
                                Log.d(TAG, line);
                            }
                        }

                        Log.d(TAG, "Http POST request sent!");
                        mainToast("sent request " + cmdF + " successfully.");
                    } catch (IOException e) {
                        e.printStackTrace();
                        mainToast("request " + cmdF + " failed. Network.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    mainToast("request " + cmdF + " failed. thread.");
                }
            }
        });

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    private void mainToast(String s) {
        final String str = s;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
            }
        });
    }

}
