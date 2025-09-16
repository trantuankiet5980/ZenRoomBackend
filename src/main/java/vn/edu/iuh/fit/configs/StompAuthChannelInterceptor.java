package vn.edu.iuh.fit.configs;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.security.Principal;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwt;

    public StompAuthChannelInterceptor(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        var accessor = StompHeaderAccessor.wrap(message);
        if ("CONNECT".equals(accessor.getCommand()!=null?accessor.getCommand().name():null)) {
            String auth = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7).trim();
                if (jwt.validateToken(token)) {
                    String userId = jwt.extractUserId(token);
                    accessor.setUser(new Principal() { @Override public String getName() { return userId; }});
                }
            }
        }
        return message;
    }
}
