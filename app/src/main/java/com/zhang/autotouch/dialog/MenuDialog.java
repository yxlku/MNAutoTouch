package com.zhang.autotouch.dialog;

import android.app.Application;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zhang.autotouch.R;
import com.zhang.autotouch.TouchEventManager;
import com.zhang.autotouch.adapter.TouchPointAdapter;
import com.zhang.autotouch.bean.TouchEvent;
import com.zhang.autotouch.bean.TouchPoint;
import com.zhang.autotouch.utils.DensityUtil;
import com.zhang.autotouch.utils.DialogUtils;
import com.zhang.autotouch.utils.GsonUtils;
import com.zhang.autotouch.utils.SpUtils;
import com.zhang.autotouch.utils.ToastUtil;
import com.zhang.autotouch.utils.ToolDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.zhang.autotouch.utils.ToolDateTime.DF_HH_MM_SS;
import static com.zhang.autotouch.utils.ToolDateTime.DF_YYYY_MM_DD;
import static com.zhang.autotouch.utils.ToolDateTime.DF_YYYY_MM_DD_HH_MM_SS;

public class MenuDialog extends BaseServiceDialog implements View.OnClickListener {

    private Button btStop;
    private CheckBox bt_order;
    private RecyclerView rvPoints;

    private AddPointDialog addPointDialog;
    private Listener listener;
    private TouchPointAdapter touchPointAdapter;
    private RecordDialog recordDialog;
    private Context mCon;

    //TODO 可以选时间 现在固定
    private TextView tv_now_time;
    private TextView tv_ready_time;
    private TextView tv_count_down;

    public MenuDialog(@NonNull Context context) {
        super(context);
        mCon = context;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_menu;
    }

    @Override
    protected int getWidth() {
        return DensityUtil.dip2px(getContext(), 400);
    }

    @Override
    protected int getHeight() {
        return WindowManager.LayoutParams.WRAP_CONTENT;
    }

    private Timer timer;
    private EditText et_h;
    private EditText et_m;
    private EditText et_s;
    private EditText et_ms;
    //TODO 次数
    private int cout = 2;

    @Override
    protected void onInited() {
        setCanceledOnTouchOutside(true);
        findViewById(R.id.bt_exit).setOnClickListener(this);
        findViewById(R.id.bt_add).setOnClickListener(this);
        findViewById(R.id.bt_record).setOnClickListener(this);
        btStop = findViewById(R.id.bt_stop);
        btStop.setOnClickListener(this);
        rvPoints = findViewById(R.id.rv);

        tv_now_time = findViewById(R.id.tv_now_time);
        tv_ready_time = findViewById(R.id.tv_ready_time);
        tv_count_down = findViewById(R.id.tv_count_down);
        bt_order = findViewById(R.id.bt_order);

        et_h = findViewById(R.id.et_h);
        et_m = findViewById(R.id.et_m);
        et_s = findViewById(R.id.et_s);
        et_ms = findViewById(R.id.et_ms);

//        spStr = "2020-12-5 14:19:00"; //tv_ready_time

        tv_now_time.setText("当前时间：" + ToolDateTime.formatDateTime(new Date(), DF_YYYY_MM_DD_HH_MM_SS));
        //时间刷新器
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = Message.obtain();
                msg.what = 0;
                handler.sendMessage(msg);
            }
        }, 1000, 1000);
        touchPointAdapter = new TouchPointAdapter();
        //事件列表管理 - 点击既可执行
        touchPointAdapter.setOnItemClickListener(new TouchPointAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, final TouchPoint touchPoint) {
                timer = new Timer();
                TimerTask task = new TimerTask(){
                    @Override
                    public void run() {
                        String spStr = initConfig(); //spStr = "2020-12-5 14:19:00"; //tv_ready_time
                        Date spDate = ToolDateTime.parseDate(spStr);
                        //当前时间
                        Date currDate = ToolDateTime.gainCurrentDate();
                        //剩余时间
                        long midTime = spDate.getTime() - currDate.getTime();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("currDate", currDate);
                        bundle.putLong("midTime", midTime);
                        bundle.putSerializable("touchPoint", touchPoint);
                        bundle.putSerializable("spDate", spDate);

                        Message msg = Message.obtain();
                        msg.what = 1;
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                };
                timer.schedule(task, 100, 100);
            }
        });
        rvPoints.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPoints.setAdapter(touchPointAdapter);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (TouchEventManager.getInstance().isPaused()) {
                    TouchEvent.postContinueAction();
                }
            }
        });
        findViewById(R.id.tv_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                touchPointAdapter.removeAllDatas();
                touchPoints = new ArrayList<>();
                SpUtils.clearT(getContext());
            }
        });
        bt_order.setChecked(false);
        bt_order.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isOrderList = isChecked;
                buttonView.setText(isChecked? "顺序执行" : "单点");
            }
        });
    }
    private int strToIn(String str){
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        return Integer.parseInt(str);
    }
    private String initConfig(){
        String h = et_h.getText().toString().trim();
        String m = et_m.getText().toString().trim();
        String s = et_s.getText().toString().trim();
        String ms = et_ms.getText().toString().trim();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, strToIn(h));
        calendar.set(Calendar.MINUTE, strToIn(m));
        calendar.set(Calendar.SECOND, strToIn(s));
        calendar.set(Calendar.MILLISECOND, strToIn(ms));
        return ToolDateTime.formatDateTime(calendar.getTime(), DF_YYYY_MM_DD_HH_MM_SS);
    }
    //是否按照列表顺序执行
    private boolean isOrderList = false;
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                tv_now_time.setText("当前时间：" + ToolDateTime.formatDateTime(new Date(), DF_YYYY_MM_DD_HH_MM_SS));
            }else if(msg.what == 1){
                Bundle bundle = msg.getData();
                TouchPoint touchPoint = (TouchPoint) bundle.get("touchPoint");
                Long midTime = bundle.getLong("midTime");
                Date spDate = (Date) bundle.getSerializable("spDate");
//
                //选择时间
                tv_ready_time.setText("选择时间：");
                et_h.setText(spDate.getHours() + "");
                et_m.setText(spDate.getMinutes() + "");
                et_s.setText(spDate.getSeconds() + "");
                et_ms.setText("0");
                String midTimeStr = ToolDateTime.formatMidTime(midTime);
                tv_count_down.setText("剩余时间：" + midTimeStr);
                if (midTime <= 0) {
                    btStop.setVisibility(View.VISIBLE);
                    dismiss();
                    if (!isOrderList) {
                        //指定item执行
                        TouchEvent.postStartAction(touchPoint);
                        timer.cancel();
                    }else{
                        TouchPoint first = touchPoints.get(0);
                        TouchEvent.postStartAction(first);
                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask(){
                            int pos = 1;

                            TouchPoint point = null;
                            @Override
                            public void run() {
                                if(pos < touchPoints.size()) {
                                    point = touchPoints.get(pos);
                                    for (TouchPoint point : touchPoints) {
                                        TouchEvent.postStartAction(point);
                                        Log.e("ssss", ToolDateTime.formatDateTime(new Date(), DF_YYYY_MM_DD_HH_MM_SS));
                                    }
                                    pos++;
                                }else{
                                    timer.cancel();
                                }
                            }
                        }, 500, 500); //两个事件间隔毫秒
                    }
                    ToastUtil.show("已开启触控点：" + touchPoint.getName());
                    timer.cancel();
                }
            }

        }
    };

    private List<TouchPoint> touchPoints;
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("啊实打实", "onStart");
        //如果正在触控，则暂停
        TouchEvent.postPauseAction();
        if (touchPointAdapter != null) {
            touchPoints = SpUtils.getTouchPoints(getContext());
            Log.d("啊实打实", GsonUtils.beanToJson(touchPoints));
            touchPointAdapter.setTouchPointList(touchPoints);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_add:
                DialogUtils.dismiss(addPointDialog);
                addPointDialog = new AddPointDialog(getContext());
                addPointDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        MenuDialog.this.show();
                    }
                });
                addPointDialog.show();
                dismiss();
                break;
            case R.id.bt_record:
                dismiss();
                if (listener != null) {
                    listener.onFloatWindowAttachChange(false);
                    if (recordDialog ==null) {
                        recordDialog = new RecordDialog(getContext());
                        recordDialog.setOnDismissListener(new OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                listener.onFloatWindowAttachChange(true);
                                MenuDialog.this.show();
                            }
                        });
                        recordDialog.show();
                    }
                }
                break;
            case R.id.bt_stop:
                btStop.setVisibility(View.GONE);
                TouchEvent.postStopAction();
                ToastUtil.show("已停止触控");
                break;
            case R.id.bt_exit:
                TouchEvent.postStopAction();
                if (listener != null) {
                    listener.onExitService();
                }
                break;

        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        /**
         * 悬浮窗显示状态变化
         * @param attach
         */
        void onFloatWindowAttachChange(boolean attach);

        /**
         * 关闭辅助
         */
        void onExitService();
    }
}
