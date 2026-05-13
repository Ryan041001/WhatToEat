package com.zjgsu.whattoeat.integration.wechat;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.WechatAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WechatAuthClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private WechatAuthClient wechatAuthClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        wechatAuthClient = new WechatAuthClient(
                restClientBuilder,
                new WechatAuthProperties("test-app-id", "test-app-secret", "https://api.weixin.qq.com", false));
    }

    @Test
    void exchangeCodeShouldReturnOpenidAndSessionKey() {
        mockServer.expect(once(), requestTo("https://api.weixin.qq.com/sns/jscode2session?appid=test-app-id&secret=test-app-secret&js_code=wx-real-code-001&grant_type=authorization_code"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          \"openid\": \"openid-001\",
                          \"session_key\": \"session-key-001\"
                        }
                        """, MediaType.APPLICATION_JSON));

        WechatAuthGateway.WechatSession session = wechatAuthClient.exchangeCode("wx-real-code-001");

        assertEquals("openid-001", session.openid());
        assertEquals("session-key-001", session.sessionKey());
        mockServer.verify();
    }

    @Test
    void exchangeCodeShouldThrowLoginCodeInvalidForWechatInvalidCodeError() {
        mockServer.expect(once(), requestTo("https://api.weixin.qq.com/sns/jscode2session?appid=test-app-id&secret=test-app-secret&js_code=used-code-001&grant_type=authorization_code"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          \"errcode\": 40029,
                          \"errmsg\": \"invalid code\"
                        }
                        """, MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> wechatAuthClient.exchangeCode("used-code-001"));

        assertEquals(ErrorCode.LOGIN_CODE_INVALID, exception.getErrorCode());
        mockServer.verify();
    }
}
