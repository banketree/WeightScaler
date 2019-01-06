package com.example.weightscaler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

public class MainActivity extends Activity implements OnItemClickListener, InterClick {
    protected static final String TAG = null;
    private scalerSDK scale;
    private ListView textScanDevice;
    private TextView stateText;
    private ContentAdapter adapter;
    private ArrayList<Map<String, Object>> listData;
    private ArrayList<Map<String, Object>> listlastData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stateText = (TextView) findViewById(R.id.statetextview);
        textScanDevice = (ListView) findViewById(R.id.listView);
        listData = new ArrayList<Map<String, Object>>();
        listlastData = new ArrayList<Map<String, Object>>();
        adapter = new ContentAdapter(this, listData, this);
        textScanDevice.setAdapter(adapter);
        textScanDevice.setOnItemClickListener(this);
        stateText.setText("蓝牙秤DEMO ");
        scale = new scalerSDK(MainActivity.this, new IGetBLEScaleDevice() {
            @Override
            public void onGetBluetoothDevice(ArrayList<ScaleDevice> scaleDevices) {
                scale.updatelist();
                setData(scaleDevices);
            }

            @Override
            public void onBluetoothState(int state) {
                if (state == 0) {
                } else {
//                    scale.Scan(true); //开始扫描
                }
            }
        });

        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
        startScan();
    }

    private void startScan() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (!granted) return;
                        scale.bleEnabled();
                        scale.Scan(true);
                    }
                });
    }

    private void setData(ArrayList<ScaleDevice> scaleDevices) {
        if (null != scaleDevices && scaleDevices.isEmpty() != true) {
            listData.clear();
            int size = scaleDevices.size();
            for (int i = 0; i < size; i++) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("devicename", scaleDevices.get(i).devicename);
                map.put("value", String.format("%4.3f" + "kg" + "     " + "SUM:%4.3f" + "kg", scaleDevices.get(i).scalevalue, scaleDevices.get(i).sumvalue));
                listData.add(map);
            }
        } else {
            listData.clear();
        }
        if (listData.equals(listlastData) == false)
            adapter.notifyDataSetChanged();
        listlastData.clear();
        for (int j = 0; j < listData.size(); j++)
            listlastData.add(listData.get(j));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public void ZeroClick(int postion, View v) {
        scale.bleSend(scale.getDevicelist().get(postion), (byte) 1);
    }

    public void CalibClick(int postion, View v) {
        scale.bleSend(scale.getDevicelist().get(postion), (byte) 2);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
    }
}
