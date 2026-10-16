package com.service.lacasita.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.service.lacasita.utils.*;

import org.springframework.messaging.Message;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.entity.UserModel;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private JwtUtil jwtService;

    @Autowired
    private UserRespository userRepository;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                String authHeader = accessor.getFirstNativeHeader("Authorization");

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new MessagingException("MISSING_TOKEN");
                }

                String token = authHeader.substring(7);

                if (!jwtService.isTokenValid(token)) {
                    log.debug("CONNECT rechazado: token expirado o inválido");
                    throw new MessagingException("TOKEN_EXPIRED");
                }

                // Un refresh token no debe abrir una sesión de WebSocket.
                if (!"access".equals(jwtService.getTokenType(token))) {
                    throw new MessagingException("INVALID_TOKEN_TYPE");
                }

                String username = jwtService.getUsernameFromToken(token);

                UserModel user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new MessagingException("USER_NOT_FOUND"));

                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        String.valueOf(user.getId()), null, List.of()));

                return message;
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

    }

}
