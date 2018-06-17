package com.exercise.duration.exercisedurationproject;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

// 1. 30초에 한번씩 호출 -> 2분에 한번씩 호출 (한 구간)
// 2. 칼로리계산 공식 : 10 * time
// 3. 30초마다 칼로리 계산,
public class MainActivity extends Activity implements View.OnClickListener {
    private TextView tvTimer;
    private TextView tvCalories;
    private Button btnStart, btnPause, btnResume, btnStop;
    private int currentTime = 0;
    private int kcal = 0;
    private long totalTime = 0;
    private long startTime = 0;
    private long pauseTime = 0;

    private long bandStartTime = 0;
    private long bandEndTime = 0;

    private int dataListNum = 0; // 현재 밴드데이터가 몇번째인지에 대한 구분자자


    private ArrayList<Long> startDurationArray = new ArrayList<>();
    private ArrayList<Long> endDurationArray = new ArrayList<>();
    private Map<Long, Map<String, Long>> pauseMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTimer = (TextView) findViewById(R.id.tv_timer);
        tvCalories = (TextView) findViewById(R.id.tv_kcal);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnPause = (Button) findViewById(R.id.btn_pause);
        btnResume = (Button) findViewById(R.id.btn_resume);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStart.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnResume.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    // 30초마다 갱신되는 칼로리Handler
    Handler kcal_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            kcal_handler.postDelayed(kcalTask, 60000); // 1분 후 실행
        }
    };
    private Runnable kcalTask = new Runnable() {
        @Override
        public void run() {
            bandStartTime = bandEndTime;
            bandEndTime = System.currentTimeMillis();
            startDurationArray.add(bandStartTime);
            endDurationArray.add(bandEndTime);
            Log.d("CHECK", "bandStartTime: " + bandStartTime);
            Log.d("CHECK", "bandEndTime: " + bandEndTime + "------");
            calculate();
            kcal++;
            dataListNum = kcal;
            tvCalories.setText(String.valueOf(kcal));
            kcal_handler.sendEmptyMessage(0);
        }
    };


    // 매초마다 갱신되는 타임Handler
    Handler time_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            time_handler.sendEmptyMessage(0);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                bandStartTime = System.currentTimeMillis();
                bandEndTime = bandStartTime;
                Log.d("CHECK", "bandStartTime: " + bandStartTime);
                startTime = System.currentTimeMillis();
                time_handler.sendEmptyMessage(0);
                kcal_handler.sendEmptyMessage(0);
                tvCalories.setText(String.valueOf(0));
                break;
            case R.id.btn_resume:
                startTime = startTime - (System.currentTimeMillis() - pauseTime);
                time_handler.sendEmptyMessage(0);
                long resumeTime = System.currentTimeMillis();
                // if (pauseMap.containsKey(pauseTime)) {
                Map<String, Long> resumeItem = new HashMap<>();
                //    resumeItem = pauseMap.get(pauseTime);
                resumeItem.put("R", resumeTime);
                pauseMap.put(resumeTime, resumeItem);
                Log.d("CHECK", pauseTime + ",의 계속 버튼 클릭: " + System.currentTimeMillis());
                // }
                break;
            case R.id.btn_pause:
                pauseTime = System.currentTimeMillis();
                time_handler.removeCallbacksAndMessages(null);
                //kcal_handler.removeCallbacksAndMessages(null);
                Map<String, Long> pauseItem = new HashMap<>();
                pauseItem.put("P", pauseTime);
                pauseMap.put(pauseTime, pauseItem);
                Log.d("CHECK", "일시정지버튼 클릭 : " + pauseTime + ", " + pauseMap);
                break;
            case R.id.btn_stop:
                startTime = 0;
                pauseTime = 0;
                time_handler.removeCallbacksAndMessages(null);
                kcal_handler.removeCallbacksAndMessages(null);
                tvTimer.setText(null);
                break;
        }
    }

    private void calculate() {
        if (startDurationArray.size() > 0 && endDurationArray.size() > 0) {
            double exerciseTime = 0;
            boolean isPauseState = false; // 현재 상태가 pause : true, 현재 상태가 resume : false
            boolean isNoData = true; // 한 구간에 일시정지or계속버튼 누른것이 아무것도 없는 경우
            long tempResumeTime = 0; // 임시 resumeTime을 저장하는 변수
            for (int i = 0; i < startDurationArray.size(); i++) {
                long start = startDurationArray.get(i);
                long end = endDurationArray.get(i);
                isNoData = true;
                if (pauseMap.size() > 0) {
                    Long key = null;
                    Iterator<Long> keys = pauseMap.keySet().iterator();
                    while (keys.hasNext()) {
                        key = keys.next();
                        if (start <= key &&
                                key <= end) {
                            isNoData = false;
                            if (pauseMap.get(key).get("R") != null) {
                                if (isPauseState) {
                                    tempResumeTime = pauseMap.get(key).get("R").longValue();
                                    isPauseState = false;
                                    Log.d("THEEND", "tempResumeTime에 저장");
                                } else {
                                    exerciseTime = exerciseTime + ((pauseMap.get(key).get("P").longValue() - start) / (1000.0 * 60.0));
                                    tempResumeTime = pauseMap.get(key).get("R").longValue();
                                    isPauseState = true;
                                    Log.d("THEEND", "exerciseTime: " + exerciseTime);
                                }
                            } else {
                                if (tempResumeTime != 0) {
                                    exerciseTime = exerciseTime + ((pauseMap.get(key).get("P").longValue() - tempResumeTime) / (1000.0 * 60.0));
                                    tempResumeTime = 0;
                                    isPauseState = true;
                                    Log.d("THEEND", "exerciseTime: " + exerciseTime);
                                } else {
                                    exerciseTime = exerciseTime + ((pauseMap.get(key).get("P").longValue() - start) / (1000.0 * 60.0));
                                    isPauseState = true;
                                }
                            }
                        }
                    }
                    if (tempResumeTime != 0) {
                        exerciseTime = exerciseTime + ((end - tempResumeTime) / (1000.0 * 60.0));
                        tempResumeTime = 0;
                        isPauseState = false;
                    }
                    if (isNoData && !isPauseState) {
                        exerciseTime = exerciseTime + ((end - start) / (1000.0 * 60.0));
                    }
                }
            }
            Log.d("THEEND", "totalExerciseTime: " + exerciseTime);
        }
    }
}
