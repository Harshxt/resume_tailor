package one.harshit.resumeTailor.config;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean 
    public ChatClient fastChatClient(Map<String, ChatModel> chatModels,
            @Value("${app.ai.fast.provider:googleGenAiChatModel}") String providerBeanName,
            @Value("${app.ai.fast.model}") String modelName) {

        ChatModel model = chatModels.get(providerBeanName);
        if (model == null) {
            throw new IllegalArgumentException("No ChatModel bean found with name: " + providerBeanName
                    + ". Available beans: " + chatModels.keySet());
        }

        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder().model(modelName))
                .defaultSystem("You are a concise, fast classification engine")
                .build();

    }
    
    @Bean 
     public ChatClient reasoningChatClient(Map<String, ChatModel> chatModels,
            @Value("${app.ai.reasoning.provider:googleGenAiChatModel}") String providerBeanName,
            @Value("${app.ai.reasoning.model}") String modelName) {

        ChatModel model = chatModels.get(providerBeanName);
        if (model == null) {
            throw new IllegalArgumentException("No ChatModel bean found with name: " + providerBeanName
                    + ". Available beans: " + chatModels.keySet());
        }

         return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder()
                        .model(modelName)
                        .temperature(0.7))
                .build();

    }

}
