package com.realmax.stm32.utils;

import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {
    /**
     * 适用于百度EasyDL 离线SDK服务请求
     *
     * @param reqUrl 接口地址
     * @return java.lang.String
     * @author 小帅丶
     * @date 2019/5/8
     **/
    public static String doPostFile(String reqUrl, Bitmap resultBitmap, ResultData resultData) {
        HttpURLConnection url_con = null;
        String responseContent = null;
        try {
            URL url = new URL(reqUrl);
            url_con = (HttpURLConnection) url.openConnection();
            url_con.setRequestMethod("POST");
            url_con.setDoOutput(true);
            url_con.setRequestProperty("Content-type", "application/x-java-serialized-object");
            byte[] data = bitmap2Byte(resultBitmap);
            url_con.getOutputStream().write(data, 0, data.length);
            url_con.getOutputStream().flush();
            url_con.getOutputStream().close();

            InputStream in = url_con.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer tempStr = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                tempStr.append(tempLine);
                tempStr.append(crlf);
                tempLine = rd.readLine();
            }
            responseContent = tempStr.toString();
            rd.close();
            in.close();
        } catch (IOException e) {
            System.out.println("请求错信息:" + e.getMessage());
        } finally {
            if (url_con != null) {
                url_con.disconnect();
            }
        }
        resultData.getResult(responseContent,resultBitmap);
        return responseContent;
    }

    private static byte[] bitmap2Byte(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public interface ResultData {
        void getResult(String responseContent, Bitmap bitmap);
    }
}
