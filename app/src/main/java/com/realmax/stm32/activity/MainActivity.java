package com.realmax.stm32.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textview.MaterialTextView;
import com.realmax.stm32.R;
import com.realmax.stm32.tcp.CustomerCallback;
import com.realmax.stm32.tcp.CustomerHandlerBase;
import com.realmax.stm32.utils.ValueUtil;
import com.realmax.stm32.view.ResultMaskView;

/**
 * @author ayuan
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ImageView ivLeft;
    private TextView tvCameraStatus;
    private CardView cardSetting;
    private ImageView ivImage;
    private ResultMaskView resultMaskView;
    private MaterialTextView tvRobotType;
    private MaterialTextView tvCurrentCamera;
    private MaterialTextView tvBatteryAngle;
    private MaterialTextView tvTarget;
    private MaterialTextView tvCoordinate;
    private MaterialTextView tvLocation;
    private RelativeLayout rlContent;
    private ImageView ivRight;
    private TextView tvImStatus;
    private SwitchMaterial swIsAi;
    private CustomerHandlerBase cameraHandler;
    private CustomerHandlerBase imHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        initData();
    }

    private void initView() {
        ivLeft = findViewById(R.id.iv_left);
        tvCameraStatus = findViewById(R.id.tv_cameraStatus);
        cardSetting = findViewById(R.id.card_setting);
        ivImage = findViewById(R.id.iv_image);
        resultMaskView = findViewById(R.id.resultMaskView);
        tvRobotType = findViewById(R.id.tv_robotType);
        tvCurrentCamera = findViewById(R.id.tv_currentCamera);
        tvBatteryAngle = findViewById(R.id.tv_batteryAngle);
        tvTarget = findViewById(R.id.tv_target);
        tvCoordinate = findViewById(R.id.tv_coordinate);
        tvLocation = findViewById(R.id.tv_location);
        rlContent = findViewById(R.id.rl_content);
        ivRight = findViewById(R.id.iv_right);
        tvImStatus = findViewById(R.id.tv_imStatus);
        swIsAi = findViewById(R.id.sw_isAi);
    }

    private void initEvent() {
        cardSetting.setOnClickListener((View v) -> {
            startActivity(new Intent(this, SettingActivity.class));
        });

        swIsAi.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {

        });
    }

    private void initData() {
        cameraHandler = new CustomerHandlerBase();
        imHandler = new CustomerHandlerBase();

        ValueUtil.getHandler(ValueUtil.CAMERA).setCustomerCallback(new CustomerCallback() {
            @Override
            public void disConnected() {
                Log.d(TAG, "disConnected: 摄像头断开");
            }

            @Override
            public void getResultData(String msg) {
                Log.d(TAG, "getResultData: " + msg);
            }
        });

        ValueUtil.getHandler(ValueUtil.IM).setCustomerCallback(new CustomerCallback() {
            @Override
            public void disConnected() {
                Log.d(TAG, "disConnected: im断开");
            }

            @Override
            public void getResultData(String msg) {
                Log.d(TAG, "getResultData: " + msg);
            }
        });
    }
}
