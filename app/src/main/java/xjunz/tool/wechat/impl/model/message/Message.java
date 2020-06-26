package xjunz.tool.wechat.impl.model.message;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.Nullable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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

    public Message(String raw) {
        if (raw.startsWith("wxid_")) {
            int spilt = raw.indexOf(":");
            this.groupTalkerId = raw.substring(0, spilt);
            this.rawContent = raw.substring(spilt + 2);
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

    /**
     * @return 发送者ID，如果发送者为自己，返回null
     */
    public @Nullable
    String getSenderId() {
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


    private String match(final String key) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        final String[] value = new String[1];
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new ByteArrayInputStream(key.getBytes()), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    super.startElement(uri, localName, qName, attributes);
                    if (qName.equals(key)) {
                        value[0] = attributes.getValue(qName);
                    }
                }
            });
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return value[0];
    }

 /*   String match(String key) {
        if (getRawContent().contains(key + "><![CDATA[")) {
            return UniUtils.extractFirst(getRawContent(), key + "><\\!\\[CDATA\\[(.+?)\\]\\]><");
        } else {
            return UniUtils.extractFirst(getRawContent(), key + ">(.+?)<");
        }
    }*/

    String rudeMatch(String key) {
        return UniUtils.extractFirst(getRawContent(), key + ">(.+?)<");
    }

    String simpleMatch(String key) {
        return UniUtils.extractFirst(getRawContent(), key + "=\"(.+?)\"");
    }

}
