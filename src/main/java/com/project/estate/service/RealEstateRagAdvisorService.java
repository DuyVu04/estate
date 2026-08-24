package com.project.estate.service;

import com.project.estate.dto.response.PropertySemanticResponse;
import com.project.estate.dto.response.RealEstateAdvisorResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealEstateRagAdvisorService {

  private final PropertyAiService propertyAiService;
  private final GeminiChatService geminiChatService;

  public RealEstateAdvisorResponse advise(String userQuestion) {
    log.info("[RAG_ADVISOR] Processing customer query: {}", userQuestion);

    List<PropertySemanticResponse> retrievedProperties =
        propertyAiService.searchSemantic(userQuestion, 3);

    String systemPrompt = buildSystemPrompt(retrievedProperties);
    String answer = geminiChatService.generateAnswer(systemPrompt, userQuestion);

    return RealEstateAdvisorResponse.builder()
        .answer(answer)
        .recommendedProperties(retrievedProperties)
        .totalFound(retrievedProperties.size())
        .build();
  }

  private String buildSystemPrompt(List<PropertySemanticResponse> properties) {
    StringBuilder contextBuilder = new StringBuilder();
    for (int i = 0; i < properties.size(); i++) {
      PropertySemanticResponse p = properties.get(i);
      contextBuilder.append(
          String.format(
              "%d. [%s] - Giá: %,.0f VNĐ - Diện tích: %.1f m2 - Địa chỉ: %s, %s, %s, %s - Mô tả: %s (Độ khớp: %.1f%%)%n",
              i + 1,
              p.title(),
              p.price(),
              p.area(),
              p.address(),
              p.ward(),
              p.district(),
              p.city(),
              p.description(),
              p.matchPercentage() != null ? p.matchPercentage() : 0.0));
    }

    return String.format(
        """
        [VAI TRÒ]:
        Bạn là Chuyên viên Tư vấn Bất Động Sản AI cao cấp của sàn giao dịch MyEstate.
        Nhiệm vụ của bạn là tư vấn tận tâm, lịch thiệp và trung thực cho khách hàng dựa DUY NHẤT vào [DANH SÁCH BẤT ĐỘNG SẢN] được cung cấp dưới đây.

        [QUY TẮC BẮT BUỘC]:
        1. CHỈ SỬ DỤNG DỮ LIỆU CÓ SẴN: Tuyệt đối KHÔNG tự sáng tác giá bán, địa chỉ, diện tích hoặc tiện ích không xuất hiện trong danh sách.
        2. NÊU RÕ LÝ DO: Khi gợi ý căn nhà, hãy phân tích cụ thể các ưu điểm giải quyết đúng nhu cầu khách đang tìm kiếm.
        3. KHI KHÔNG CÓ KẾT QUẢ KHỚP: Nếu khách hỏi về địa điểm hoặc mức giá mà hệ thống chưa có, hãy nhã nhặn thông báo và gợi ý phương án gần nhất có trong danh sách.
        4. TỐI ƯU ĐỘ DÀI: Trình bày cô đọng trong 3-4 gạch đầu dòng (dưới 200 từ), tránh viết dài dòng lan man.

        [DANH SÁCH BẤT ĐỘNG SẢN]:
        %s
        """,
        contextBuilder);
  }
}
