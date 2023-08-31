package com.Teletubbies.Apollo.monitor.component;

import com.Teletubbies.Apollo.deploy.domain.ApolloDeployService;
import com.Teletubbies.Apollo.deploy.repository.AwsServiceRepository;
import com.Teletubbies.Apollo.monitor.exception.MonitorException;
import com.Teletubbies.Apollo.monitor.service.CloudWatchService;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class ApolloWebSocketHandler extends TextWebSocketHandler {
    private final CloudWatchService cloudWatchService;
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String[] params = session.getUri().getQuery().split("&");
        Map<String, String> value = new HashMap<>();
        for (String param: params) {
            String[] keyValue = param.split("=");
            if (keyValue.length > 1) {
                value.put(keyValue[0], keyValue[1]);
            }
        }
        String userId = value.get("userId");
        String serviceId = value.get("serviceId");
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("serviceId", serviceId);
        sessions.add(session);
    }

    @Scheduled(fixedRate = 5000)
    public void sendDataToAll() throws IOException {
        for (WebSocketSession session: sessions) {
            if (session.isOpen()) {
                String userId = (String) session.getAttributes().get("userId");
                String dashboardBody = cloudWatchService.getCloudWatchMetrics("AWS/ECS", Long.parseLong(userId));
                session.sendMessage(new TextMessage(dashboardBody));
            }
        }
    }
}
