package com.harispraba.voicediary.adapter;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.harispraba.voicediary.R;
import com.harispraba.voicediary.config.AppConfig;
import com.harispraba.voicediary.model.Recording;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rm.com.audiowave.AudioWaveView;

public class RecordingListAdapter extends RecyclerView.Adapter<RecordingListAdapter.ViewHolder> {
    private Activity mActivity;
    private LayoutInflater layoutInflater;
    private AppConfig appConfig;
    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private String dataKey;
    private String userName;
    private MediaPlayer mPlayer;

    private Handler seekHandler;
    private Runnable seekUpdater;

    private ArrayList<DataSnapshot> mSnapshotList;
    private ArrayList<String> mSnapshotKeys;

    public RecordingListAdapter(Activity activity, AppConfig config, DatabaseReference dbRef,
                                StorageReference stoRef, String data, String user){
        mActivity = activity;
        layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        appConfig = config;
        mDatabase = dbRef;
        storageRef = stoRef;
        dataKey = data;
        userName = user;
        mPlayer = new MediaPlayer();

        mSnapshotList = new ArrayList<>();
        mSnapshotKeys = new ArrayList<>();

        DatabaseReference recDbRef = mDatabase.child("notes/" + userName + "/" + dataKey + "/recordings");
        recDbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                mSnapshotList.add(dataSnapshot);
                mSnapshotKeys.add(dataSnapshot.getKey());
                notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String key = dataSnapshot.getKey();
                int index = mSnapshotKeys.indexOf(key);

                if(index > -1){
                    mSnapshotList.set(index, dataSnapshot);
                    mSnapshotKeys.set(index, dataSnapshot.getKey());
                    notifyItemChanged(index);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();
                int index = mSnapshotKeys.indexOf(key);

                if(index > -1){
                    mSnapshotList.remove(index);
                    mSnapshotKeys.remove(index);
                    notifyItemRemoved(index);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.buttonPlay) ImageView buttonPlay;
        @BindView(R.id.buttonPause) ImageView buttonPause;
        @BindView(R.id.buttonDownload) ImageView buttonDownload;
        @BindView(R.id.audioWave) AudioWaveView audioWave;
        @BindView(R.id.recLength) TextView recLength;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recording_item, parent, false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Recording rec = getItem(position);

        final File localFile = new File(appConfig.getLocalDir() + File.separator + rec.getFileName());
        if(localFile.exists()){
            try {
                InputStream inputStream = new FileInputStream(localFile);
                holder.audioWave.setRawData(readByte(inputStream));
                holder.audioWave.setWaveColor(R.color.colorAccent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final int duration = getDuration(localFile.getPath());
        int seconds = duration / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;

        holder.recLength.setText(String.format(Locale.US,"%02d:%02d", minutes, seconds));
        holder.buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(localFile.getPath());
                    mPlayer.prepare();
                    mPlayer.start();
                    holder.buttonPlay.setVisibility(View.GONE);
                    holder.buttonPause.setVisibility(View.VISIBLE);

                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            holder.buttonPlay.setVisibility(View.VISIBLE);
                            holder.buttonPause.setVisibility(View.GONE);

                            stopPlaying();
                        }
                    });

                    setupSeekBar(mPlayer, holder.audioWave, duration);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        holder.buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.buttonPlay.setVisibility(View.VISIBLE);
                holder.buttonPause.setVisibility(View.GONE);

                stopPlaying();
            }
        });
        holder.buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // download file if it's still in cloud
            }
        });
    }

    private void setupSeekBar(final MediaPlayer mediaPlayer, final AudioWaveView waveView, final int duration){
        waveView.setProgress(0);
        seekHandler = new Handler();
        seekUpdater = new Runnable() {
            @Override
            public void run() {
                int pos = mediaPlayer.getCurrentPosition();
                float percentage = (pos * 100) / duration;
                waveView.setProgress(percentage);
                seekHandler.postDelayed(this, 50);
            }
        };
        seekHandler.postDelayed(seekUpdater, 50);

//        waveView.setOnProgressListener(new OnProgressListener() {
//            @Override
//            public void onStartTracking(float v) {
//
//            }
//
//            @Override
//            public void onStopTracking(float v) {
//                int pos = (int) ((float)duration * (v / 100));
//                mediaPlayer.seekTo(pos);
////                mediaPlayer.sta
//            }
//
//            @Override
//            public void onProgressChanged(float v, boolean b) {
//
//            }
//        });
    }

    public void stopPlaying(){
        if(seekHandler != null)
            seekHandler.removeCallbacks(seekUpdater);

        if(mPlayer != null){
            mPlayer.release();
            mPlayer = null;
        }
    }

    public int getItemCount() {
        return mSnapshotList.size();
    }

    private Recording getItem(int pos){
        return mSnapshotList.get(pos).getValue(Recording.class);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    private byte[] readByte(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1){
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }

    private int getDuration(String path){
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(path);
        String data = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        return Integer.parseInt(data);
    }
}
