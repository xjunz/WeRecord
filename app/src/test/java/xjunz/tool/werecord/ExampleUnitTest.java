/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

import org.apaches.commons.codec.DecoderException;
import org.apaches.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xjunz.tool.werecord.util.ShellUtils;
import xjunz.tool.werecord.util.Utils;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String pref = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" +
                "<map>\n" +
                "    <set name=\"uin_set\">\n" +
                "        <string>-11249</string>\n" +
                "    </set>\n" +
                "</map>";
        Utils.extract(pref, ">([-]?\\d+)<");
    }

    @Test
    public void deHex() {
        try {
            System.out.println(new String(Hex.decodeHex("6361743a202f646174612f757365722f302f636f6d2e74656e63656e742e6d6d2f7368617265645f70726566732f636f6d2e74656e63656e742e6d6d5f707265666572656e6365735f6163636f756e745f7377697463682e786d6c3a204e6f20737563682066696c65206f72206469726563746f7279")));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testXmlParse() {
        MessageParser.parseJoinGroupMessage("");
    }

    @Test
    public void testRoot() throws ShellUtils.ShellException {
        String out = ShellUtils.cat("/data/user/0/com.tencent.mm/shared_prefs/app_brand_global_sp.xml");
    }

    @NotNull
    private String generateLineText(@NotNull String preLine, int curCount) {
        int preCount = (preLine.length() + 1) / 2;
        int diff = curCount - preCount;
        if (diff < 0) {
            return preLine.substring(0, preLine.length() + diff * 2);
        } else if (diff > 0) {
            StringBuilder append = new StringBuilder(preLine);
            for (int i = 1; i <= diff; i++) {
                append.append("\n").append(preCount + i);
            }
            return append.toString();
        } else {
            return preLine;
        }
    }

    @Test
    public void testLine() {
        System.out.println(generateLineText("1\n2\n3\n4\n5", 8));
    }

    @Test
    public void testString() {
        String wxid = "wxid_unknown";
        String raw = "wxid_unknown:<hello:wxid_unknown>";
        int index = raw.indexOf(':');
        String newStr = "wxid_new" + raw.substring(index + wxid.length());
        System.out.println(raw.substring(0, index));

    }

    private class A {
        String b = "xjunz";
        WeakReference<String> weakB = new WeakReference<>("xjunz");
    }

    @Test
    public void testEdition() {
        EditionProfile editionProfile = new EditionProfile();
        editionProfile.addAddition(1, 1);
        editionProfile.addAddition(5, 12);
        editionProfile.addAddition(3, 3);
        editionProfile.addDeletion(2, 10);
        editionProfile.addDeletion(21, 21);
        editionProfile.removeAddition(8, 10);
    }


    private static class EditionProfile {
        private List<Integer> deletionNodes;
        private List<Integer> additionNodes;

        private EditionProfile() {
            deletionNodes = new ArrayList<>();
            additionNodes = new ArrayList<>();
        }


        private void addDeletion(int start, int end) {
            int index = Collections.binarySearch(deletionNodes, start);
            int insertion = index >= 0 ? index : -(index + 1);
            deletionNodes.add(insertion, start);
            deletionNodes.add(insertion + 1, end);
        }

        private void addAddition(int start, int end) {
            int index = Collections.binarySearch(additionNodes, start);
            int insertion = index >= 0 ? index : -(index + 1);
            additionNodes.add(insertion, start);
            additionNodes.add(insertion + 1, end);
        }

        private void removeDeletion(int start, int end) {
            deletionNodes.remove(Integer.valueOf(start));
            deletionNodes.remove(Integer.valueOf(end));
        }

        private void removeAddition(int start, int end) {
            int startIndex = Collections.binarySearch(additionNodes, start);
            int endIndex = Collections.binarySearch(additionNodes, end);
            if (startIndex >= 0) {
                if (endIndex >= 0) {
                    additionNodes.remove(Integer.valueOf(start - 1));
                    additionNodes.remove(Integer.valueOf(end + 1));
                } else {
                    additionNodes.set(startIndex, end + 1);
                }
            } else {
                if (endIndex >= 0) {
                    additionNodes.set(endIndex, start - 1);
                } else {
                    int insertion = -(startIndex + 1);
                    additionNodes.add(insertion, start - 1);
                    additionNodes.add(insertion + 1, end + 1);
                }
            }
        }

    }
}