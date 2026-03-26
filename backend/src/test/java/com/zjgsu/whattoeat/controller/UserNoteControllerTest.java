package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.model.entity.UserRestaurantNoteEntity;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.repository.UserRestaurantNoteRepository;
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
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserNoteControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRestaurantNoteRepository userRestaurantNoteRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRestaurantNoteRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createNoteShouldReturn201AndPersistRecord() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-create-success", "Alice");
        String token = loginAndExtractToken("mock-code-note-create-success", "Alice");

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE001\",\"content\":\"must try braised chicken\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());

        UserRestaurantNoteEntity saved = userRestaurantNoteRepository.findByUserIdAndPoiId(user.getId(), "B0FFNOTE001")
                .orElseThrow();
        assertThat(saved.getNote()).isEqualTo("must try braised chicken");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void createNoteShouldReturn401WhenAuthorizationMissing() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-create-noauth", "Bob");

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE002\",\"content\":\"late-night noodles\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void createNoteShouldReturn409WhenDuplicateRecordExists() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-create-duplicate", "Cathy");
        String token = loginAndExtractToken("mock-code-note-create-duplicate", "Cathy");

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE003\",\"content\":\"crispy duck\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE003\",\"content\":\"crispy duck again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2005));
    }

    @Test
    void createNoteShouldReturn400WhenContentBlank() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-create-invalid", "David");
        String token = loginAndExtractToken("mock-code-note-create-invalid", "David");

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE004\",\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    void createNoteShouldReturn400WhenContentMissing() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-create-missing-content", "Doris");
        String token = loginAndExtractToken("mock-code-note-create-missing-content", "Doris");

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE005\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void createNoteShouldReturn400WhenContentTooLong() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-create-too-long", "Duke");
        String token = loginAndExtractToken("mock-code-note-create-too-long", "Duke");
        String content = String.join("", Collections.nCopies(1001, "a"));

        mockMvc.perform(post("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FFNOTE006\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    void listNotesShouldReturnPaginatedData() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-list-success", "Eve");
        String token = loginAndExtractToken("mock-code-note-list-success", "Eve");
        createNote(user.getId(), "B0FFNOTE101", "first", LocalDateTime.of(2026, 3, 26, 8, 0), LocalDateTime.of(2026, 3, 26, 8, 5));
        createNote(user.getId(), "B0FFNOTE102", "second", LocalDateTime.of(2026, 3, 26, 9, 0), LocalDateTime.of(2026, 3, 26, 9, 5));
        createNote(user.getId(), "B0FFNOTE103", "third", LocalDateTime.of(2026, 3, 26, 10, 0), LocalDateTime.of(2026, 3, 26, 10, 5));

        mockMvc.perform(get("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].poiId").value("B0FFNOTE103"))
                .andExpect(jsonPath("$.data.items[0].content").value("third"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-03-26T10:00:00"))
                .andExpect(jsonPath("$.data.items[0].updatedAt").value("2026-03-26T10:05:00"))
                .andExpect(jsonPath("$.data.items[1].poiId").value("B0FFNOTE102"));
    }

    @Test
    void listNotesShouldReturn401WhenTokenUserDoesNotMatchPathUser() throws Exception {
        UserEntity owner = createUser("mock-openid-note-list-owner", "Frank");
        createUser("mock-openid-mock-code-note-list-other", "Grace");
        String otherUserToken = loginAndExtractToken("mock-code-note-list-other", "Grace");

        mockMvc.perform(get("/api/v1/users/{userId}/notes", owner.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void listNotesShouldOrderByUpdatedAtDescWithStableIdDescAcrossPages() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-list-updated-order", "Nina");
        String token = loginAndExtractToken("mock-code-note-list-updated-order", "Nina");
        LocalDateTime recentUpdatedAt = LocalDateTime.of(2026, 3, 26, 15, 0);
        LocalDateTime olderUpdatedAt = LocalDateTime.of(2026, 3, 26, 14, 0);

        createNote(user.getId(), "B0FFNOTE501", "first recent", LocalDateTime.of(2026, 3, 26, 8, 0), recentUpdatedAt);
        createNote(user.getId(), "B0FFNOTE502", "second recent", LocalDateTime.of(2026, 3, 26, 9, 0), recentUpdatedAt);
        createNote(user.getId(), "B0FFNOTE503", "first older", LocalDateTime.of(2026, 3, 26, 10, 0), olderUpdatedAt);
        createNote(user.getId(), "B0FFNOTE504", "second older", LocalDateTime.of(2026, 3, 26, 11, 0), olderUpdatedAt);

        mockMvc.perform(get("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].poiId").value("B0FFNOTE502"))
                .andExpect(jsonPath("$.data.items[1].poiId").value("B0FFNOTE501"));

        mockMvc.perform(get("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].poiId").value("B0FFNOTE504"))
                .andExpect(jsonPath("$.data.items[1].poiId").value("B0FFNOTE503"));
    }

    @Test
    void listNotesShouldSupportOptionalKeywordFilteringByContent() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-list-keyword", "Olivia");
        String token = loginAndExtractToken("mock-code-note-list-keyword", "Olivia");
        createNote(user.getId(), "B0FFNOTE601", "spicy noodles near dorm", LocalDateTime.of(2026, 3, 26, 8, 0), LocalDateTime.of(2026, 3, 26, 8, 30));
        createNote(user.getId(), "B0FFNOTE602", "quiet cafe for study", LocalDateTime.of(2026, 3, 26, 9, 0), LocalDateTime.of(2026, 3, 26, 9, 30));
        createNote(user.getId(), "B0FFNOTE603", "extra spicy hotpot", LocalDateTime.of(2026, 3, 26, 10, 0), LocalDateTime.of(2026, 3, 26, 10, 30));

        mockMvc.perform(get("/api/v1/users/{userId}/notes", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "spicy")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].poiId").value("B0FFNOTE603"))
                .andExpect(jsonPath("$.data.items[1].poiId").value("B0FFNOTE601"));
    }

    @Test
    void getNoteDetailShouldReturn200AndData() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-detail-success", "Helen");
        String token = loginAndExtractToken("mock-code-note-detail-success", "Helen");
        UserRestaurantNoteEntity note = createNote(
                user.getId(),
                "B0FFNOTE201",
                "best after class",
                LocalDateTime.of(2026, 3, 26, 11, 0),
                LocalDateTime.of(2026, 3, 26, 11, 30));

        mockMvc.perform(get("/api/v1/users/{userId}/notes/{noteId}", user.getId(), note.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(note.getId()))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.poiId").value("B0FFNOTE201"))
                .andExpect(jsonPath("$.data.content").value("best after class"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-26T11:00:00"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-03-26T11:30:00"));
    }

    @Test
    void getNoteDetailShouldReturn404WhenRecordDoesNotExist() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-detail-missing", "Ivy");
        String token = loginAndExtractToken("mock-code-note-detail-missing", "Ivy");

        mockMvc.perform(get("/api/v1/users/{userId}/notes/{noteId}", user.getId(), 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2004));
    }

    @Test
    void getNoteDetailShouldReturn400WhenNoteIdNotPositive() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-detail-invalid-id", "Iris");
        String token = loginAndExtractToken("mock-code-note-detail-invalid-id", "Iris");

        mockMvc.perform(get("/api/v1/users/{userId}/notes/{noteId}", user.getId(), 0)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void updateNoteShouldReturn200AndPersistContent() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-update-success", "Jack");
        String token = loginAndExtractToken("mock-code-note-update-success", "Jack");
        UserRestaurantNoteEntity note = createNote(
                user.getId(),
                "B0FFNOTE301",
                "before update",
                LocalDateTime.of(2026, 3, 26, 12, 0),
                LocalDateTime.of(2026, 3, 26, 12, 10));

        mockMvc.perform(put("/api/v1/users/{userId}/notes/{noteId}", user.getId(), note.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"after update\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(note.getId()))
                .andExpect(jsonPath("$.data.content").value("after update"))
                .andExpect(jsonPath("$.data.poiId").value("B0FFNOTE301"));

        LocalDateTime originalUpdatedAt = note.getUpdatedAt();
        UserRestaurantNoteEntity updated = userRestaurantNoteRepository.findByIdAndUserId(note.getId(), user.getId())
                .orElseThrow();
        assertThat(updated.getNote()).isEqualTo("after update");
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void updateNoteShouldReturn400WhenContentBlank() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-update-invalid", "Luna");
        String token = loginAndExtractToken("mock-code-note-update-invalid", "Luna");
        UserRestaurantNoteEntity note = createNote(
                user.getId(),
                "B0FFNOTE302",
                "before update",
                LocalDateTime.of(2026, 3, 26, 12, 30),
                LocalDateTime.of(2026, 3, 26, 12, 35));

        mockMvc.perform(put("/api/v1/users/{userId}/notes/{noteId}", user.getId(), note.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    void updateNoteShouldReturn400WhenContentMissing() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-update-missing-content", "Leo");
        String token = loginAndExtractToken("mock-code-note-update-missing-content", "Leo");
        UserRestaurantNoteEntity note = createNote(
                user.getId(),
                "B0FFNOTE303",
                "before update",
                LocalDateTime.of(2026, 3, 26, 12, 40),
                LocalDateTime.of(2026, 3, 26, 12, 45));

        mockMvc.perform(put("/api/v1/users/{userId}/notes/{noteId}", user.getId(), note.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void updateNoteShouldReturn400WhenContentTooLong() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-update-too-long", "Lena");
        String token = loginAndExtractToken("mock-code-note-update-too-long", "Lena");
        UserRestaurantNoteEntity note = createNote(
                user.getId(),
                "B0FFNOTE304",
                "before update",
                LocalDateTime.of(2026, 3, 26, 12, 50),
                LocalDateTime.of(2026, 3, 26, 12, 55));
        String content = String.join("", Collections.nCopies(1001, "b"));

        mockMvc.perform(put("/api/v1/users/{userId}/notes/{noteId}", user.getId(), note.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    void updateNoteShouldReturn400WhenNoteIdNotPositive() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-update-invalid-id", "Luke");
        String token = loginAndExtractToken("mock-code-note-update-invalid-id", "Luke");

        mockMvc.perform(put("/api/v1/users/{userId}/notes/{noteId}", user.getId(), 0)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"after update\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void deleteNoteShouldReturn200AndRemoveRecord() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-delete-success", "Mike");
        String token = loginAndExtractToken("mock-code-note-delete-success", "Mike");
        UserRestaurantNoteEntity note = createNote(
                user.getId(),
                "B0FFNOTE401",
                "delete me",
                LocalDateTime.of(2026, 3, 26, 13, 0),
                LocalDateTime.of(2026, 3, 26, 13, 10));

        mockMvc.perform(delete("/api/v1/users/{userId}/notes/{noteId}", user.getId(), note.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(userRestaurantNoteRepository.findByIdAndUserId(note.getId(), user.getId())).isEmpty();
    }

    @Test
    void deleteNoteShouldReturn400WhenNoteIdNotPositive() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-note-delete-invalid-id", "Mia");
        String token = loginAndExtractToken("mock-code-note-delete-invalid-id", "Mia");

        mockMvc.perform(delete("/api/v1/users/{userId}/notes/{noteId}", user.getId(), 0)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }

    private UserRestaurantNoteEntity createNote(
            Long userId,
            String poiId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        UserRestaurantNoteEntity entity = new UserRestaurantNoteEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setNote(content);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return userRestaurantNoteRepository.saveAndFlush(entity);
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
