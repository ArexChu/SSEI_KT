package com.ecust.equsys.utils;

import com.android.uhflibs.as3992_native;

import java.io.IOException;

/**
 * Created by ares on 2016/10/31.
 */

public class RFIDReader {
    private static as3992_native native_method;
    private static DeviceControl DevCtrl;
    private static ReadThread rthread;

    public static as3992_native getNative() {
        if (native_method==null){
            native_method = new as3992_native();
        }
        return native_method;
    }
    public static DeviceControl getDevCtrl() {
        if (DevCtrl==null){
            try {
                DevCtrl = new DeviceControl("/sys/class/misc/mtgpio/pin");
            } catch (IOException e){
            }
        }
        return DevCtrl;
    }
    public static ReadThread getReadThread() {
        if (rthread==null){
            rthread = new ReadThread();
        }
        return rthread;
    }


}
