package com.smartLive.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 控制字符清理功能测试
 */
@SpringBootTest
public class ControlCharCleanTest {

    @Resource
    private ObjectMapper objectMapper;

    @Test
    public void testControlCharCleanDeserializer() throws JsonProcessingException {
        // 准备包含控制字符的JSON字符串
        String jsonWithControlChars = "{\n" +
                "  \"name\": \"正常文本\",\n" +
                "  \"description\": \"包含" + (char) 0x01 + "控制" + (char) 0x1F + "字符" + (char) 0x7F + "的文本\",\n" +
                "  \"content\": \"开头" + (char) 0x02 + "中间" + (char) 0x0B + "结尾" + (char) 0x0C + "\",\n" +
                "  \"normalField\": \"普通字段\"\n" +
                "}";

        System.out.println("原始JSON: " + jsonWithControlChars);

        // 使用配置的ObjectMapper反序列化
        TestData result = objectMapper.readValue(jsonWithControlChars, TestData.class);

        System.out.println("清理后结果:");
        System.out.println("name: " + result.getName());
        System.out.println("description: " + result.getDescription());
        System.out.println("content: " + result.getContent());
        System.out.println("normalField: " + result.getNormalField());

        // 验证控制字符被清理
        assertNotNull(result);
        assertEquals("正常文本", result.getName());
        assertEquals("包含控制字符的文本", result.getDescription()); // 控制字符应该被移除
        assertEquals("开头中间结尾", result.getContent()); // 控制字符应该被移除
        assertEquals("普通字段", result.getNormalField());

        // 验证长度
        assertEquals(5, result.getDescription().length()); // "包含控制字符的文本" 5个字符
        assertEquals(4, result.getContent().length()); // "开头中间结尾" 4个字符
    }

    @Test
    public void testNormalJson() throws JsonProcessingException {
        // 测试正常JSON
        String normalJson = "{\n" +
                "  \"name\": \"测试名称\",\n" +
                "  \"description\": \"这是一个正常的描述文本\",\n" +
                "  \"content\": \"正常内容\"\n" +
                "}";

        TestData result = objectMapper.readValue(normalJson, TestData.class);

        assertNotNull(result);
        assertEquals("测试名称", result.getName());
        assertEquals("这是一个正常的描述文本", result.getDescription());
        assertEquals("正常内容", result.getContent());
    }

    @Test
    public void testWithWhitespaceChars() throws JsonProcessingException {
        // 测试包含允许的空白字符（\r, \n, \t）
        String jsonWithWhitespace = "{\n" +
                "  \"name\": \"带换行\\n和制表符\\t的文本\",\n" +
                "  \"description\": \"第一行\\r\\n第二行\\t制表符\"\n" +
                "}";

        TestData result = objectMapper.readValue(jsonWithWhitespace, TestData.class);

        assertNotNull(result);
        assertTrue(result.getName().contains("\n"));
        assertTrue(result.getName().contains("\t"));
        assertTrue(result.getDescription().contains("\r\n"));
        assertTrue(result.getDescription().contains("\t"));
    }

    @Test
    public void testMapDeserialization() throws JsonProcessingException {
        // 测试Map反序列化
        String jsonWithControlChars = "{\n" +
                "  \"field1\": \"正常值\",\n" +
                "  \"field2\": \"包含" + (char) 0x05 + "控制字符\"\n" +
                "}";

        Map<String, Object> result = objectMapper.readValue(jsonWithControlChars, Map.class);

        assertNotNull(result);
        assertEquals("正常值", result.get("field1"));
        assertEquals("包含控制字符", result.get("field2")); // 控制字符被移除
    }

    @Test
    public void testNullAndEmpty() throws JsonProcessingException {
        // 测试null和空字符串
        String json = "{\n" +
                "  \"name\": null,\n" +
                "  \"description\": \"\",\n" +
                "  \"content\": \"   \"\n" +
                "}";

        TestData result = objectMapper.readValue(json, TestData.class);

        assertNotNull(result);
        assertNull(result.getName());
        assertEquals("", result.getDescription());
        assertEquals("   ", result.getContent());
    }

    /**
     * 测试数据类
     */
    public static class TestData {
        private String name;
        private String description;
        private String content;
        private String normalField;

        // getter and setter
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getNormalField() {
            return normalField;
        }

        public void setNormalField(String normalField) {
            this.normalField = normalField;
        }
    }
}