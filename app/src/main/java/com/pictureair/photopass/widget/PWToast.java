package com.pictureair.photopass.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pictureair.photopass.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 自定义Toast
 *
 * How To Use:
 * 1. PWToast pwToast = new PWToast(context);
 *    pwToast.setTextAndShow(); 注意：可选择自己需要的方法
 *
 * 2. PWToast.getInstance(context).setTextAndShow(); 注意：可选择自己需要的方法
 *    此方法无法cancel，如果需要cancel toast的话，请使用方式1
 */
public class PWToast extends Toast {

    private Toast toast;
    private TextView textView;
    private Timer timer;
    private static volatile PWToast pwToast;

    public static PWToast getInstance(Context context) {
        if(pwToast == null) {
            synchronized(PWToast.class) {
                if(pwToast == null) {
                    pwToast = new PWToast(context);
                }
            }
        }
        return pwToast;
    }

    public PWToast(Context context) {
        super(context);
        toast = new Toast(context);
        timer = new Timer();
        //获取LayoutInflater对象
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //由layout文件创建一个View对象
        View view = inflater.inflate(R.layout.newtoast, null);
        //实例化ImageView和TextView对象
        textView = (TextView) view.findViewById(R.id.toast_textview);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);// 设置toast位置
    }

    /**
     * 显示toast
     * @param str
     * @param time
     */
    public void setTextAndShow(String str, int time) {
        textView.setText(str);
        setTimeAndShow(time);
    }

    /**
     * 显示toast
     * @param stringId
     * @param time
     */
    public void setTextAndShow(int stringId, int time) {
        if (stringId == 0) {
            textView.setText("default toast");
        } else {
            textView.setText(stringId);
        }
        setTimeAndShow(time);
    }

    /**
     * 显示toast, 默认LENGTH_SHORT时间
     * @param str
     */
    public void setTextAndShow(String str) {
        textView.setText(str);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * 显示toast, 默认LENGTH_SHORT时间
     * @param stringId
     */
    public void setTextAndShow(int stringId) {
        if (stringId == 0) {
            textView.setText("default toast");
        } else {
            textView.setText(stringId);
        }
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * 取消显示toast
     */
    public void cancelShow(){
        if (toast != null) {
            toast.cancel();
        }
    }

    /**
     * 设置时间
     * @param time
     */
    private void setTimeAndShow(int time) {
        if (time < 1000) {//至少1000ms以上
            time = 1000;
        }
        toast.show();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (toast != null) {
                    toast.cancel();
                }
            }
        }, time);
    }
}
