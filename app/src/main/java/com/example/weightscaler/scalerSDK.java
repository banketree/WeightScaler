package com.example.weightscaler;

import java.util.ArrayList;

import android.content.Context;

import com.example.weightscaler.WeightScale.BTN_TYPE;

public class scalerSDK {
    private Context _context;
    private WeightScale scale;

    public scalerSDK(Context context, IGetBLEScaleDevice getBLEScaleDevice) {
        this.scale = new WeightScale(this._context, getBLEScaleDevice);
        this._context = context;
    }

    public ArrayList<ScaleDevice> getDevicelist() {
        return this.scale.m_ScaleDevice();
    }

    public boolean bleIsEnabled() {
        return this.scale.bluetoothIsEnabled();
    }

    public void bleEnabled() {
        this.scale.bluetoothEnabled();
    }


    public void Scan(boolean enable) {
        if (enable == true) {
            this.scale.scanDevice(true);
        } else {
            this.scale.scanDevice(false);
        }
    }

    public int getState() {
        int i;
        if (scale.connectdate == false) i = 0;
        else
            i = 1;
        return i;
    }

    public void bleSend(ScaleDevice scaledevice, byte type) {
        if (type == 1)
            scale.connectDevice(scaledevice, BTN_TYPE.BTN_ZERO);
        if (type == 2)
            scale.connectDevice(scaledevice, BTN_TYPE.BTN_CALIIB);
    }

    public void updatelist() {
        this.scale.updataDevicelist();
    }
}
