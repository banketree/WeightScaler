package com.example.weightscaler;

import java.util.ArrayList;

public interface IGetBLEScaleDevice {
    void onGetBluetoothDevice(ArrayList<ScaleDevice> scaleDevices);

    void onBluetoothState(int state);
}
