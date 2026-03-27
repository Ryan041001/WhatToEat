package com.zjgsu.whattoeat.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

    private final String contract = Files.readString(Path.of("..", "docs", "api.yaml"));

    OpenApiContractTest() throws IOException {
    }

    @Test
    void wechatLoginSuccessResponseShouldUseTypedEnvelopeSchema() throws IOException {
        assertTrue(contract.contains("/api/v1/auth/wechat-login:\n    post:\n      tags: [Auth]"));
        assertTrue(contract.contains("'201':\n          description: 登录成功并创建会话\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/LoginSuccessResponse'"));
        assertTrue(contract.contains("LoginSuccessResponse:\n      type: object"));
        assertTrue(contract.contains("data:\n          $ref: '#/components/schemas/LoginResponse'"));
    }

    @Test
    void payloadSuccessResponsesShouldUseExplicitEnvelopeSchemas() {
        assertTrue(contract.contains("/api/v1/auth/me:\n    get:"));
        assertTrue(contract.contains("'200':\n          description: 获取成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/UserInfoSuccessResponse'"));

        assertTrue(contract.contains("/api/v1/restaurants/nearby:\n    get:"));
        assertTrue(contract.contains("'200':\n          description: 查询成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/RestaurantPageSuccessResponse'"));

        assertTrue(contract.contains("/api/v1/restaurants/search:\n    get:"));
        assertTrue(contract.contains("'200':\n          description: 查询成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/RestaurantPageSuccessResponse'"));

        assertTrue(contract.contains("/api/v1/recommendations/random:\n    get:"));
        assertTrue(contract.contains("'200':\n          description: 推荐成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/RecommendationResultSuccessResponse'"));

        assertTrue(contract.contains("/api/v1/recommendations/cards:\n    get:"));
        assertTrue(contract.contains("'200':\n          description: 获取成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/RecommendationCardListSuccessResponse'"));

        assertTrue(contract.contains("operationId: createBlacklist"));
        assertTrue(contract.contains("operationId: listBlacklist"));
        assertTrue(contract.contains("'200':\n          description: 查询成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/BlacklistPageSuccessResponse'"));

        assertTrue(contract.contains("operationId: listNotes"));
        assertTrue(contract.contains("'200':\n          description: 查询成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/NotePageSuccessResponse'"));

        assertTrue(contract.contains("operationId: getNoteDetail"));
        assertTrue(contract.contains("'200':\n          description: 查询成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/NoteDetailSuccessResponse'"));

        assertTrue(contract.contains("operationId: updateNote"));
        assertTrue(contract.contains("'200':\n          description: 更新成功\n          content:\n            application/json:\n              schema:\n                $ref: '#/components/schemas/NoteDetailSuccessResponse'"));
    }

    @Test
    void requestConstraintsAndNotFoundResponsesShouldMatchImplementedBehavior() {
        assertTrue(contract.contains("WechatLoginRequest:\n      type: object"));
        assertTrue(contract.contains("code:\n          type: string\n          minLength: 10\n          pattern: ^mock-code-.*"));

        assertTrue(contract.contains("summary: 按关键词搜索餐厅"));
        assertTrue(contract.contains("description: 搜索关键词（必填，且不能是空白字符串）"));
        assertTrue(contract.contains("pattern: .*(\\S|[^\\s]).*"));

        assertTrue(contract.contains("CreateBlacklistRequest:\n      type: object"));
        assertTrue(contract.contains("poiId:\n          type: string\n          minLength: 1\n          maxLength: 64\n          pattern: .*(\\S|[^\\s]).*"));
        assertTrue(contract.contains("CreateNoteRequest:\n      type: object"));

        assertTrue(contract.contains("'404':\n          $ref: '#/components/responses/AmapNoResultNotFound'"));
        assertTrue(contract.contains("'404':\n          $ref: '#/components/responses/UserOrAmapNoResultNotFound'"));
        assertTrue(contract.contains("'404':\n          $ref: '#/components/responses/BlacklistNotFound'"));
        assertTrue(contract.contains("'404':\n          $ref: '#/components/responses/NoteNotFound'"));

        assertTrue(contract.contains("AmapNoResultNotFound:"));
        assertTrue(contract.contains("code: 3003"));
        assertTrue(contract.contains("BlacklistNotFound:"));
        assertTrue(contract.contains("code: 2002"));
        assertTrue(contract.contains("NoteNotFound:"));
    }

    @Test
    void queryParameterDocsShouldDescribeConcreteValuesInsteadOfEmptyPlaceholders() {
        assertTrue(contract.contains("UserId:\n      name: userId\n      in: path\n      required: true"));
        assertTrue(contract.contains("minimum: 1"));
        assertTrue(contract.contains("description: 必填；用户主键 ID。请先调用 /api/v1/auth/me 获取当前登录用户的 id，再将该值填入路径中；不能留空。"));
        assertTrue(contract.contains("example: 1"));

        assertTrue(contract.contains("OptionalUserId:\n      name: userId\n      in: query\n      required: false"));
        assertTrue(contract.contains("description: 可选；传入后会按该用户黑名单过滤结果。必须为正整数且对应用户必须存在；不需要过滤时请省略该参数，不要传空值。"));

        assertTrue(contract.contains("Longitude:\n      name: longitude\n      in: query\n      required: true"));
        assertTrue(contract.contains("description: 必填；必须传具体经度数值，不能只传参数名或传空值。"));
        assertTrue(contract.contains("example: 120.35"));

        assertTrue(contract.contains("Latitude:\n      name: latitude\n      in: query\n      required: true"));
        assertTrue(contract.contains("description: 必填；必须传具体纬度数值，不能只传参数名或传空值。"));
        assertTrue(contract.contains("example: 30.31"));
    }
}
