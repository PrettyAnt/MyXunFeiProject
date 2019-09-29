package util.audio;

import android.media.AudioTrack;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import util.LogUtil;

/**
 * @author ChenYu
 * Author's github https://github.com/PrettyAnt
 * <p>
 * Created on 19:51  2019-08-28
 * PackageName : com.example.spdbsoappandroid.voiceplay.util
 * describle :
 */
public class PcmDataProvider implements Runnable {
    private static final String TAG = "PcmDataProvider";
    private String mFilePath;
    private int mBuffsize;
    private volatile boolean run;
    private AudioTrack mTrack;
    private Thread mThread;
    private Condition mCondition;
    private ReentrantLock mLock;
    private volatile boolean mPause;
    private AudioTrackPlayer audioTrackPlayer;

    public PcmDataProvider(AudioTrack track, int buffSize) {
        this.mBuffsize = buffSize;
        this.mTrack = track;
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
    }

    public void start(String filePath, AudioTrackPlayer audioTrackPlayer) {
        this.mFilePath = filePath;
        this.audioTrackPlayer = audioTrackPlayer;
        run = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void stop() {
        run = false;
    }

    public void pause() {
        mPause = true;
    }

    public void resume() {
        try {
            mLock.lock();
            mPause = false;
            mCondition.signal();
            LogUtil.infoLog(TAG, "resume播放");
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void run() {
        File file = new File(mFilePath);
        if (!file.exists()) {
            LogUtil.errorLog(TAG, "文件不存在");
            return;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (fis == null) return;
        byte[] buff = new byte[mBuffsize];
        while (run) {
            try {
                mLock.lock();
                while (mPause) {
                    LogUtil.infoLog(TAG, "暂停播放");
                    mCondition.await();
                }
                int ret = fis.read(buff);
                LogUtil.infoLog(TAG, "fis.read ret = " + ret);
                if (ret != -1) {
                    int r = mTrack.write(buff, 0, ret);
                    if (r < 0) {
                        LogUtil.errorLog(TAG, "AudioTrack写入错误 r =" + r);
                        break;
                    }
                } else {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mLock.unlock();
            }
        }
        if (audioTrackPlayer.getStatus() != Status.UN_INIT) {//如果不是未被初始化
            audioTrackPlayer.playNextData();
        }
    }
}
