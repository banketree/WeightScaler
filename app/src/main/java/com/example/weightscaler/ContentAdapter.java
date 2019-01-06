package com.example.weightscaler;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class ContentAdapter extends BaseAdapter implements OnClickListener {

    private ArrayList<Map<String, Object>> mContentList;
    private LayoutInflater mInflater;
    private InterClick mCallback;
    private int index;

    public ContentAdapter(Context context, ArrayList<Map<String, Object>> contentList,
                          InterClick callback) {
        mContentList = contentList;
        mInflater = LayoutInflater.from(context);
        mCallback = callback;
    }

    @Override
    public int getCount() {
        return mContentList.size();
    }

    @Override
    public Object getItem(int position) {
        return mContentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.device_name, null);
            holder = new ViewHolder();
            holder.devicename = (TextView) convertView.findViewById(R.id.Devname);
            holder.value = (TextView) convertView.findViewById(R.id.valuetext);
            holder.button1 = (Button) convertView.findViewById(R.id.Zero_btn);
            //holder.button2=(Button) convertView.findViewById(R.id.Calib_btn);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.devicename.setText((String) mContentList.get(position).get("devicename"));
        holder.value.setText((String) mContentList.get(position).get("value"));
        holder.button1.setOnClickListener(this);
        //	holder.button2.setOnClickListener(this);
        // 设置位置，获取点击的条目按钮
        holder.button1.setTag(position);
        //	holder.button2.setTag(position);
        return convertView;
    }

    public class ViewHolder {
        public TextView devicename;
        public TextView value;
        public Button button1;
    }


    // 响应按钮点击事件,调用子定义接口，并传入View
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Zero_btn:
                index = (Integer) v.getTag();
                mCallback.ZeroClick(index, v);
                break;
            //	case R.id.Calib_btn:
            //		index=(Integer) v.getTag();
            //		mCallback.CalibClick(index,v);
            //		break;
            default:
                break;
        }
    }
}
