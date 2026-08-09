package vn.edu.iuh.nhom01_www.chatbot.dto;

import lombok.Data;

@Data
public class ChatbotRequest {
    @jakarta.validation.constraints.NotBlank
    private String prompt;
}
