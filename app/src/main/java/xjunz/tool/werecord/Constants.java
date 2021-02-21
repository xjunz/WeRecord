/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

/**
 * @author xjunz 2021/2/16 23:24
 */
public class Constants {
    public static final String URL_APP_DOWNLOAD_PAGE = "http://d.firim.top/werecord/";
    public static boolean USER_DEBUGGABLE = BuildConfig.DEBUG;
    private static final String FEEDBACK_QQ_GROUP_NUMBER = "602611929";
    private static final String FEEDBACK_TEMP_QQ_CHAT_NUMBER = "3285680362";
    private static final String FEEDBACK_EMAIL = "3285680362@qq.com";
    public static final String URI_LAUNCH_QQ_TEMP_CHAT = "mqqwpa://im/chat?chat_type=wpa&uin=" + FEEDBACK_TEMP_QQ_CHAT_NUMBER;
    public static final String URI_JOIN_QQ_FEEDBACK_QQ_GROUP = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + FEEDBACK_QQ_GROUP_NUMBER + "&card_type=group&source=qrcode";
    public static final String URI_DONATE_ALIPAY = "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/fkx154567xmljmwmkmchc65?t=1606376518084";
    public static final String URI_FEEDBACK_EMAIL = "mailto:" + FEEDBACK_EMAIL;
}
