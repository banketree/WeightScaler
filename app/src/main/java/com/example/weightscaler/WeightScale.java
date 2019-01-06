package com.example.weightscaler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.weightscaler.MainActivity.TAG;

public class WeightScale {
    private Context context;
    public ArrayList<ScaleDevice> mScaleDevices;
    private BluetoothAdapter bluetoothAdapter;
    private float scalevalue = 0.0f;
    private float scalesumvalue = 0.0f;
    static boolean scanupdate = false;
    static boolean connectdate = false;
    private BleThread mBleThread;
    public IGetBLEScaleDevice getBleScaleDevice;

    public WeightScale(Context context, IGetBLEScaleDevice getBLEScaleDevice) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mScaleDevices = new ArrayList<ScaleDevice>();
        this.getBleScaleDevice = getBLEScaleDevice;
        setBluetoothState(0);
    }

    private void setBluetoothState(int state) {
        if (null != getBleScaleDevice) {
            getBleScaleDevice.onBluetoothState(state);
        }
    }

    public boolean bluetoothIsEnabled() {
        try {
            if (bluetoothAdapter == null) return false;
            if (!bluetoothAdapter.isEnabled()) return false;
            return true;
        } catch (Exception e) {
        }
        return false;
    }


    public void bluetoothEnabled() {
        try {
            if (bluetoothAdapter == null) return;
            bluetoothAdapter.enable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ScaleDevice> m_ScaleDevice() {
        return mScaleDevices;
    }

    public void scanDevice(boolean enable) {
        bluetoothAdapter.stopLeScan(leScanCallback);
        boolean result = false;
        if (enable) {
            result = bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }

        if (result) Log.i(TAG, "scanDevice: " + result);
    }

    public void updataDevicelist() {
        if (!this.mScaleDevices.isEmpty()) {
            for (int i = 0; i < this.mScaleDevices.size(); ++i) {
                if (System.currentTimeMillis() - ((Long) this.mScaleDevices.get(i).mtime).longValue() > 3000L) {
                    this.mScaleDevices.remove(i);
                }
            }
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (!scanupdate) {
                scanupdate = true;
                if (bytestoAnalysis(scanRecord) == true) {
                    ScaleDevice mscaleDevice = new ScaleDevice();
                    mscaleDevice.devicename = device.getName();
                    mscaleDevice.deviceaddr = device.getAddress();
                    if (mscaleDevice.devicename == null) {
                        mscaleDevice.devicename = "蓝牙秤(" + device.getAddress().substring(12, 14) + device.getAddress().substring(15, 17) + ")";
                    }
                    mscaleDevice.scalevalue = scalevalue;
                    mscaleDevice.sumvalue = scalesumvalue;
                    mscaleDevice.mtime = Long.valueOf(System.currentTimeMillis());
                    generateScanlerDevice(mscaleDevice);
                    if (null != getBleScaleDevice) {
                        getBleScaleDevice.onGetBluetoothDevice(mScaleDevices);
                    }
                }
                scanupdate = false;
            }
        }
    };

    private void generateScanlerDevice(ScaleDevice scaleDevice) {
        if (!mScaleDevices.isEmpty()) {
            int size = mScaleDevices.size();
            for (int i = 0; i < size; ++i) {
                if (mScaleDevices.get(i).deviceaddr.equals(scaleDevice.deviceaddr)) {
                    mScaleDevices.set(i, scaleDevice);
                    break;
                }
                if (i == size - 1) {
                    mScaleDevices.add(scaleDevice);
                }
            }
        } else {
            mScaleDevices.add(scaleDevice);
        }
    }

    private boolean bytestoAnalysis(byte[] bytes) {
        byte[] AnChars = new byte[12];
        if (bytes.length == 0) return false;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == -6) {
                if ((bytes.length - i) >= 12 && bytes[i + 1] == -5) {
                    for (int j = 0; j < 12; j++) AnChars[j] = bytes[i + j];
                    break;
                }
            }
            if (i == bytes.length - 1) return false;
        }
        byte xor8;
        xor8 = AnChars[11];
        if (sum8xor(AnChars, 11) == xor8) {
            scalevalue = ArryToFloat(AnChars, 3);
            scalesumvalue = ArryToFloat(AnChars, 7);
        } else return false;
        return true;
    }

    static float ArryToFloat(byte[] Array, int Pos) {
        int accum = 0;
        accum = Array[Pos + 0] & 0xFF;
        accum |= (long) (Array[Pos + 1] & 0xFF) << 8;
        accum |= (long) (Array[Pos + 2] & 0xFF) << 16;
        accum |= (long) (Array[Pos + 3] & 0xFF) << 24;
        return Float.intBitsToFloat(accum);
    }

    private byte sum8xor(byte[] data, int len) {
        int fcs = 0;
        int sc;
        for (int i = 0; i < len; i++) {
            sc = data[i] & 0xff;
            fcs += sc;
        }
        fcs = fcs ^ 0xFF;
        return (byte) fcs;
    }

    public void connectDevice(ScaleDevice scaledevice, BTN_TYPE type) {
        byte senddate = 0x00;
        if (type == BTN_TYPE.BTN_ZERO) senddate = (byte) 0xc0;
        if (type == BTN_TYPE.BTN_CALIIB) senddate = (byte) 0xEC;
        if (connectdate == false) {
            setBluetoothState(1);
            mBleThread = new BleThread(bluetoothAdapter, scaledevice, senddate);
            mBleThread.start();
            connectdate = true;
        }
    }

    private class BleThread extends Thread {
        public int connectState = 0;
        public BLE_item ble_item = new BLE_item();
        private BluetoothAdapter mbluetoothAdapter;
        private ScaleDevice mscaleDevice;
        private BluetoothDevice device;
        private BluetoothGatt bluetoothGatt;
        private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
            //检测连接状态变化
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectState = 0;
                }
            }

            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    setServiceUUID(getSupportedGattServices());
                    connectState = 2;
                }
            }
        };
        private byte mSendDate;

        public BleThread(BluetoothAdapter bluetoothAdapter, ScaleDevice scaledevice, Byte sendData) {
            mbluetoothAdapter = bluetoothAdapter;
            mscaleDevice = scaledevice;
            mSendDate = sendData;
        }

        public void close() {
            if (bluetoothGatt == null) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            ble_item.write_characteristic = null;
            bluetoothGatt.disconnect();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            bluetoothGatt.close();
        }

        public List<BluetoothGattService> getSupportedGattServices() {
            if (bluetoothGatt == null) return null;

            return bluetoothGatt.getServices();
        }

        public void setServiceUUID(List<BluetoothGattService> services) {
            for (BluetoothGattService service : services) {
                ble_item.addService(service);
            }
            for (BluetoothGattService service : services) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        setCharacteristicNotification(characteristic, true);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                        if (ble_item.write_characteristic_NoRe == null) {
                            ble_item.write_characteristic_NoRe = characteristic;
                        }
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                        if (ble_item.write_characteristic == null) {
                            ble_item.write_characteristic = characteristic;
                        }
                    }
                }
            }
        }

        class BLE_item {
            public ArrayList<String> arr_serviceUUID = new ArrayList<String>();
            public ArrayList<BluetoothGattService> arr_services = new ArrayList<BluetoothGattService>();
            public BluetoothGattCharacteristic write_characteristic;
            public BluetoothGattCharacteristic write_characteristic_NoRe;

            public void addService(BluetoothGattService service) {
                service.getCharacteristics();
                arr_services.add(service);
                String str_uuid = service.getUuid().toString();
                arr_serviceUUID.add(str_uuid.substring(4, 8));
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    String str_c_uuid = characteristic.getUuid().toString();
                    str_c_uuid = str_c_uuid.substring(4, 8);
                    if (str_c_uuid.toLowerCase().contains("fff1")) {
                        setCharacteristicNotification(characteristic, true);
                    }
                    if (str_c_uuid.toLowerCase().contains("fff2")) {
                        write_characteristic = characteristic;
                    }
                }
            }
        }

        public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                  boolean enabled) {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                return;
            }
            bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            //新增修改
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }

        public int write(final byte b[]) {
            if (ble_item.write_characteristic == null) return 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                ble_item.write_characteristic.setValue(b);
                return writeCharacteristic(ble_item.write_characteristic) ? 1 : 0;

            }
            return 0;
        }

        public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                return false;
            }
            return bluetoothGatt.writeCharacteristic(characteristic);
        }

        public boolean connectscale() {
            if (connectState == 0) {
                connectState = 1;
                device = mbluetoothAdapter.getRemoteDevice(mscaleDevice.deviceaddr);
                bluetoothGatt = device.connectGatt(context, false, mGattCallback);
                if (bluetoothGatt == null) {
                    connectState = 0;
                    return false;
                }
                for (int i = 0; i < 400; i++) {
                    if (connectState == 2) break;
                    if (connectState == 0) break;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
                if (connectState != 2) {
                    close();
                    connectState = 0;
                    return false;
                }
                for (int i = 0; i < 100; i++) {
                    if (ble_item.write_characteristic != null) break;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (write(new byte[]{(mSendDate)}) == 0) {
                    connectState = 0;
                    close();
                    return false;
                }
                connectState = 0;
                close();
                return false;
            }
            return false;
        }

        public void run() {
            {
                connectscale();
                connectdate = false;
                WeightScale.this.setBluetoothState(0);
            }
        }
    }

    public static enum BTN_TYPE {
        BTN_ZERO,
        BTN_CALIIB;

        private BTN_TYPE() {
        }
    }
}