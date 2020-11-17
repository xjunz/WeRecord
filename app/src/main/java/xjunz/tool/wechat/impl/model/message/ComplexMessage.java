/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import xjunz.tool.wechat.impl.repo.ComplexMessageRepository;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;

public class ComplexMessage extends Message {
    /**
     * 消息显示的样式
     * 0：普通显示
     * 1：横向全屏显示
     */
    private int showType;
    private String title, description, parsedContent;
    /**
     * 是否已填充，即是否从数据库中查询过此消息，懒加载的标识
     *
     * @see ComplexMessageRepository#fulfillComplexMessage(ComplexMessage)
     */
    private boolean hasFulfilled;
    /**
     * 是否成功从数据库中查询到此消息，未成功查询到的消息会执行内容解析
     */
    private boolean hasFound;
    /**
     * 是否已解析，懒解析的标识
     */
    private boolean hasParsed;
    /**
     * 消息来源（如分享的源应用程序）
     */
    private String source;
    /**
     * 消息类型亚类，数据库的"type"字段查询到的值
     *
     * @see MessageFactory 的"SUBTYPE_"常量
     */
    private int rawSubtype = -1;
    /**
     * 消息类型亚类
     *
     * @see MessageFactory.Type
     */
    private MessageFactory.Type subtype;

    public ComplexMessage(ContentValues values) {
        super(values);
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }


    @NonNull
    public String getTitle() {
        fulfillUnless();
        parseUnless();
        return title;
    }

    @NonNull
    public String getDescription() {
        fulfillUnless();
        parseUnless();
        return description;
    }


    /**
     * 返回解析后的内容
     * <p>
     * {@link ComplexMessage#getTitle()}+换行+{@link ComplexMessage#getDescription()}
     * </p>
     *
     * @return 解析后的内容
     */
    @NonNull
    @Override
    public String getParsedContent() {
        if (parsedContent == null) {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(getTitle())) {
                sb.append(getTitle());
                if (!TextUtils.isEmpty(getDescription())) {
                    sb.append("\n").append(getDescription());
                }
            } else {
                if (!TextUtils.isEmpty(getDescription())) {
                    sb.append(getDescription());
                }
            }
            parsedContent = sb.toString();
        }
        return parsedContent;
    }

    @NonNull
    @Override
    public CharSequence getSpannedContent() {
        return getParsedContent();
    }

    public String getSource() {
        fulfillUnless();
        parseUnless();
        return source;
    }


    public void setSource(String source) {
        this.source = source;
    }


    public int getRawSubtype() {
        fulfillUnless();
        parseUnless();
        return rawSubtype;
    }

    private void setSubtype(MessageFactory.Type subtype) {
        this.subtype = subtype;
    }

    public void setRawSubtype(int rawSubtype) {
        this.rawSubtype = rawSubtype;
        setSubtype(judgeSubtype(rawSubtype));
    }


    @NonNull
    public MessageFactory.Type getSubtype() {
        fulfillUnless();
        parseUnless();
        return subtype;
    }

    private void fulfillUnless() {
        if (!hasFulfilled) {
            hasFound = RepositoryFactory.get(ComplexMessageRepository.class).fulfillComplexMessage(this);
            hasFulfilled = true;
        }
    }

    private void parseUnless() {
        if (!hasFound && !hasParsed) {
            parseMessage();
        }
    }

    @NonNull
    @CheckResult
    private MessageFactory.Type judgeSubtype(int rawSubtype) {
        switch (rawSubtype) {
            case MessageFactory.SUBTYPE_FILE:
                return MessageFactory.Type.FILE;
            case MessageFactory.SUBTYPE_HB:
                return MessageFactory.Type.HB;
            case MessageFactory.SUBTYPE_GIF:
                return MessageFactory.Type.GIF;
            case MessageFactory.SUBTYPE_IMAGE:
                return MessageFactory.Type.IMAGE;
            case MessageFactory.SUBTYPE_MUSIC:
                return MessageFactory.Type.MUSIC;
            case MessageFactory.SUBTYPE_POSITION_SHARE:
                return MessageFactory.Type.POSITION_SHARE;
            case MessageFactory.SUBTYPE_REPLY:
                return MessageFactory.Type.REPLY;
            case MessageFactory.SUBTYPE_REPOST:
                return MessageFactory.Type.REPOST;
            case MessageFactory.SUBTYPE_SHARE:
                return MessageFactory.Type.SHARE;
            case MessageFactory.SUBTYPE_TRANSFER:
                return MessageFactory.Type.TRANSFER;
            case MessageFactory.SUBTYPE_URL:
            case MessageFactory.SUBTYPE_1:
            case MessageFactory.SUBTYPE_46:
                return MessageFactory.Type.SHARED_URL;
            case MessageFactory.SUBTYPE_GAME:
            case MessageFactory.SUBTYPE_WCX:
                return MessageFactory.Type.WCX_SHARED;
            case MessageFactory.SUBTYPE_VIDEO:
                return MessageFactory.Type.VIDEO;
        }
        if (getType().isComplex()) {
            return getType();
        }
        return MessageFactory.Type.OTHERS;
    }

    private void parseMessage() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            ComplexMessageHandler handler = new ComplexMessageHandler();
            parser.parse(new ByteArrayInputStream(content.getBytes()), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            setSubtype(getType());
            e.printStackTrace();
        } finally {
            hasParsed = true;
        }
    }

    private class ComplexMessageHandler extends DefaultHandler {
        private String currentQName = "";
        private boolean firstTitle = true, firstDes = true, firstAppName = true, firstType = true, firstSourceDisplayName = true;
        private final StringBuilder mTitle = new StringBuilder();
        private final StringBuilder mDes = new StringBuilder();
        private String mSource;
        private int mType = -1;

        private void notifyNotFirst(@NotNull String qName) {
            switch (qName) {
                case "title":
                    firstTitle = false;
                    break;
                case "des":
                    firstDes = false;
                    break;
                case "type":
                    firstType = false;
                    break;
                case "appname":
                    firstAppName = false;
                    break;
                case "sourcedisplayname":
                    firstSourceDisplayName = false;
                    break;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            currentQName = qName;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            notifyNotFirst(qName);
            currentQName = "";
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            if (getMsgId() == 653) {
                Log.i("xjunz-", mTitle.toString());
            }
            setTitle(mTitle.toString());
            setDescription(mDes.toString());
            setRawSubtype(mType);
            setSource(mSource);
            // setSubtype(judgeSubtype(mType));
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            String text = new String(ch, start, length);
            if (firstTitle && "title".equals(currentQName)) {
                mTitle.append(text);
            } else if (firstDes && "des".equals(currentQName)) {
                mDes.append(text);
            } else if (firstType && "type".equals(currentQName)) {
                try {
                    this.mType = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (firstSourceDisplayName && "sourcedisplayname".equals(currentQName)) {
                if (TextUtils.isEmpty(mSource)) {
                    mSource = text;
                }
            } else if (firstAppName && "appname".equals(currentQName)) {
                if (TextUtils.isEmpty(mSource)) {
                    mSource = text;
                }
            }
        }
    }
}
