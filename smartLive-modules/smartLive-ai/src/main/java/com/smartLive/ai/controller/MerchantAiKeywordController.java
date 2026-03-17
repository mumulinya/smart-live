package com.smartLive.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/merchant")
public class MerchantAiKeywordController {

    private final ChatClient chatClient;

    public MerchantAiKeywordController(@Qualifier("keywordChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/keywords")
    public List<String> extractKeywords(@RequestBody List<String> contents) {
        List<String> normalizedContents = contents == null
                ? new ArrayList<>()
                : contents.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
        if (normalizedContents.isEmpty()) {
            return new ArrayList<>();
        }
        String prompt = """
                Extract up to 5 core issue keywords from the bad reviews below.
                Return keywords only, separated by commas, with no numbering or explanation.

                Bad reviews:
                %s
                """.formatted(String.join("\n", normalizedContents));
        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        return normalizeKeywords(result);
    }

    private List<String> normalizeKeywords(String result) {
        if (result == null || result.isBlank()) {
            return new ArrayList<>();
        }
        String cleaned = result.replace('\uFF1A', ',')
                .replace(':', ',')
                .replace('\uFF1B', ',')
                .replace(';', ',')
                .replace('\u3001', ',')
                .replace('\uFF0C', ',')
                .replace("\r", ",")
                .replace("\n", ",");
        String[] parts = cleaned.split(",");
        Set<String> keywords = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String keyword = part.trim()
                    .replaceAll("^[0-9.\\-\\s]+", "")
                    .replaceAll("^[,\\uFF0C]+", "")
                    .replaceAll("[,\\uFF0C\\u3002.!?\\uFF01\\uFF1F]+$", "");
            if (!keyword.isBlank()) {
                keywords.add(keyword);
            }
            if (keywords.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }
}
