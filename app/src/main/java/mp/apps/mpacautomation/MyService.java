package mp.apps.mpacautomation;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyService extends Service {

    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";


    private static final String TAG = ">>>>>>>MS";
    private DatabaseReference database;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        // do your jobs here
        Log.d(TAG, "onStartCommand");
        database = FirebaseDatabase.getInstance().getReference("mpacautomation");
        database.child("CMD").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.getValue(String.class);
                Log.d(TAG, "changed: " + s);

                if ("OFF".equals(s)) {
                    InfraRed IR = new InfraRed(MyService.this);
                    IR.call("OFF");
                } else if ("DRY".equals(s)) {
                    InfraRed IR = new InfraRed(MyService.this);
                    IR.call("DRY");
                }

                if (!"".equals(s)) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            database.child("CMD").setValue("");
                        }
                    }, 500);
                }
                BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
                int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                //int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS); // api 26 (android 8.0)
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                String str = "% ";
                if (isCharging)
                    str += "charging";
                else
                    str += "discharging";
                database.child("BAT").setValue(batLevel + str);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
           }
        });
        //database.setValue("Hello, World!");


        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    // https://stackoverflow.com/questions/34573109/how-to-make-an-android-app-to-always-run-in-background
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build());
    }
}