package com.ecust.equsys.utils;

import com.android.uhflibs.as3992_native;

/**
 * Created by ares on 2016/12/8.
 */

public class ReadThread extends Thread {
    private as3992_native native_method;
    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            native_method = RFIDReader.getNative();
            native_method.read_thread();
        }
    }
}
