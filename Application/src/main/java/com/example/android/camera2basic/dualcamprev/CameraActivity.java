/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic.dualcamprev;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity {
    //CheckThread mThread = new CheckThread();

    BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action){
                    case "com.android.server.display.broadcast.CONNECTION_CHANGED": {
                        int value = intent.getIntExtra("displayValue", -1);
                        boolean connected = intent.getBooleanExtra("connected", false);
                        Log.e("onReceive",
                                String.format("action: %s, value: %d, connected: %s", action, value, connected));
                        if(connected){
                            Camera2BasicFragment fragment = (Camera2BasicFragment) getSupportFragmentManager().getFragments().get(0);
                            fragment.refreshCamera();
                        }
                        break;
                    }
                    case "android.intent.action.HDMI_PLUGGED": {
                        boolean state = intent.getBooleanExtra("state", false);
                        Log.e("onReceive",
                                String.format("action: %s, state: %s", action, state));
                        Camera2BasicFragment fragment = (Camera2BasicFragment) getSupportFragmentManager().getFragments().get(0);
                        fragment.refreshCamera();
                        break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.android.server.display.broadcast.CONNECTION_CHANGED");
        filter.addAction("android.intent.action.HDMI_PLUGGED");
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("KeyF12", "Down");
        if (keyCode == KeyEvent.KEYCODE_F12){
            Camera2BasicFragment fragment = (Camera2BasicFragment) getSupportFragmentManager().getFragments().get(0);
            fragment.checkCameraMode(true);
//            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
//            tr.replace(R.id.container, Camera2BasicFragment.newInstance());
//            tr.commit();
        }
        return super.onKeyDown(keyCode, event);
    }



//    @Override
//    protected void onStart() {
//        super.onStart();
//        mThread.start();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        mThread.interrupt();
//    }
//
//    static class CheckThread extends Thread {
//        public void run() {
//            String org = "0";
//            while(! interrupted()){
//                try {
//                    String s = SystemProperties.get("sys.rk.sw1", "0");
//                    if(! org.equals(s)){
//                        org = s;
//                        Log.d("sys.rk.sw1", org);
//                    }
//                    sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}
