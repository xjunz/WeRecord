/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat;

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

import xjunz.tool.wechat.util.UniUtils;


public class MessageParser {
    // private static final String TAG = xjunz.tool.wechat.impl.model.message.MessageParser.class.getName();

    private static final String TEST_XML =
            "<sysmsg type=\"sysmsgtemplate\">\n" +
                    "\t<sysmsgtemplate>\n" +
                    "\t\t<content_template type=\"tmpl_type_profile\">\n" +
                    "\t\t\t<plain><![CDATA[]]></plain>\n" +
                    "\t\t\t<template><![CDATA[\"$username$\"邀请\"$names$\"加入了群聊]]></template>\n" +
                    "\t\t\t<link_list>\n" +
                    "\t\t\t\t<link name=\"username\" type=\"link_profile\">\n" +
                    "\t\t\t\t\t<memberlist>\n" +
                    "\t\t\t\t\t\t<member>\n" +
                    "\t\t\t\t\t\t\t<username><![CDATA[wxid_wgabh1cqlq9u22]]></username>\n" +
                    "\t\t\t\t\t\t\t<nickname><![CDATA[张羽]]></nickname>\n" +
                    "\t\t\t\t\t\t</member>\n" +
                    "\t\t\t\t\t</memberlist>\n" +
                    "\t\t\t\t</link>\n" +
                    "\t\t\t\t<link name=\"names\" type=\"link_profile\">\n" +
                    "\t\t\t\t\t<memberlist>\n" +
                    "\t\t\t\t\t\t<member>\n" +
                    "\t\t\t\t\t\t\t<username><![CDATA[wxid_0zzj702m3xzc21]]></username>\n" +
                    "\t\t\t\t\t\t\t<nickname><![CDATA[\uD83C\uDF4F]]></nickname>\n" +
                    "\t\t\t\t\t\t</member>\n" +
                    "\t\t\t\t\t</memberlist>\n" +
                    "\t\t\t\t\t<separator><![CDATA[、]]></separator>\n" +
                    "\t\t\t\t</link>\n" +
                    "\t\t\t</link_list>\n" +
                    "\t\t</content_template>\n" +
                    "\t</sysmsgtemplate>\n" +
                    "</sysmsg>\n" +
                    "\n";

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
                patterns = UniUtils.extract(text, "\\$(.+k ?)\\$");
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