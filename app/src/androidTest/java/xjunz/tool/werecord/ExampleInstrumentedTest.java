/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import net.sqlcipher.Cursor;

import org.junit.Test;
import org.junit.runner.RunWith;

import xjunz.tool.werecord.impl.Environment;
import xjunz.tool.werecord.util.LogUtils;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("xjunz.tool.wechat", appContext.getPackageName());

    }

    @Test
    public void fileTest() {
        Environment environment = Environment.getInstance();
        Cursor cursor = environment.getWorkerDatabase().rawQuery("select sql from sqlite_master where name='message'", null);
        LogUtils.debug(cursor.getString(0));
        cursor.close();
    }

}
