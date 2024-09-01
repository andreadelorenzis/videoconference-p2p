package com.andreadelorenzis.videoconferencep2p.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.andreadelorenzis.videoconferencep2p.controller.SignalingHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registra il SignalingHandler per l'endpoint /ws
        registry.addHandler(new SignalingHandler(), "/ws").setAllowedOrigins("*");
    }
}

