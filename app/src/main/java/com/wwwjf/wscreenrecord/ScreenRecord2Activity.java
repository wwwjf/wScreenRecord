package com.wwwjf.wscreenrecord;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.wwwjf.wscreenrecord.service.ScreenRecordService;
import com.wwwjf.wscreenrecord.service.ScreenRecorder;
import com.wwwjf.wscreenrecord.service.ScreenUtil;
import com.wwwjf.wscreenrecord.utils.CommonUtil;
import com.wwwjf.wscreenrecord.utils.PermissionUtils;
import com.wwwjf.wscreenrecord.utils.ToastUtil;

public class ScreenRecord2Activity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = ScreenRecord2Activity.class.getSimpleName();

    private TextView mTvStart;
    private TextView mTvEnd;

    private TextView mTvTime;

    private int REQUEST_CODE = 1;

    boolean isrun = false;//用来标记录屏的状态private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;//录制视频的工具private int width, height, dpi;//屏幕宽高和dpi，后面会用到
    private ScreenRecorder screenRecorder;//这个是自己写的录视频的工具类，下文会放完整的代码
    Thread thread;//录视频要放在线程里去执行
    MediaProjectionManager mediaProjectionManager;

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

        mediaProjectionManager = (MediaProjectionManager) this.getSystemService(MEDIA_PROJECTION_SERVICE);
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);


    }

    private ServiceConnection mServiceConnection;

    /**
     * 开启录制 Service
     */
    private void startScreenRecordService() {

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
                        ToastUtil.show(ScreenRecord2Activity.this, "取消");
                    }
                }).setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + ScreenRecord2Activity.this.getPackageName()));
                        ScreenRecord2Activity.this.startActivity(intent);
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
            Log.i(TAG, "onStopRecord: stopTip=" + stopTip);
            ToastUtil.show(ScreenRecord2Activity.this, stopTip);
        }

        @Override
        public void onRecording(String timeTip) {
            Log.i(TAG, "onRecording: timeTip=" + timeTip);
            mTvTime.setText(timeTip);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            try {
                ScreenUtil.setUpData(resultCode,data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ToastUtil.show(this,"拒绝录屏");
        }*/

        //方案2：
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection != null) {
                screenRecorder = new ScreenRecorder(CommonUtil.getScreenWidth(), CommonUtil.getScreenHeight(), mediaProjection, CommonUtil.getScreenDpi());
            }
            thread = new Thread() {
                @Override
                public void run() {
                    screenRecorder.startRecorder();//跟ScreenRecorder有关的下文再说，总之这句话的意思就是开始录屏的意思
                }
            };
            thread.start();
//        binding.startPlayer.setText("停止");//开始和停止我用的同一个按钮，所以开始录屏之后把按钮文字改一下
            isrun = true;//录屏状态改成真
        }

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.tv_start: {
//                ScreenUtil.startScreenRecord(this,REQUEST_CODE);
                if (mediaProjectionManager != null) {
                    Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                    PackageManager packageManager = this.getPackageManager();
                    if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        //存在录屏授权的Activity
                        this.startActivityForResult(intent, REQUEST_CODE);
                    } else {
                        Toast.makeText(this, R.string.can_not_record_tip, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case R.id.tv_end: {
//                ScreenUtil.stopScreenRecord(this);
                screenRecorder.stop();
                break;
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null){
            unbindService(mServiceConnection);
        }
    }
}