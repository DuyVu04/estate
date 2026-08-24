package com.project.estate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Chat Service sử dụng Spring AI ChatClient.
 *
 * <p>Spring AI tự động cấu hình OpenAI-compatible ChatModel dựa trên spring.ai.openai.* trong
 * application-dev.yml (base-url trỏ sang Gemini OpenAI-compatible endpoint).
 */
@Service
@Slf4j
public class GeminiChatService {

  private final ChatClient chatClient;

  /**
   * Inject ChatClient.Builder do Spring AI auto-configure. Builder pattern cho phép cấu hình thêm
   * defaultSystem, tools, advisors,...
   */
  public GeminiChatService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  /**
   * Sinh câu trả lời AI dựa trên system prompt và câu hỏi của người dùng.
   *
   * @param systemPrompt chứa context RAG (danh sách BĐS + quy tắc)
   * @param userQuestion câu hỏi nguyên văn của khách hàng
   * @return câu trả lời từ Gemini qua Spring AI ChatClient
   */
  public String generateAnswer(String systemPrompt, String userQuestion) {
    try {
      String answer = chatClient.prompt().system(systemPrompt).user(userQuestion).call().content();

      log.info("[SPRING_AI_CHAT] Successfully generated answer via ChatClient");
      return answer;
    } catch (Exception e) {
      log.error("[SPRING_AI_CHAT] Error calling ChatClient: {}", e.getMessage());
      return "Dạ chào bạn, hệ thống AI MyEstate đã tìm thấy các bất động sản phù hợp nhất"
          + " với nhu cầu của bạn ở danh sách đính kèm phía dưới. Bạn hãy xem chi tiết từng căn nhé!";
    }
  }
}
