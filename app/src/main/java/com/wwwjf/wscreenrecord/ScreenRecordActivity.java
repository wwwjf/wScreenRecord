package com.wwwjf.wscreenrecord;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.wwwjf.wscreenrecord.service.ScreenRecordService;
import com.wwwjf.wscreenrecord.service.ScreenUtil;
import com.wwwjf.wscreenrecord.utils.CommonUtil;
import com.wwwjf.wscreenrecord.utils.PermissionUtils;
import com.wwwjf.wscreenrecord.utils.ToastUtil;

public class ScreenRecordActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = ScreenRecordActivity.class.getSimpleName();

    private TextView mTvStart;
    private TextView mTvEnd;

    private TextView mTvTime;

    private int REQUEST_CODE = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_record_activity);

        CommonUtil.init(this);
        PermissionUtils.checkPermission(this);
        mTvStart = findViewById(R.id.tv_start);
        mTvStart.setOnClickListener(this);

        mTvTime = findViewById(R.id.tv_record_time);

        mTvEnd = findViewById(R.id.tv_end);
        mTvEnd.setOnClickListener(this);

       startScreenRecordService();

    }

    private ServiceConnection mServiceConnection;

    /**
     * 开启录制 Service
     */
    private void startScreenRecordService(){

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ScreenRecordService.RecordBinder recordBinder = (ScreenRecordService.RecordBinder) service;
                ScreenRecordService screenRecordService = recordBinder.getRecordService();
                ScreenUtil.setScreenService(screenRecordService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, ScreenRecordService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        ScreenUtil.addRecordListener(recordListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int temp : grantResults) {
            if (temp == PermissionChecker.PERMISSION_DENIED) {
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("申请权限").setMessage("这些权限很重要").setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ToastUtil.show(ScreenRecordActivity.this, "取消");
                    }
                }).setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + ScreenRecordActivity.this.getPackageName()));
                        ScreenRecordActivity.this.startActivity(intent);
                    }
                }).create();
                dialog.show();
                break;
            }
        }
    }

    private ScreenUtil.RecordListener recordListener = new ScreenUtil.RecordListener() {
        @Override
        public void onStartRecord() {
            Log.i(TAG, "onStartRecord: ");
        }

        @Override
        public void onPauseRecord() {
            Log.i(TAG, "onPauseRecord: ");
        }

        @Override
        public void onResumeRecord() {
            Log.i(TAG, "onResumeRecord: ");
        }

        @Override
        public void onStopRecord(String stopTip) {
            Log.i(TAG, "onStopRecord: stopTip="+stopTip);
            ToastUtil.show(ScreenRecordActivity.this,stopTip);
        }

        @Override
        public void onRecording(String timeTip) {
            Log.i(TAG, "onRecording: timeTip="+timeTip);
            mTvTime.setText(timeTip);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            try {
                ScreenUtil.setUpData(resultCode,data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ToastUtil.show(this,"拒绝录屏");
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.tv_start:{
                ScreenUtil.startScreenRecord(this,REQUEST_CODE);
                break;
            }
            case R.id.tv_end:{
                ScreenUtil.stopScreenRecord(this);
                break;
            }
        }

    }
}