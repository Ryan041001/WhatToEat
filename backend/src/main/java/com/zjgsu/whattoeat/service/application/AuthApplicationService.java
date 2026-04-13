package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.WechatAuthProperties;
import com.zjgsu.whattoeat.integration.wechat.WechatAuthGateway;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.service.application.auth.InMemorySessionStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private static final String MOCK_CODE_PREFIX = "mock-code-";

    private final UserRepository userRepository;
    private final InMemorySessionStore sessionStore;
    private final WechatAuthGateway wechatAuthGateway;
    private final WechatAuthProperties wechatAuthProperties;

    public AuthApplicationService(
            UserRepository userRepository,
            InMemorySessionStore sessionStore,
            WechatAuthGateway wechatAuthGateway,
            WechatAuthProperties wechatAuthProperties) {
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
        this.wechatAuthGateway = wechatAuthGateway;
        this.wechatAuthProperties = wechatAuthProperties;
    }

    @Transactional
    public LoginResult wechatLogin(String code, String nickname) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.LOGIN_CODE_INVALID);
        }

        String openid = resolveOpenid(code);
        UserEntity user = userRepository.findByOpenid(openid).orElseGet(() -> {
            UserEntity created = new UserEntity();
            created.setOpenid(openid);
            created.setNickname(nickname);
            return userRepository.save(created);
        });

        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
            user = userRepository.save(user);
        }

        String token = sessionStore.generateToken();
        sessionStore.save(token, user.getId());

        return new LoginResult(token, user);
    }

    private String resolveOpenid(String code) {
        if (code.startsWith(MOCK_CODE_PREFIX)) {
            if (!wechatAuthProperties.mockLoginEnabled()) {
                throw new BusinessException(ErrorCode.LOGIN_CODE_INVALID);
            }
            return "mock-openid-" + code;
        }

        if (!hasWechatCredentials()) {
            if (wechatAuthProperties.mockLoginEnabled()) {
                return "mock-openid-" + code;
            }
            throw new BusinessException(ErrorCode.LOGIN_CODE_INVALID);
        }

        return wechatAuthGateway.exchangeCode(code).openid();
    }

    private boolean hasWechatCredentials() {
        return isNotBlank(wechatAuthProperties.appId()) && isNotBlank(wechatAuthProperties.appSecret());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public void logout(String token) {
        if (sessionStore.getUserId(token) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        sessionStore.remove(token);
    }

    @Transactional(readOnly = true)
    public UserEntity me(String token) {
        Long userId = sessionStore.getUserId(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public record LoginResult(String token, UserEntity user) {
    }
}
