package vn.edu.iuh.fit.configs;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Bean(name = "wsBrokerTaskScheduler")
    public TaskScheduler wsBrokerTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000")
                .withSockJS();
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client subscribe các topic dưới đây
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(wsBrokerTaskScheduler())
                .setHeartbeatValue(new long[]{10000, 10000});
        // Client gửi message lên BE theo prefix này (sẽ dùng cho chat sau)
        config.setApplicationDestinationPrefixes("/app");
        // Gửi riêng từng user: convertAndSendToUser -> /user/{id}/queue/...
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
