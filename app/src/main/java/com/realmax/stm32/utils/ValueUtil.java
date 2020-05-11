package com.realmax.stm32.utils;

import android.util.Log;

import com.realmax.stm32.tcp.CustomerHandlerBase;

import java.util.HashMap;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author ayuan
 */
public class ValueUtil {
    private static final String TAG = "ValueUtil";
    public final static String CAMERA = "camera";
    public final static String IM = "im";

    /**
     * Netty的回调监听集合
     */
    private static HashMap<String, CustomerHandlerBase> handlerHashMap = new HashMap<>(1);

    /**
     * 连接状态的集合
     */
    private static HashMap<String, Boolean> isConnectedHashMap = new HashMap<>(1);

    /**
     * @return 返回handler的集合
     */
    public static HashMap<String, CustomerHandlerBase> getHandlerHashMap() {
        return handlerHashMap;
    }

    /**
     * @return 返回连接的集合
     */
    public static HashMap<String, Boolean> getIsConnectedHashMap() {
        return isConnectedHashMap;
    }

    /**
     * 存入已经连接的handler
     *
     * @param key                 handler的标示符
     * @param customerHandlerBase 需要存入的Handler
     */
    public static void putHandler(String key, CustomerHandlerBase customerHandlerBase) {
        handlerHashMap.put(key, customerHandlerBase);
    }

    /**
     * 存入指定的连接的状态
     *
     * @param key             连接状态的标示符
     * @param connectedStatus 需要存入的连接状态
     */
    public static void putConnectedStatus(String key, boolean connectedStatus) {
        isConnectedHashMap.put(key, connectedStatus);
    }

    /**
     * 获取指定的Handler
     *
     * @param key handler的标示符
     * @return 返回指定的Handler
     */
    public static CustomerHandlerBase getHandler(String key) {
        CustomerHandlerBase customerHandlerBase = handlerHashMap.get(key);
        if (customerHandlerBase == null) {
            customerHandlerBase = new CustomerHandlerBase();
            handlerHashMap.put(key, customerHandlerBase);
        }
        return customerHandlerBase;
    }

    /**
     * 获取指定连接的壮腿
     *
     * @param key 连接状态的标示符
     * @return 返回连接状态
     */
    public static Boolean getConnectedStatus(String key) {
        Boolean aBoolean = isConnectedHashMap.get(key);
        if (aBoolean == null) {
            aBoolean = false;
            isConnectedHashMap.put(key, aBoolean);
        }
        return aBoolean;
    }

    /**
     * 发送获取摄像头摄像数据的指令
     *
     * @param deviceType 设备类型
     * @param cameraNum  摄像头编号
     */
    public static void sendCameraCmd(String deviceType, int cameraNum) {
        CustomerHandlerBase customerHandler = getHandlerHashMap().get(CAMERA);
        if (customerHandler == null) {
            return;
        }

        ChannelHandlerContext handlerContext = customerHandler.getHandlerContext();

        if (handlerContext == null) {
            return;
        }

        String command = "{\"cmd\": \"start\", \"deviceType\": \"" + deviceType + "\", \"deviceId\": 1, \"cameraNum\": " + cameraNum + "}";
        Log.d(TAG, "sendCameraCmd: " + command);
        /*String command = "{\"cmd\": \"start\", \"deviceType\": \"十字交叉路口\", \"deviceId\": 1, \"cameraNum\": 1}";*/
        handlerContext.writeAndFlush(Unpooled.copiedBuffer(option(EncodeAndDecode.getStrUnicode(command), (byte) 0x82)));
    }

    /**
     * 发送停止获取摄像头拍摄信心的指令
     */
    public static void sendStopCmd() {
        try {
            CustomerHandlerBase customerHandler = getHandlerHashMap().get("camera");
            if (customerHandler == null) {
                return;
            }

            ChannelHandlerContext handlerContext = customerHandler.getHandlerContext();

            if (handlerContext == null) {
                return;
            }

            String command = "{\"cmd\": \"stop\"}";
            handlerContext.writeAndFlush(Unpooled.copiedBuffer(option(EncodeAndDecode.getStrUnicode(command), (byte) 0x02)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将需要发送的消息加工成服务端可识别的数据
     *
     * @param command 需要发送的指令
     * @param b       版本号
     * @return 返回即将要发送的数据的byte数组
     */
    private static byte[] option(String command, byte b) {
        // 将指令转换成byte数组（此处的指令是已经转换成了Unicode编码，如果不转换长度计算会有问题）
        byte[] commandBytes = command.getBytes();
        // 这里的长度是字节长度（总长度是数据的字节长度+其他数据的长度：帧头、帧尾……）
        int size = commandBytes.length + 10;
        // 帧长度=总长度-帧头的长度（2byte）-帧尾的长度(2byte)
        int headLen = size - 4;
        // 将帧长度转换成小端模式
        byte[] lens = int2Bytesle(headLen);
        // 将需要验证的数据合并成一个byte数组
        // 将所有的参数放进去（其中帧头、协议版本号、帧尾是不变的数据）
        // 注意：需要将每个16进制的数据单独当成byte数组的一个元素，例：0xffaa -->  new byte[]{(byte) 0xff, (byte) 0xaa},需要拆分开
        byte[] combine = combine(new byte[]{(byte) 0xff, (byte) 0xaa, b}, lens, commandBytes, new byte[]{(byte) 0x00, (byte) 0xff, (byte) 0x55});
        // 进行加和校验
        int checkSum = checkSum(combine, size);
        return combine(
                new byte[]{
                        (byte) 0xff,
                        (byte) 0xaa,
                        b,
                        (byte) Integer.parseInt(Integer.toHexString(lens[0]), 16),
                        (byte) Integer.parseInt(Integer.toHexString(lens[1]), 16),
                        (byte) Integer.parseInt(Integer.toHexString(lens[2]), 16),
                        (byte) Integer.parseInt(Integer.toHexString(lens[3]), 16)
                },
                commandBytes,
                new byte[]{
                        (byte) Integer.parseInt(Integer.toHexString(checkSum), 16),
                        (byte) 0xff,
                        (byte) 0x55
                }
        );
    }

    /**
     * 加和校验
     *
     * @param bytes 需要校验的byte数组
     * @return 返回校验结果（16进制数据）
     */
    private static int checkSum(byte[] bytes, int size) {
        int cs = 0;
        int i = 2;
        int j = size - 3;
        while (i < j) {
            cs += bytes[i];
            i += 1;
        }
        return cs & 0xff;
    }

    /**
     * int转换为小端byte[]（高位放在高地址中）
     *
     * @param iValue 需要转换的数字
     * @return 返回小端模式的byte数组
     */
    private static byte[] int2Bytesle(int iValue) {
        byte[] rst = new byte[4];
        // 先写int的最后一个字节
        rst[0] = (byte) (iValue & 0xFF);
        // int 倒数第二个字节
        rst[1] = (byte) ((iValue & 0xFF00) >> 8);
        // int 倒数第三个字节
        rst[2] = (byte) ((iValue & 0xFF0000) >> 16);
        // int 第一个字节
        rst[3] = (byte) ((iValue & 0xFF000000) >> 24);
        return rst;
    }

    /**
     * 任意个byte数组合并
     *
     * @param bytes 多个byte数组
     * @return 发挥合并后的byte数组
     */
    private static byte[] combine(byte[]... bytes) {
        // 开始合并的位置
        int position = 0;
        // 新数组的总长度
        int length = 0;
        // 算出新数组的总长度
        for (byte[] aByte : bytes) {
            length += aByte.length;
        }
        // 创建一个新的byte数组
        byte[] ret = new byte[length];
        // 将byte数组合并成一个byte数组
        for (byte[] aByte : bytes) {
            // 参数1：待合并的数组
            // 参数2：开始合并的位置（从参数一的第n哥元素开始合并）
            // 参数3：合并的目标数组
            // 参数4：在目标数组的开始位置
            // 参数5：<=参数一的长度（这里取值为参数一的总长度相当于参数一的所有元素）
            System.arraycopy(aByte, 0, ret, position, aByte.length);
            // 计算合并下一个数组在新数组中的开始位置
            position += aByte.length;
        }
        return ret;
    }
}
