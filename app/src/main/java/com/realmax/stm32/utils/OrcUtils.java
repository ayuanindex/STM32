package com.realmax.stm32.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.ddk.DDKConfig;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferInterface;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeConfig;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import com.baidu.ai.edge.core.util.FileUtil;
import com.baidu.ai.edge.core.util.Util;
import com.google.gson.Gson;
import com.realmax.stm32.bean.ConfigBean;
import com.realmax.stm32.view.model.BaseRectBoundResultModel;
import com.realmax.stm32.view.model.DetectResultModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrcUtils {
    private static final String TAG = "OrcUtils";
    /**
     * 请替换为您的序列号
     */
    private static final String SERIAL_NUM = "9231-7B58-0AA2-C403";
    private static final int MODEL_DETECT = 2;
    private Context context;
    private ConfigBean configBean;
    private String currentSoc;
    private int platform;
    private static final int TYPE_INFER = 0;
    private static final int TYPE_DDK150 = 1;
    private static final int TYPE_DDK200 = 11;
    private static final int TYPE_SNPE = 2;

    InferInterface mInferManager;

    public OrcUtils(Context context) {
        this.context = context;
        initConfig();
        init();
    }

    /**
     * 初始化配置文件
     */
    private void initConfig() {
        try {
            String configJson = FileUtil.readAssetFileUtf8String(context.getAssets(), "demo/config.json");
            configBean = new Gson().fromJson(configJson, ConfigBean.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        ThreadPoolManager.execute(new Runnable() {
            @Override
            public void run() {
                // 验证芯片类型
                if (checkChip()) {
                    initManager();

                    // 选择设备类型
                    choosePlatform();
                }
            }
        });
    }

    /**
     * 执行识别操作
     *
     * @param bitmap   需要识别的图像
     * @param listener 识别成功的回调
     */
    public void detect(Bitmap bitmap, Listener listener) {
        ThreadPoolManager.execute(() -> OrcUtils.this.setPictureProcess(bitmap, listener));
    }

    /**
     * 验证新品类型是否支持
     *
     * @return 返回验证结果
     */
    private boolean checkChip() {
        if (configBean.getSoc().contains("dsp") && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            currentSoc = "dsp";
            return true;
        } else if (configBean.getSoc().contains("npu") && (Build.HARDWARE.contains("kirin970") || Build.HARDWARE.contains("kirin980"))) {
            if (Build.HARDWARE.contains("kirin970")) {
                currentSoc = "npu150";
            }
            if (Build.HARDWARE.contains("kirin980")) {
                currentSoc = "npu200";
            }
            return true;
        } else if (configBean.getSoc().contains("arm")) {
            currentSoc = "arm";
            return true;
        }
        return false;
    }

    /**
     * 初始化管理器
     */
    private void initManager() {
        ThreadPoolManager.executeSingle(() -> {
            try {
                Log.d(TAG, "initManager: ");
                if (configBean.getModel_type() == MODEL_DETECT) {
                    switch (platform) {
                        case TYPE_DDK200:
                            DDKConfig mDetectConfig = new DDKConfig(context.getAssets(), "ddk-detect/config.json");
                            mInferManager = new DDKManager(context, mDetectConfig, SERIAL_NUM);
                            break;
                        case TYPE_SNPE:
                            SnpeConfig mSnpeClassifyConfig = new SnpeConfig(context.getAssets(), "snpe-detect/config.json");
                            mInferManager = new SnpeManager(context, mSnpeClassifyConfig, SERIAL_NUM);
                            break;
                        case TYPE_INFER:
                        default:
                            InferConfig mInferConfig = new InferConfig(context.getAssets(), "infer-detect/config.json");
                            // 可修改ARM推断使用的CPU核数
                            mInferConfig.setThread(Util.getInferCores());
                            mInferManager = new InferManager(context, mInferConfig, SERIAL_NUM);
                            break;
                    }
                }
            } catch (BaseException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 选择设备类型
     */
    private void choosePlatform() {
        switch (currentSoc) {
            case "dsp":
                platform = TYPE_SNPE;
                break;
            case "npu150":
                platform = TYPE_DDK150;
                break;
            case "npu200":
                platform = TYPE_DDK200;
                break;
            default:
            case "arm":
                platform = TYPE_INFER;
        }
    }

    private void setPictureProcess(Bitmap bitmap, Listener listener) {
        // 模块中的
        ThreadPoolManager.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 线程同步
                    synchronized (this) {
                        // 解决检测结果
                        if (mInferManager == null) {
                            Log.d(TAG, "onDetectBitmap: 模型初始化中，请稍后");
                            listener.onResult(bitmap, null);
                            return;
                        } else {
                            float threshold = BaseConfig.DEFAULT_THRESHOLD;
                            Log.d(TAG, "run: " + threshold);
                            List<DetectionResultModel> modelList = mInferManager.detect(bitmap, 0.9f);
                            listener.onResult(bitmap, fillDetectionResultModel(modelList));
                        }
                    }
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 填充检测结果模型
     *
     * @param modelList 识别结果
     * @return 返回转换后的识别结果
     */
    private List<BaseRectBoundResultModel> fillDetectionResultModel(List<DetectionResultModel> modelList) {
        List<BaseRectBoundResultModel> results = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            DetectionResultModel mDetectionResultModel = modelList.get(i);
            DetectResultModel mDetectResultModel = new DetectResultModel();
            mDetectResultModel.setIndex(i + 1);
            mDetectResultModel.setConfidence(mDetectionResultModel.getConfidence());
            mDetectResultModel.setName(mDetectionResultModel.getLabel());
            mDetectResultModel.setBounds(mDetectionResultModel.getBounds());
            results.add(mDetectResultModel);
        }
        return results;
    }

    public void close() {
        try {
            if (mInferManager != null) {
                mInferManager.destroy();
            }
        } catch (BaseException e) {
            e.printStackTrace();
        }
    }

    public interface Listener {
        void onResult(Bitmap resultBitmap, List<BaseRectBoundResultModel> resultModels);
    }
}
