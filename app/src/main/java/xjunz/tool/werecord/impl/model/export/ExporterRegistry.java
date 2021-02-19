/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.export;

import java.lang.ref.WeakReference;

/**
 * 本质上就是一个用于数据共享的工具类，用于在不同Activity之间共享{@link Exporter}
 *
 * @author xjunz 2021/1/29 22:55
 */
public class ExporterRegistry {
    private WeakReference<Exporter> mRegisteredExporter;
    private static ExporterRegistry sInstance;

    public synchronized static ExporterRegistry getInstance() {
        return sInstance == null ? sInstance = new ExporterRegistry() : sInstance;
    }

    public void register(Exporter exporter) {
        if (mRegisteredExporter != null) {
            mRegisteredExporter.clear();
        }
        mRegisteredExporter = new WeakReference<>(exporter);
    }

    public Exporter obtain() {
        return mRegisteredExporter.get();
    }
}
