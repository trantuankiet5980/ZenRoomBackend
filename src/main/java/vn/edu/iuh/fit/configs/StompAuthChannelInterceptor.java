// StompAuthChannelInterceptor.java
package vn.edu.iuh.fit.configs;

import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.security.Principal;
import java.util.Optional;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwt;

    public StompAuthChannelInterceptor(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        var acc = StompHeaderAccessor.wrap(message);
        if (acc.getCommand() == StompCommand.CONNECT) {
            // Lấy cả "Authorization" lẫn "authorization"
            String auth = Optional.ofNullable(acc.getFirstNativeHeader(HttpHeaders.AUTHORIZATION))
                    .orElseGet(() -> acc.getFirstNativeHeader("authorization"));

            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.startsWith("Bearer") ? auth.substring(6).trim() : auth.trim();
                try {
                    if (jwt.validateToken(token)) {
                        String userId = jwt.extractUserId(token);
                        acc.setUser(() -> userId); // set Principal
                        // (khuyến nghị) set luôn login header để dễ debug
                        acc.setLogin(userId);
                        System.out.println("[WS][CONNECT] Principal set to userId=" + userId);
                    } else {
                        System.out.println("[WS][CONNECT] JWT invalid");
                    }
                } catch (Exception e) {
                    System.out.println("[WS][CONNECT] JWT parse error: " + e.getMessage());
                }
            } else {
                System.out.println("[WS][CONNECT] Missing Authorization header");
            }
        }
        return message;
    }
    @Component
    public class WsEventsLogger implements ApplicationListener<AbstractSubProtocolEvent> {
        @Override
        public void onApplicationEvent(AbstractSubProtocolEvent event) {
            System.out.println("[WS][Event] " + event.getClass().getSimpleName());
        }
    }
}
