package com.zjgsu.whattoeat.integration.wechat;

public interface WechatAuthGateway {

    WechatSession exchangeCode(String code);

    record WechatSession(String openid, String sessionKey, String unionid) {
    }
}
