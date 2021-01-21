/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.wechat.impl.model.message;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.format.Formatter;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.impl.repo.RepositoryFactory;
import xjunz.tool.wechat.impl.repo.WxAppRepository;

/**
 * AppMessage是可以在"AppMessage"表中查询到记录的消息类型。
 */
public class AppMessage extends ComplexMessage {
    private String mTitle;
    private String mDes;
    private String mAppId;
    private String mAppName;
    private int mSubtype;
    public static final int PARSE_ERROR_APP_XML = 3;

    public AppMessage(ContentValues values, MessageFactory.Type superType) {
        super(values, superType);
        parseMessage();
    }

    public int getRawSubtype() {
        return mSubtype;
    }

    @Override
    public void modifyContent(String content) {
        super.modifyContent(content);
        //每次修改content以后重新解析消息
        parseMessage();
    }

    @Nullable
    @Override
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    @Override
    public String getDescription() {
        return mDes;
    }

    @Nullable
    @Override
    public String getCaption() {
        return getSource();
    }

    @Nullable
    public String getSource() {
        return mAppName;
    }

    private void setRawSubtype(int rawSubtype) {
        mSubtype = rawSubtype;
        type = judgeSubtype(rawSubtype);
    }

    public String getAppId() {
        return mAppId;
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
                return MessageFactory.Type.SHARED_URL;
            case MessageFactory.SUBTYPE_NOTIFICATION:
                return MessageFactory.Type.NOTIFICATION;
            case MessageFactory.SUBTYPE_GAME:
            case MessageFactory.SUBTYPE_WCX:
                return MessageFactory.Type.WCX_SHARED;
            case MessageFactory.SUBTYPE_VIDEO:
                return MessageFactory.Type.VIDEO;
            default:
                return getType();
        }
    }

    private void parseMessage() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            ComplexMessageHandler handler = new ComplexMessageHandler();
            parser.parse(new ByteArrayInputStream(content.getBytes()), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (!(e instanceof ComplexMessageHandler.StopParseException)) {
                e.printStackTrace();
                this.parseErrorCode = PARSE_ERROR_APP_XML;
            }
        }
    }

    private class ComplexMessageHandler extends DefaultHandler {
        private String currentLocalName;
        private final StringBuilder title = new StringBuilder();
        private StringBuilder des = new StringBuilder();
        private final StringBuilder type = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            currentLocalName = qName;
            if (Objects.equals(localName, "appmsg")) {
                mAppId = attributes.getValue("appid");
                mAppName = RepositoryFactory.get(WxAppRepository.class).getNameOf(mAppId);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            switch (localName) {
                case "title":
                    mTitle = title.toString();
                    break;
                case "des":
                    mDes = des.toString();
                    break;
                case "totallen":
                    try {
                        mDes = Formatter.formatFileSize(App.getContext(), Long.parseLong(des.toString()));
                        throw new StopParseException();
                    } catch (NumberFormatException e) {
                        throw new StopParseException();
                    }
                case "type":
                    try {
                        setRawSubtype(Integer.parseInt(type.toString()));
                        //如果是文件的话，我们还要解析totallen节点，先不停止解析
                        if (mSubtype != MessageFactory.SUBTYPE_FILE) {
                            throw new StopParseException();
                        } else {
                            des = new StringBuilder();
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        throw new StopParseException();
                    }
                    break;
            }
        }

        /**
         * 手动抛出此异常停止解析XML
         */
        private class StopParseException extends SAXException {
            StopParseException() {
                super("Stop parsing manually.");
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            switch (currentLocalName) {
                case "title":
                    title.append(ch, start, length);
                    break;
                case "des":
                case "totallen":
                    des.append(ch, start, length);
                    break;
                case "type":
                    type.append(ch, start, length);
                    break;
            }
        }
    }

    ;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mTitle);
        dest.writeString(this.mDes);
        dest.writeString(this.mAppId);
        dest.writeString(this.mAppName);
        dest.writeInt(this.mSubtype);
    }

    protected AppMessage(Parcel in) {
        super(in);
        this.mTitle = in.readString();
        this.mDes = in.readString();
        this.mAppId = in.readString();
        this.mAppName = in.readString();
        this.mSubtype = in.readInt();
    }

    public static final Creator<AppMessage> CREATOR = new Creator<AppMessage>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public AppMessage createFromParcel(Parcel source) {
            return new AppMessage(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public AppMessage[] newArray(int size) {
            return new AppMessage[size];
        }
    };
}
