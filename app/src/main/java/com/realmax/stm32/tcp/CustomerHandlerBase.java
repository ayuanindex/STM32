package com.realmax.stm32.tcp;

import org.json.JSONException;
import org.json.JSONObject;

import io.netty.channel.ChannelHandlerContext;

public class CustomerHandlerBase extends BaseNettyHandler {
    private static final String TAG = "CustomerHandlerBase";
    private ChannelHandlerContext handlerContext;
    private CustomerCallback customerCallback;
    private boolean flag = false;
    private StringBuffer strings = new StringBuffer();
    private String currentCommand = "";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.handlerContext = ctx;
    }

    @Override
    public void callbackFunction(String jsonStr) {
        if (customerCallback != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                currentCommand = jsonStr;
                customerCallback.getResultData(jsonStr);
            } catch (JSONException e) {
                getJson(jsonStr);
            }
        }
    }

    private void getJson(String jsonStr) {
        for (char c : jsonStr.toCharArray()) {
            // 判断是否已经开始记录阶段
            if (c == '{') {
                // 设置flag标记，将开始记录数据
                flag = true;
            }
            if (flag) {
                // 通过stringBuilder来拼接字符串44
                strings.append(c);
            }
            // 判断是否已经是右边的括号
            if (c == '}') {
                // flag设置为false停止记录
                // 将StringBuilder记录的整段的字符串提取出来
                // 初始化StringBuilder
                flag = false;
                String json = strings.toString();
                strings = new StringBuffer();
                currentCommand = json;
                customerCallback.getResultData(json);
            }
        }
    }

    public ChannelHandlerContext getHandlerContext() {
        return handlerContext;
    }

    public void setHandlerContext(ChannelHandlerContext handlerContext) {
        this.handlerContext = handlerContext;
    }

    public CustomerCallback getCustomerCallback() {
        return customerCallback;
    }

    public void setCustomerCallback(CustomerCallback customerCallback) {
        this.customerCallback = customerCallback;
    }

    public String getCurrentCommand() {
        return currentCommand;
    }

    public void setCurrentCommand(String currentCommand) {
        this.currentCommand = currentCommand;
    }
}
