/*
 * Copyright (c) 2020 xjunz. 保留所有权利
 */

package xjunz.tool.wechat.impl.model.message;

import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import xjunz.tool.wechat.App;
import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.impl.model.account.User;
import xjunz.tool.wechat.util.UniUtils;

public class Message {
    private int msgId;
    private String rawContent;
    private String groupTalkerId;
    private String imgName;
    private String imgPath;
    private boolean isSend;
    private long createTimeStamp;
    private String talkerId;
    private String senderId;
    private List<String> contentUrls;
    private int rawType;
    private Type type;

    public enum Type {
        CALL(R.string.call),
        FILE(R.string.file, true),
        TRANSFER(R.string.transfer, true),
        LOCATION(R.string.location, true),
        HB(R.string.hongbao, true),
        EMOJI(R.string.emoji),
        REPOST(R.string.repost, true),
        RECOMMEND(R.string.recommend_friend),
        SYSTEM(R.string.system_msg),
        PLAIN(R.string.plain_text),
        WCX_SHARED(R.string.wcx, true),
        SHARED_URL(R.string.shared_url, true),
        PICTURE(R.string.picture),
        PUSH(R.string.push),
        VIDEO(R.string.video);
        String caption;
        boolean isComplex;

        Type(@StringRes int captionRes, boolean isComplex) {
            this.caption = App.getStringOf(captionRes);
            this.isComplex = isComplex;
        }

        Type(@StringRes int captionRes) {
            this.caption = App.getStringOf(captionRes);
            this.isComplex = false;
        }

        public boolean isComplex() {
            return isComplex;
        }

        public String getCaption() {
            return caption;
        }


    }

    public Message(@Nullable String raw, @NonNull String talkerId) {
        this.talkerId = talkerId;
        if (raw != null && talkerId.endsWith("@chatroom")) {
            if (raw.startsWith("wxid_")) {
                int spilt = raw.indexOf(":");
                this.groupTalkerId = raw.substring(0, spilt);
                this.rawContent = raw.substring(spilt + 2);
            } else {
                this.rawContent = raw;
            }
        } else {
            this.rawContent = raw;
        }
    }

    public int getMsgId() {
        return msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }


    @NonNull
    public String requireSenderId() {
        if (senderId == null) {
            if (groupTalkerId == null) {
                senderId = talkerId;
            } else {
                senderId = groupTalkerId;
            }
        }
        return senderId;
    }

    /**
     * @return 发送者ID，如果发送者为自己，返回null
     */
    @Nullable
    public String getSenderId() {
        if (isSend) {
            return null;
        }
        if (senderId == null) {
            if (groupTalkerId == null) {
                senderId = talkerId;
            } else {
                senderId = groupTalkerId;
            }
        }
        return senderId;
    }

    /**
     * @return 消息中所有的url
     */
    public List<String> getContentUrls() {
        if (contentUrls == null) {
            contentUrls = new ArrayList<>();
            Matcher matcher = Patterns.WEB_URL.matcher(rawContent);
            while (matcher.find()) {
                contentUrls.add(matcher.group());
            }
        }
        return contentUrls;
    }


    public String getImagePath() {
        User user = Environment.getInstance().getCurrentUser();
        if (imgPath == null) {
            if (!TextUtils.isEmpty(imgName)) {
                if (imgName.startsWith("T")) {
                    int index = imgName.lastIndexOf("_");
                    String md5 = imgName.substring(index + 1);
                    imgPath = user.imageCachePath + File.separator
                            + md5.substring(0, 2) + File.separator
                            + md5.substring(2, 4) + File.separator
                            + "th_" + md5;
                }
            }
        }
        return imgPath;
    }


    /**
     * @return 未经过加工的消息内容
     */
    public String getRawContent() {
        return rawContent;
    }


    public String getGroupTalkerId() {
        return groupTalkerId;
    }

    public boolean isSend() {
        return isSend;
    }


    public long getCreateTimeStamp() {
        return createTimeStamp;
    }

    public String getTalkerId() {
        return talkerId;
    }


    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public void setSend(boolean send) {
        isSend = send;
    }


    public void setCreateTimeStamp(long createTimeStamp) {
        this.createTimeStamp = createTimeStamp;
    }

    public void setTalkerId(String talkerId) {
        this.talkerId = talkerId;
    }


    private String xmlMatch(final String key) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        final String[] value = new String[1];
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new ByteArrayInputStream(getRawContent().getBytes()), new DefaultHandler() {
                boolean isType;

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    super.characters(ch, start, length);
                    if (isType) {
                        String content = new String(ch, start, length);
                        value[0] = content;
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);
                    isType = localName.equals(key);
                }
            });
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return value[0];
    }

    String match(String key) {
        String matched;
        if (getRawContent().contains(key + "><![CDATA[")) {
            matched = UniUtils.extractFirst(getRawContent(), key + "><\\!\\[CDATA\\[(.*?)\\]\\]></" + key);
        } else {
            matched = UniUtils.extractFirst(getRawContent(), key + ">(.*?)</" + key);
        }
        if (matched != null) {
            Pattern pattern = Pattern.compile("&#x(.*?);");
            Matcher matcher = pattern.matcher(matched);
            while (matcher.find()) {
                String ascii = String.valueOf((char) Integer.valueOf(Objects.requireNonNull(matcher.group(1)), 16).intValue());
                matched = matched.replace(matcher.group(), ascii);
            }
        }
        return matched;
    }

    String rudeMatch(String key) {
        return UniUtils.extractFirst(getRawContent(), key + ">(.+?)<");
    }

    String simpleMatch(String key) {
        return UniUtils.extractFirst(getRawContent(), key + "=\"(.+?)\"");
    }

    public int getRawType() {
        return rawType;
    }

    @Nullable
    public CharSequence getContent() {
        if (!type.isComplex) {
            if (type == Type.PLAIN) {
                return Html.fromHtml(getRawContent());
            } else if (type == Type.SYSTEM) {
                return Html.fromHtml(getRawContent());
            } else {
                return "<" + type.getCaption() + ">";
            }
        }
        return null;
    }

    private static final int TYPE_PLAIN_MSG = 1;
    private static final int TYPE_PICTURE = 3;
    private static final int TYPE_EMOJI = 47;
    private static final int TYPE_TRANSFER = 419430449;
    private static final int TYPE_HB = 436207665;
    private static final int TYPE_SHARE = 49;
    private static final int TYPE_CALL = 50;
    private static final int TYPE_RECOMMEND = 42;
    private static final int SUBTYPE_FILE = 6;
    private static final int SUBTYPE_WCX_URL = 33;
    private static final int SUBTYPE_URL = 5;
    private static final int SUBTYPE_REPOST = 19;
    private static final int SUBTYPE_WCX_VIDEO = 36;
    private static final int TYPE_SYSTEM = 10000;
    private static final int TYPE_PUSH = 285212721;
    private static final int TYPE_VIDEO = 43;
    private static final int TYPE_LOCATION = 48;


    private Type judgeType(int rawType) {
        switch (rawType) {
            case TYPE_PLAIN_MSG:
                return Type.PLAIN;
            case TYPE_PICTURE:
                return Type.PICTURE;
            case TYPE_EMOJI:
                return Type.EMOJI;
            case TYPE_TRANSFER:
                return Type.TRANSFER;
            case TYPE_HB:
                return Type.HB;
            case TYPE_CALL:
                return Type.CALL;
            case TYPE_VIDEO:
                return Type.VIDEO;
            case TYPE_RECOMMEND:
                return Type.RECOMMEND;
            case TYPE_LOCATION:
                return Type.LOCATION;
            case TYPE_PUSH:
                return Type.PUSH;
            case TYPE_SHARE:
                int subType = Integer.parseInt(match("type"));
                switch (subType) {
                    case SUBTYPE_FILE:
                        return Type.FILE;
                    case SUBTYPE_WCX_URL:
                    case SUBTYPE_WCX_VIDEO:
                        return Type.WCX_SHARED;
                    case SUBTYPE_URL:
                        return Type.SHARED_URL;
                    case SUBTYPE_REPOST:
                        return Type.REPOST;
                }
                return null;
            case TYPE_SYSTEM:
                return Type.SYSTEM;
        }
        return null;
    }

    public void setRawType(int rawType) {
        this.rawType = rawType;
        this.type = judgeType(rawType);
    }

    public Type getType() {
        return type;
    }

    private String title;
    private String description;

    public String getTitle() {
        if (title == null) {
            title = match("title");
        }
        return title;
    }

    public String getDescription() {
        if (description == null) {
            description = match("des");
        }
        return description;
    }
}
