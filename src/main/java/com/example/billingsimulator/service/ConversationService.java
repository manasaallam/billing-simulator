package com.example.billingsimulator.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    private final Map<String, String> conversations = new ConcurrentHashMap<>();

    public void save(String conversationId, String question) {

        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        conversations.put(conversationId, question);
    }

    public String get(String conversationId) {

        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }

        return conversations.get(conversationId);
    }

    public void remove(String conversationId) {

        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        conversations.remove(conversationId);
    }

    public boolean exists(String conversationId) {

        return conversationId != null &&
                conversations.containsKey(conversationId);
    }
}