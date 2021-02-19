/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import xjunz.tool.werecord.impl.model.account.Contact;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.impl.repo.RepositoryFactory;
import xjunz.tool.werecord.util.Utils;

import static xjunz.tool.werecord.impl.model.message.MessageFactory.TYPE_SYSTEM_JOIN_GROUP;
import static xjunz.tool.werecord.impl.model.message.MessageFactory.TYPE_SYSTEM_PAT;

/**
 * 系统消息类
 */
public class SystemMessage extends Message {
    private Spanned html;

    public SystemMessage(ContentValues values) {
        super(values, MessageFactory.Type.SYSTEM);
    }

    @Override
    protected boolean needParseSenderId() {
        return getRawType() == TYPE_SYSTEM_JOIN_GROUP;
    }

    private Spanned getHtml() {
        return html = html == null ? HtmlCompat.fromHtml(escapeTag().replace("\n", "<br>"),HtmlCompat.FROM_HTML_MODE_COMPACT) : html;
    }

    /**
     * 去除img TAG，因为我们没必要显示系统消息里的图片
     * 去除scene TAG，某些消息里的场景TAG
     *
     * @return 去除了img TAG 的内容
     */
    @NotNull
    private String escapeTag() {
        //我们仅做一个简单的替换
        return content.replace("<img", "<img_escaped").replaceAll("<scene>.*?</scene>", "");
    }

    @NonNull
    @Override
    public String getParsedContent() {
        if (parsedContent == null) {
            switch (getRawType()) {
                case TYPE_SYSTEM_JOIN_GROUP:
                    parseJoinGroupMessage();
                    break;
                case TYPE_SYSTEM_PAT:
                    parsePatMessage();
                    break;
                default:
                    parsedContent = getHtml().toString();
                    break;
            }
        }
        return parsedContent;
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        if (spannedContent == null) {
            switch (getRawType()) {
                case TYPE_SYSTEM_JOIN_GROUP:
                case TYPE_SYSTEM_PAT:
                    spannedContent = getParsedContent();
                    break;
                default:
                    spannedContent = getHtml();
                    break;
            }
        }
        return spannedContent;
    }


    private void parseJoinGroupMessage() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            JoinGroupMessageHandler handler = new JoinGroupMessageHandler();
            parser.parse(new ByteArrayInputStream(content.getBytes()), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private void parsePatMessage() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            PatMessageHandler handler = new PatMessageHandler();
            parser.parse(new ByteArrayInputStream(content.getBytes()), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }


    private class JoinGroupMessageHandler extends DefaultHandler {
        private boolean inTemplate;
        private boolean inTarget;
        private boolean inLink;
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
                    inLink = true;
                    currentPattern = attributes.getValue("name");
                    break;
                case "plain":
                case "nickname":
                    inTarget = true;
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
                case "link":
                    inLink = false;
                    break;
                case "plain":
                case "nickname":
                    inTarget = false;
                    break;
            }
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            for (String pattern : patterns) {
                String matched = matchedMap.get(pattern);
                if (matched == null) {
                    matched = "";
                }
                template = template.replace("$" + pattern + "$", matched);
            }
            parsedContent = template;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            String text = new String(ch, start, length);
            if (text.length() == 0) {
                text = "";
            }
            if (inTemplate) {
                template = text;
                patterns = Utils.extract(text, "\\$(.+?)\\$");
                matchedMap = new HashMap<>();
            } else if (inLink && inTarget) {
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

    private class PatMessageHandler extends DefaultHandler {
        private boolean inTemplate;
        private final StringBuilder message;
        private final ContactRepository repo;

        public PatMessageHandler() {
            super();
            message = new StringBuilder();
            repo = RepositoryFactory.get(ContactRepository.class);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if ("template".equals(qName)) {
                inTemplate = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if ("template".equals(qName)) {
                inTemplate = false;
            }
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            parsedContent = message.toString();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            if (inTemplate) {
                String text = new String(ch, start, length);
                List<String> wxids = Utils.extract(text, "\\$\\{(.+?)\\}");
                for (String wxid : wxids) {
                    Contact contact = repo.get(wxid);
                    if (contact != null) {
                        text = text.replace("${" + wxid + "}", contact.getName());
                    }
                }
                if (message.length() == 0) {
                    message.append(text);
                } else {
                    message.append("\n").append(text);
                }
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.parsedContent);
    }

    protected SystemMessage(Parcel in) {
        super(in);
        this.parsedContent = in.readString();
    }

    public static final Creator<SystemMessage> CREATOR = new Creator<SystemMessage>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public SystemMessage createFromParcel(Parcel source) {
            return new SystemMessage(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public SystemMessage[] newArray(int size) {
            return new SystemMessage[size];
        }
    };

    @Override
    public String exportAsPlainText() {
        return String.format("[%s]\n%s", getType().getCaption(), getParsedContent());
    }
}
