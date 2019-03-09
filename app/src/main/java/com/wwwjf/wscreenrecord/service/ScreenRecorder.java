package com.wwwjf.wscreenrecord.service;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;


import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class ScreenRecorder {

    private int mWidth, mHeight, mDensty;
    private MediaProjection mediaProjection;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodec mEncorder;
    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private boolean isQuit = false;
    private boolean mMuxerStarted = false;
    private int mTrackIndex;
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cache";

    private MediaRecorder mediaRecorder;

    public ScreenRecorder(int mWidth, int mHeight, MediaProjection mediaProjection, int mDensty) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mediaProjection = mediaProjection;
        this.mDensty = mDensty;

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void startRecorder() {
        prepareRecorder();
        startLuYin();
        startRecording();
    }

    public void stop() {
        isQuit = true;
        releaseEncorders(1);
        List<String> filePath = new ArrayList<>();
        filePath.add(path + "/APlanyinpin.amr");
        filePath.add(path + "/APlanshipin.mp4");
        joinVideo(filePath, path);
    }

    public void destory() {
        releaseEncorders(0);
    }

    private void startLuYin() {
        File file = new File(path, "APlanyinpin.amr");
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setOutputFile(file.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.e("HandDrawActivity", "已经开始录音");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareRecorder() {
        mBufferInfo = new MediaCodec.BufferInfo();  //元数据，描述bytebuffer的数据，尺寸，偏移
        //创建格式化对象 MIMI_TYPE 传入的 video/avc 是H264编码格式
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
        int frameRate = 45;
        format.setInteger(MediaFormat.KEY_BIT_RATE, 3000000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);

        try {
            mEncorder = MediaCodec.createEncoderByType("video/avc");
            mEncorder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncorder.createInputSurface();
            mEncorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            releaseEncorders(0);
        }
    }

    private void startRecording() {
        File saveFile = new File(path, "APlanshipin.mp4");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMuxer = new MediaMuxer(saveFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mediaProjection.createVirtualDisplay("SCREENRECORDER", mWidth, mHeight, mDensty, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        mInputSurface, null, null);
                drainEncoder();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void drainEncoder() {
        while (!isQuit) {
            Log.e("TAG", "drain.....");
            int bufferIndex = mEncorder.dequeueOutputBuffer(mBufferInfo, 0);
            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mTrackIndex = mMuxer.addTrack(mEncorder.getOutputFormat());
                if (!mMuxerStarted && mTrackIndex >= 0) {
                    mMuxer.start();
                    mMuxerStarted = true;

                    Log.e("HandDrawActivity", "已经开始录屏");
                }
            }
            if (bufferIndex >= 0) {
                Log.e("TAG", "drain...write..");
                ByteBuffer bufferData = mEncorder.getOutputBuffer(bufferIndex);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (mMuxerStarted) {
                        bufferData.position(mBufferInfo.offset);
                        bufferData.limit(mBufferInfo.offset + mBufferInfo.size);
                        mMuxer.writeSampleData(mTrackIndex, bufferData, mBufferInfo);
                    }
                }
                mEncorder.releaseOutputBuffer(bufferIndex, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
        Log.e("HandDrawActivity", "已经结束录屏");

    }

    private void releaseEncorders(int i) {
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        mBufferInfo = null;
        if (mEncorder != null) {
            mEncorder.stop();
        }
        mInputSurface = null;
        if (mMuxer != null && i == 1) {
            mMuxer.stop();
        }
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
        }

    }

    private boolean joinVideo(List<String> filePaths, String resultPath) {
        Log.e("HandDrawActivity", "准备合成中");
        boolean result = false;

        if (filePaths == null || filePaths.size() <= 0 || TextUtils.isEmpty(resultPath)) {
            throw new IllegalArgumentException();
        }

        if (filePaths.size() == 1) { // 只有一个视频片段，不需要合并
            return true;
        }

        try {
            Movie[] inMovies = new Movie[filePaths.size()];
            for (int i = 0; i < filePaths.size(); i++) {
                Log.e("HandDrawActivity", "filePaths=" + filePaths.get(i));
                File f = new File(filePaths.get(i));
                if (f.exists()) {
                    inMovies[i] = MovieCreator.build(filePaths.get(i));
                }
            }

            // 分别取出音轨和视频
            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();
            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                }
            }

            // 合并到最终的视频文件
            Movie outMovie = new Movie();

            if (audioTracks.size() > 0) {
                outMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                outMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }

            Container mp4file = new DefaultMp4Builder().build(outMovie);

            // 将文件输出
            File resultFile = new File(resultPath, "APlanTeacherAnswer.mp4");
            if (resultFile.exists() && resultFile.isFile()) {
                resultFile.delete();
            }
            FileChannel fc = new RandomAccessFile(resultFile, "rw").getChannel();
            mp4file.writeContainer(fc);
            fc.close();
            Log.e("HandDrawActivity", "合成完毕");
            // 合成完成后把原片段文件删除
            for (String filePath : filePaths) {
                File file = new File(filePath);
                file.delete();
            }
            result = true;

//            HandDrawActivity.sendVideo();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }
}