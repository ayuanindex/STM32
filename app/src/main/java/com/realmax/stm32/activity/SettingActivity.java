package com.realmax.stm32.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.realmax.stm32.App;
import com.realmax.stm32.R;
import com.realmax.stm32.bean.LinkBean;
import com.realmax.stm32.tcp.CustomerHandlerBase;
import com.realmax.stm32.tcp.NettyLinkUtil;
import com.realmax.stm32.utils.ThreadPoolManager;
import com.realmax.stm32.utils.ValueUtil;

import io.netty.channel.EventLoopGroup;

/**
 * @author ayuan
 */
public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingActivity";
    private ImageView ivBack;
    private CardView cardCameraConnected;
    private CardView cardImConnected;
    private TextView tvCamera;
    private TextView tvIm;

    private static void onClick(View v) {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
        initListener();
        initData();
    }

    private void initView() {
        ivBack = findViewById(R.id.iv_back);
        cardCameraConnected = findViewById(R.id.card_cameraConnected);
        cardImConnected = findViewById(R.id.card_imConnected);
        tvCamera = findViewById(R.id.tv_camera);
        tvIm = findViewById(R.id.tv_im);
    }

    private void initListener() {
        ivBack.setOnClickListener((View v) -> finish());

        cardCameraConnected.setOnClickListener((View v) -> {
            showDialog(ValueUtil.CAMERA);
        });

        cardImConnected.setOnClickListener((View v) -> {
            showDialog(ValueUtil.IM);
        });
    }

    private void initData() {

    }

    /**
     * 弹出输入IP和端口号的对话框进行连接
     *
     * @param type 设备标示符
     */
    private void showDialog(String type) {
        Boolean connectedStatus = ValueUtil.getConnectedStatus(type);
        Log.d(TAG, "showDialog: " + connectedStatus);
        if (connectedStatus) {
            App.showToast("已连接");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog = builder.create();
        View inflate = View.inflate(this, R.layout.dialog_connect, null);
        alertDialog.setView(inflate);
        ViewHolder viewHolder = new ViewHolder(inflate);

        viewHolder.cardLink.setOnClickListener((View v) -> {
            String ipStr = viewHolder.etIp.getText().toString().trim();
            if (TextUtils.isEmpty(ipStr)) {
                App.showToast("请输入IP地址");
                return;
            }

            String portStr = viewHolder.etPort.getText().toString().trim();
            if (TextUtils.isEmpty(portStr)) {
                App.showToast("请输入端口号");
                return;
            }
            alertDialog.dismiss();

            int portInt = Integer.parseInt(portStr);

            ThreadPoolManager.execute(() -> {
                LinkBean linkBean = new LinkBean(type);

                CustomerHandlerBase customerHandlerBase = ValueUtil.getHandler(type);

                linkBean.connected(ipStr, portInt, customerHandlerBase, new NettyLinkUtil.Callback() {
                    @Override
                    public void success(EventLoopGroup eventLoopGroup) {
                        if (type.equals(ValueUtil.CAMERA)) {
                            tvCamera.setBackgroundColor(getColor(R.color.connectedSuccess));
                        } else {
                            tvIm.setBackgroundColor(getColor(R.color.connectedSuccess));
                        }

                        // 存入状态
                        ValueUtil.putConnectedStatus(type, true);

                    }

                    @Override
                    public void error() {
                        if (type.equals(ValueUtil.CAMERA)) {
                            tvCamera.setBackgroundColor(getColor(R.color.connectedError));
                        } else {
                            tvIm.setBackgroundColor(getColor(R.color.connectedError));
                        }

                        customerHandlerBase.getCustomerCallback().disConnected();
                    }
                });
            });
        });

        viewHolder.cardCancel.setOnClickListener((View v) -> alertDialog.dismiss());

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        alertDialog.show();
    }

    public static class ViewHolder {
        public View rootView;
        public EditText etIp;
        public EditText etPort;
        public CardView cardLink;
        public CardView cardCancel;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.etIp = rootView.findViewById(R.id.et_ip);
            this.etPort = rootView.findViewById(R.id.et_port);
            this.cardLink = rootView.findViewById(R.id.card_link);
            this.cardCancel = rootView.findViewById(R.id.card_cancel);
        }
    }
}
