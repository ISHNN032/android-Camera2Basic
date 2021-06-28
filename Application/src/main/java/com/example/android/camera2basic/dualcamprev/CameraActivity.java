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

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class CameraActivity extends AppCompatActivity {
    //CheckThread mThread = new CheckThread();

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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("KeyF12", "Down");
        if (keyCode == KeyEvent.KEYCODE_F12){
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.replace(R.id.container, Camera2BasicFragment.newInstance());
            tr.commit();
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
