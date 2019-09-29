package util.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import util.LogUtil;


/**
 * @author ChenYu
 * Author's github https://github.com/PrettyAnt
 * <p>
 * Created on 19:51  2019-08-28
 * PackageName : com.example.spdbsoappandroid.voiceplay.util
 * describle :
 */
public class AudioTrackPlayer {
    private AudioTrack audioTrack;
    private PcmDataProvider pcmDataProvider;
    private int status;
    private static AudioTrackPlayer INSTANCE;
    private ArrayList<String> filePath;

    public static AudioTrackPlayer getInstance() {
        if (INSTANCE == null) {
            synchronized (AudioTrackPlayer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AudioTrackPlayer();
                }
            }
        }
        return INSTANCE;
    }


    /**
     * 从开始位置播放
     *
     * @param filePath
     */
    public void play(ArrayList<String> filePath) {
        this.filePath = filePath;
        if (status == Status.UN_INIT) {
            int bufferSizeInBytes = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            pcmDataProvider = new PcmDataProvider(audioTrack, bufferSizeInBytes);
            status = Status.PREPARE;
        }
        if (status == Status.PLAYING) {
//            throw new IllegalStateException("正在播放，不能重复点击");
            return;
        }
        if (status == Status.PAUSE) {
            resume();
            return;
        }
        audioTrack.play();
        if (filePath.size() > 0) {
            LogUtil.debugLog(LogUtil.TAG, "play--->>> 开始播放");
            pcmDataProvider.start(filePath.get(0), this);
            filePath.remove(0);
            status = Status.PLAYING;
        } else {
            LogUtil.warnLog(LogUtil.TAG, "-------无数据----------");
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        LogUtil.debugLog(LogUtil.TAG, "stop--->>>停止播放");
        if (status == Status.PLAYING || status == Status.PAUSE) {
            status = Status.UN_INIT;
            audioTrack.stop();
            pcmDataProvider.stop();
            audioTrack.release();
            filePath.clear();

        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        LogUtil.debugLog(LogUtil.TAG, "pause--->>>暂停播放" + status);
        if (status == Status.PLAYING) {
            audioTrack.pause();
            pcmDataProvider.pause();
            status = Status.PAUSE;
        }
    }

    /**
     * 继续播放
     */
    public void resume() {
        LogUtil.debugLog(LogUtil.TAG, "resume--->>>继续播放");
        if (status == Status.PAUSE) {
            audioTrack.play();
            pcmDataProvider.resume();
            status = Status.PLAYING;
        }
    }

    /**
     * 获取当前的播放状态
     * @return
     */
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * 播放下一条
     */
    public void playNextData() {
        audioTrack.play();
        if (filePath.size() == 0) {
            LogUtil.debugLog(LogUtil.TAG, "playNextData--->>>播放完全结束");
            stop();
        } else {
            LogUtil.infoLog(LogUtil.TAG, "正在播放下一条......播放的文件为--->>>" + filePath.get(0)+"  剩余条数--->>>"+filePath.size());
            pcmDataProvider.start(filePath.get(0), this);
            filePath.remove(0);
            status = Status.PLAYING;
        }
    }

}
