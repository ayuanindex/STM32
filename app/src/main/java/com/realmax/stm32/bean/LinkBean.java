package com.realmax.stm32.bean;

import com.realmax.stm32.tcp.CustomerHandlerBase;
import com.realmax.stm32.tcp.NettyLinkUtil;
import com.realmax.stm32.utils.ValueUtil;

/**
 * @author ayuan
 */
public class LinkBean {
    private String type;

    public LinkBean(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void connected(String ip, int port, CustomerHandlerBase customerHandlerBase, NettyLinkUtil.Callback status) {
        try {
            ValueUtil.putHandler(type, customerHandlerBase);
            NettyLinkUtil nettyLinkUtil = new NettyLinkUtil(ip, port);
            nettyLinkUtil.start(status, customerHandlerBase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
