package com.zxr;

import com.zxr.common.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    // 测试1：没有带 Token，访问任务接口应该被拒绝 (401)
    @Test
    public void testAccessWithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/agent/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputText\":\"select 1\"}"))
                .andExpect(status().isUnauthorized()); // 期望返回 401
    }

    // 测试2：越权访问（假设 taskId=999 属于别人，你拿自己的 token 查）
    @Test
    public void testAccessOtherUserTask_ShouldReturnError() throws Exception {
        // 生成一个测试用的 Token (假设 userId 是 999)
        String token = jwtUtil.createToken(999L);

        mockMvc.perform(get("/api/agent/task/1/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // 检查返回的 JSON 里是否有 "access denied"
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    assert responseBody.contains("access denied") || responseBody.contains("not found");
                });
    }
}