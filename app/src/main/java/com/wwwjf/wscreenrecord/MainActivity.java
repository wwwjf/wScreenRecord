package com.wwwjf.wscreenrecord;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wwwjf.wscreenrecord.MyView.SliderCloseView;
import com.wwwjf.wscreenrecord.utils.CommonUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView mTvAdd;
    private SliderCloseView mSliderView;

    private TextView mTvInner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonUtil.init(this);
        setContentView(R.layout.activity);
        mSliderView = findViewById(R.id.sliderview);
        mTvAdd = findViewById(R.id.tv_add);
        mTvAdd.setOnClickListener(this);

    }

    @Override
    public void onBackPressed() {
        if (mSliderView.isSliderViewVisible()){
            mSliderView.hiddenSliderView();
            return;
        }
        super.onBackPressed();
    }

    private void addInnerView(){

        View secondView = LayoutInflater.from(this).inflate(R.layout.inner_view,null);
        mTvInner = secondView.findViewById(R.id.tv_inner);
        mTvInner.setOnClickListener(this);
        mSliderView.addViewToLayout(secondView,CommonUtil.getScreenWidth());

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_add:{
                addInnerView();
                break;
            }
            case R.id.tv_inner:{
                Toast.makeText(this,"我在内嵌页面",Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this,ScreenRecordActivity.class));
                break;
            }
        }
    }
}
