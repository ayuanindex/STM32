package com.realmax.stm32.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textview.MaterialTextView;
import com.realmax.stm32.R;
import com.realmax.stm32.tcp.CustomerCallback;
import com.realmax.stm32.utils.EncodeAndDecode;
import com.realmax.stm32.utils.OrcUtils;
import com.realmax.stm32.utils.ThreadPoolManager;
import com.realmax.stm32.utils.ValueUtil;
import com.realmax.stm32.view.ResultMaskView;
import com.realmax.stm32.view.model.BaseRectBoundResultModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * @author ayuan
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String DEVICETYPE = "坦克";
    private CardView cardLeft;
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
    private CardView cardRight;
    private TextView tvImStatus;
    private SwitchMaterial swIsAi;
    private OrcUtils orcUtils;
    private Handler uiHandler;
    private boolean flag = true;
    private int currentCamera = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        requestPermission();
    }

    private void initView() {
        cardLeft = findViewById(R.id.card_left);
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
        cardRight = findViewById(R.id.card_right);
        tvImStatus = findViewById(R.id.tv_imStatus);
        swIsAi = findViewById(R.id.sw_isAi);
    }

    @SuppressLint("SetTextI18n")
    private void initEvent() {
        cardSetting.setOnClickListener((View v) -> {
            startActivity(new Intent(this, SettingActivity.class));
        });

        swIsAi.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            resultMaskView.clear();
        });

        cardLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCamera > 1) {
                    currentCamera--;
                }
                ValueUtil.sendCameraCmd(DEVICETYPE, currentCamera);
            }
        });

        cardRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCamera < 5) {
                    currentCamera++;
                }
                ValueUtil.sendCameraCmd(DEVICETYPE, currentCamera);
            }
        });
    }

    private void initData() {
        orcUtils = new OrcUtils(this);
        uiHandler = new Handler(getMainLooper());
        resultMaskView.setHandler(uiHandler);

        ValueUtil.getHandler(ValueUtil.CAMERA).setCustomerCallback(new CustomerCallback() {
            @Override
            public void disConnected() {
                Log.d(TAG, "disConnected: 摄像头断开");
                tvCameraStatus.setText("摄像头：断开连接");
            }

            @Override
            public void getResultData(String msg) {
                setImage(msg);
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

    /**
     * 请求必要的权限
     */
    @SuppressLint("NewApi")
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
        } else {
            initData();
        }
    }


    /**
     * 设置照片
     *
     * @param msg json字符串
     */
    @SuppressLint({"NewApi", "SetTextI18n"})
    private void setImage(String msg) {
        try {
            if (TextUtils.isEmpty(msg)) {
                return;
            }

            JSONObject jsonObject = new JSONObject(msg);
            String cameraNum = jsonObject.optString("cameraNum");

            String cameraImg = jsonObject.optString("cameraImg");
            Bitmap bitmap = EncodeAndDecode.base64ToImage(cameraImg);
            ThreadPoolManager.execute(() -> {

                runOnUiThread(() -> {
                    ivImage.setImageBitmap(bitmap);
                    if (Integer.parseInt(cameraNum) == currentCamera) {
                        tvCurrentCamera.setText("当前COM：" + currentCamera + "前置摄像头");
                    }
                });

                if (swIsAi.isChecked()) {
                    if (flag) {
                        flag = false;
                        orcUtils.detect(bitmap, (Bitmap resultBitmap, List<BaseRectBoundResultModel> resultModels) -> {
                            flag = true;
                            if (swIsAi.isChecked()) {
                                resultMaskView.setRectListInfo(resultModels, bitmap.getWidth(), bitmap.getHeight());
                            }
                        });
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            String substring = msg.substring(1);
            Log.d(TAG, "setImage: " + substring);
            setImage(substring);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ValueUtil.getConnectedStatus(ValueUtil.CAMERA)) {
            ValueUtil.sendCameraCmd(DEVICETYPE, currentCamera);
            tvCameraStatus.setText("摄像头：已连接");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ValueUtil.sendStopCmd();
        orcUtils.close();
    }
}
