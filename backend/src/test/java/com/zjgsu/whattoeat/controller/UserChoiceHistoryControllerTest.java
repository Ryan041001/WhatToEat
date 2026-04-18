package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserChoiceHistoryControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserChoiceHistoryRepository userChoiceHistoryRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userChoiceHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createChoiceHistoryShouldReturn201AndPersistRecord() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-choice-history-001", "Alice");
        String token = loginAndExtractToken("mock-code-choice-history-001", "Alice");

        mockMvc.perform(post("/api/v1/users/{userId}/choice-history", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFCH001\",\"poiName\":\"兰州拉面\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(userChoiceHistoryRepository.findByUserIdOrderByChosenAtDesc(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent())
                .extracting(UserChoiceHistoryEntity::getPoiId)
                .containsExactly("B0FFCH001");
    }

    @Test
    void listChoiceHistoryShouldReturnPaginatedItemsOrderedByChosenAtDesc() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-choice-history-002", "Bob");
        String token = loginAndExtractToken("mock-code-choice-history-002", "Bob");
        createChoice(user.getId(), "B0FFCH101", "轻食工坊", LocalDateTime.of(2026, 4, 18, 10, 0));
        createChoice(user.getId(), "B0FFCH102", "兰州拉面", LocalDateTime.of(2026, 4, 18, 11, 0));

        mockMvc.perform(get("/api/v1/users/{userId}/choice-history", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].poiId").value("B0FFCH102"))
                .andExpect(jsonPath("$.data.items[0].poiName").value("兰州拉面"))
                .andExpect(jsonPath("$.data.items[1].poiId").value("B0FFCH101"));
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.saveAndFlush(user);
    }

    private UserChoiceHistoryEntity createChoice(Long userId, String poiId, String poiName, LocalDateTime chosenAt) {
        UserChoiceHistoryEntity entity = new UserChoiceHistoryEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiName(poiName);
        entity.setChosenAt(chosenAt);
        return userChoiceHistoryRepository.saveAndFlush(entity);
    }

    private String loginAndExtractToken(String code, String nickname) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extractToken(loginResult.getResponse().getContentAsString());
    }

    private String extractToken(String responseBody) {
        Matcher matcher = TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("token not found in response: " + responseBody);
        }
        return matcher.group(1);
    }
}
