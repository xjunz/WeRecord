/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import xjunz.tool.werecord.util.Utils;


public class MessageParser {
    private static final String TEST_XML = "";

    public static void parseJoinGroupMessage(String raw) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        long start = System.currentTimeMillis();
        try {
            SAXParser saxParser = factory.newSAXParser();
            JoinGroupMessageHandler handler = new JoinGroupMessageHandler();
            saxParser.parse(new ByteArrayInputStream(TEST_XML.getBytes()), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - start));
    }

    private static class JoinGroupMessageHandler extends DefaultHandler {
        private boolean inTemplate;
        private boolean inNickname;
        private String template;
        private String currentPattern;
        private List<String> patterns = new ArrayList<>();
        private HashMap<String, String> matchedMap;


        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            switch (qName) {
                case "template":
                    inTemplate = true;
                    break;
                case "link":
                    currentPattern = attributes.getValue("name");
                    break;
                case "nickname":
                    inNickname = true;
                default:
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            switch (qName) {
                case "template":
                    inTemplate = false;
                    break;
                case "nickname":
                    inNickname = false;
                default:
                    break;
            }
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            for (String pattern : patterns) {
                template = template.replace("$" + pattern + "$", Objects.requireNonNull(matchedMap.get(pattern)));
            }
            System.out.println(template);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            String text = new String(ch, start, length).trim();
            if (inTemplate) {
                template = text;
                patterns = Utils.extract(text, "\\$(.+k ?)\\$");
                matchedMap = new HashMap<>();
            } else if (inNickname) {
                String matched = matchedMap.get(currentPattern);
                if (matched == null) {
                    matched = text;
                } else {
                    matched = matched + "、" + text;
                }
                matchedMap.put(currentPattern, matched);
            }
        }
    }
}