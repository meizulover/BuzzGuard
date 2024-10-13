package com.example.mosquitorepellent;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    private Thread soundThread;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取按钮并设置点击事件
        Button toggleButton = findViewById(R.id.toggleButton);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startScreenBrightness();
                    startSound();
                    isPlaying = true;
                    toggleButton.setText("停止白屏和声音");
                } else {
                    stopSound();
                    resetScreenBrightness();
                    isPlaying = false;
                    toggleButton.setText("开启白屏并播放声音");
                }
            }
        });
    }

    // 设置屏幕亮度为最高
    private void startScreenBrightness() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f; // 1.0 表示最高亮度
        getWindow().setAttributes(layoutParams);
        getWindow().getDecorView().setBackgroundColor(0xFFFFFFFF); // 设置背景为白色
    }

    // 重置屏幕亮度
    private void resetScreenBrightness() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(layoutParams);
        getWindow().getDecorView().setBackgroundColor(0xFF000000); // 重置背景为黑色
    }

    // 开始播放随机频率的声音（包括求偶的1200 Hz）
    private void startSound() {
        isPlaying = true;
        soundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPlaying) {
                    // 随机选择一个频率：400 Hz 到 600 Hz 或 1200 Hz
                    int frequency;
                    if (random.nextBoolean()) {
                        frequency = 1200; // 求偶频率
                    } else {
                        frequency = random.nextInt(201) + 400; // 400 到 600 Hz
                    }

                    playFrequency(frequency);
                    try {
                        Thread.sleep(1000); // 每隔 1 秒更换一次频率
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        soundThread.start();
    }

    // 播放指定频率的声音
    private void playFrequency(int frequency) {
        int sampleRate = 44100; // 采样率
        int numSamples = sampleRate;
        double[] sample = new double[numSamples];
        byte[] generatedSnd = new byte[2 * numSamples];

        // 生成正弦波
        for (int i = 0; i < numSamples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
        }

        // 转换为 16-bit PCM 数据
        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        // 初始化或更新 AudioTrack
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC
        );
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

    // 停止声音播放
    private void stopSound() {
        isPlaying = false;
        if (soundThread != null) {
            try {
                soundThread.join(); // 确保线程停止
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound();
    }
}
