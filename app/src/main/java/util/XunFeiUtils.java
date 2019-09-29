package util;

import android.content.Context;
import android.os.Handler;


import com.iflytek.aipsdk.audio.AudioHelper;
import com.iflytek.aipsdk.audio.AudioListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import bean.SubContentModel;

/**
 * @author ChenYu
 * Author's github https://github.com/PrettyAnt
 * <p>
 * Created on 19:51  2019-08-28
 * PackageName : com.example.spdbsoappandroid.voiceplay.util
 * describle :
 */
public class XunFeiUtils {
    private static XunFeiUtils INSTANCE;
    private AudioHelper audioHelper;
    //科大讯飞
    //private String RECODE_PARAMS = "aue=raw,vad_res=meta_vad_16k.jet,res=0,vad_eos=500";
    public final static String RECODE_PARAMS = "aue=speex-wb,vad_res=meta_vad_16k.jet,res=0,vad_eos=600,mi=100";//压缩
    //private String RECODE_PARAMS = "aue=speex-wb,vad_res="+FileModel.AnimationFile + "/" +"meta_vad_16k.jet,res=1";
    public final static String RECODE_PARAMSSP = "aue=speex-wb-decode";//解压缩
    private SubContentModel subContentModel;
    private boolean isStart = false;
    private Context context;
    private Handler unSpeexHandler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                FileInputStream in = new FileInputStream(subContentModel.getRootPath() + "/" + subContentModel.getFileName() + ".pcm");
                int left = in.available();
                int perSize = 190320;      //每次块大小
                if (left < perSize) {
                    perSize = left;
                }

                byte[] temp = new byte[perSize];
                int size = 0;
                while ((size = in.read(temp)) != -1 && isStart == true) {
                    //写入音频流，调用该接口，一般都是耗时操作，为防止ANR，建议app开启线程调用或者其他异步方式调用
                    int error = audioHelper.startUnSpeex(temp);
                    left -= size;
                    if (left <= 0)
                        break;
                    if (left < perSize) {
                        perSize = left;
                        temp = new byte[perSize];
                    }
                    if (error != 0) {
                        /**
                         * writeAudio返回为非零，表示尚未开始会话，或者提前结束会话
                         */
                        unSpeexHandler.removeCallbacks(this);
                        break;
                    }
                }

                in.close();
                audioHelper.stopRecord();
                //写入音频结束后，一定要调用识别停止接口,获取最终的结果，且防止连接超时报错
                isStart = false;
            } catch (Exception e) {
                LogUtil.errorLog(LogUtil.TAG, "读取文件失败");
            }
        }
    };


    /**
     * 科大解码监听
     */
    private AudioListener unSpeexListener = new AudioListener() {
        @Override
        public void onRecordBuffer(byte[] bytes, int i) {
            if (bytes != null && bytes.length > 0) {
                writeData(bytes);
//                PlayVoiceUtils.getInstance().initAudioTrack(bytes).playAudioTrack();
            }
            LogUtil.errorLog(LogUtil.TAG, "unSpeexListener---onRecordBuffer-->>>" + i);
        }

        @Override
        public void onError(int i) {
            LogUtil.errorLog(LogUtil.TAG, "unSpeexListener---onError-->>>" + i);
        }
    };


    public static XunFeiUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (XunFeiUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new XunFeiUtils();
                }
            }
        }
        return INSTANCE;
    }

    public XunFeiUtils init(Context context, SubContentModel subContentModel) {
        this.context = context;
        this.subContentModel = subContentModel;
        audioHelper = new AudioHelper(context);

        return this;
    }


    /**
     * 启动科大解码监听
     */
    public XunFeiUtils startunSpeexListener() {
        if (audioHelper != null) {
            audioHelper.init(RECODE_PARAMSSP, unSpeexListener);
        }
        isStart = true;
        readFile();
        return this;
    }

    /**
     * 读取pcm
     */
    private void readFile() {
        unSpeexHandler.post(runnable);
    }

    public void pause() {
    }

    /**
     * 写文件
     *
     * @param bytes
     */
    private void writeData(byte[] bytes) {
        try {
            File file;
            String decodepcmPathName = subContentModel.getRootPath() + "/" + subContentModel.getFileName() + "_decode.pcm";
            file = new File(decodepcmPathName);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(bytes);
            fos.close();
            //继续解码下一个

            //直接播放pcm fixme

            //以下的转换仅仅是用来检查语音是否正常，不影响
//            String wavPath = subContentModel.getRootPath() + "/" + subContentModel.getFileName() + ".wav";
//            convertPcm2Wav(decodepcmPathName, wavPath, 16000, 1, 16);//将解压后的pcm文件转换为wav文件
//            subContentModel.setWavPath(wavPath);
        } catch (Exception e) {

        }


    }

    //--------------------pcm------>>wav

    /**
     * PCM文件转WAV文件
     *
     * @param inPcmFilePath  输入PCM文件路径
     * @param outWavFilePath 输出WAV文件路径
     * @param sampleRate     采样率，例如44100
     * @param channels       声道数 单声道：1或双声道：2
     * @param bitNum         采样位数，8或16
     */
    private void convertPcm2Wav(String inPcmFilePath, String outWavFilePath, int sampleRate,
                                int channels, int bitNum) {

        FileInputStream in = null;
        FileOutputStream out = null;
        byte[] data = new byte[1024];

        try {
            //采样字节byte率
            long byteRate = sampleRate * channels * bitNum / 8;

            in = new FileInputStream(inPcmFilePath);
            out = new FileOutputStream(outWavFilePath);

            //PCM文件大小
            long totalAudioLen = in.getChannel().size();

            //总大小，由于不包括RIFF和WAV，所以是44 - 8 = 36，在加上PCM文件大小
            long totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen, sampleRate, channels, byteRate);

            int length = 0;
            while ((length = in.read(data)) > 0) {
                out.write(data, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 输出WAV文件
     *
     * @param out           WAV输出文件流
     * @param totalAudioLen 整个音频PCM数据大小
     * @param totalDataLen  整个数据大小
     * @param sampleRate    采样率
     * @param channels      声道数
     * @param byteRate      采样字节byte率
     * @throws IOException
     */
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }


}
