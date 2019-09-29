package com.example.myxunfeiproject;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import util.LogUtil;
import util.audio.AudioTrackPlayer;

@RuntimePermissions
public class ScrollingActivity extends AppCompatActivity {
    @BindView(R.id.tv_start_unspeex)
    TextView tvStartUnspeex;
    @BindView(R.id.tv_start_play)
    TextView tvStartPlay;
    @BindView(R.id.tv_stop_play)
    TextView tvStopPlay;
    @BindView(R.id.tv_pause_play)
    TextView tvPausePlay;
    @BindView(R.id.tv_continue_play)
    TextView tvContinuePlay;
    private AudioTrackPlayer audioTrackPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        ButterKnife.bind(this);
//        requestPermissions(this);
        ScrollingActivityPermissionsDispatcher.getReadPermissionWithPermissionCheck(this);
        audioTrackPlayer = AudioTrackPlayer.getInstance();
        initData();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private ArrayList<String> pcmData = new ArrayList<>();

    /**
     * 已有的数据
     */
    private void initData() {
        pcmData.clear();
        String rootPath = Environment.getExternalStorageDirectory() + "/spdb_cache/test";
        File file = new File(rootPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String[] list = file.list();
        for (String dir : list) {
            pcmData.add(rootPath + "/" + dir);
        }
    }

    /**
     * 已有的数据
     */
    private void initData2() {
            pcmData.clear();
//        String rootPath = Environment.getExternalStorageDirectory() + "/spdb_cache/test";
            AssetManager assets = getResources().getAssets();
            try {
                String[] list = assets.list("voice");
                for (String dir : list) {
                    pcmData.add(  "file:///android_asset/voice/"+dir);
                }
            } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *  从Assets中读取图片
     * @param fileName
     * @return
     */
    private Bitmap getImageFromAssetsFile(String fileName)
    {
        Bitmap image = null;
        AssetManager am = getResources().getAssets();
        try
        {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return image;

    }

    /**
     * 模拟动态添加数据
     */
    private void addData() {
        LogUtil.errorLog(LogUtil.TAG, "--------添加了一个数据---------------");
        pcmData.add(Environment.getExternalStorageDirectory() + "/spdb_cache/aaa_decode.pcm");
    }

    @OnClick({R.id.tv_start_unspeex, R.id.tv_start_play, R.id.tv_stop_play, R.id.tv_pause_play, R.id.tv_continue_play})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_start_unspeex:
                addData();
                break;
            case R.id.tv_start_play:
                audioTrackPlayer.play(pcmData);
                break;
            case R.id.tv_stop_play:
                audioTrackPlayer.stop();
                break;
            case R.id.tv_pause_play:
                audioTrackPlayer.pause();
                break;
            case R.id.tv_continue_play:
                audioTrackPlayer.resume();
                break;
        }
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    void getReadPermission() {
        LogUtil.errorLog(LogUtil.TAG, "--------getReadPermission---------------");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtil.errorLog(LogUtil.TAG, "--------onRequestPermissionsResult---------------" + requestCode);
        ScrollingActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showPermission(final PermissionRequest request) {
        LogUtil.errorLog(LogUtil.TAG, "--------showPermission---------------" + request);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 99);
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    void deniedPermission() {
        LogUtil.errorLog(LogUtil.TAG, "--------deniedPermission---------------");
    }

    @OnNeverAskAgain(Manifest.permission.READ_EXTERNAL_STORAGE)
    void neverAskPermission() {
        LogUtil.errorLog(LogUtil.TAG, "--------neverAskPermission---------------");
    }


//
////    String decodepcmPathName = Environment.getExternalStorageDirectory() + "/spdb_cache/test/aaa_decode.pcm";
//
//    /**
//     * 申请权限
//     *
//     * @param activity
//     */
//    public void requestPermissions(Activity activity) {
//        try {
//            if (Build.VERSION.SDK_INT >= 23) {
//                int permission = ActivityCompat.checkSelfPermission(activity,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                if (permission != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(activity, new String[]
//                            {Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                    Manifest.permission.LOCATION_HARDWARE, Manifest.permission.READ_PHONE_STATE,
//                                    Manifest.permission.WRITE_SETTINGS, Manifest.permission.READ_EXTERNAL_STORAGE,
//                                    Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS}, 0x0010);
//                }
//
//                if (permission != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(activity, new String[]{
//                            Manifest.permission.ACCESS_COARSE_LOCATION,
//                            Manifest.permission.ACCESS_FINE_LOCATION}, 0x0010);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
