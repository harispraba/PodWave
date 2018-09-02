package com.harispraba.voicediary;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.transition.Slide;
import android.support.transition.Transition;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.harispraba.voicediary.activity.SelectFolderActivity;
import com.harispraba.voicediary.adapter.RecordingListAdapter;
import com.harispraba.voicediary.adapter.SelectFolderAdapter;
import com.harispraba.voicediary.config.AppConfig;
import com.harispraba.voicediary.model.Recording;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static butterknife.internal.Utils.arrayOf;

public class CreateNoteActivity extends AppCompatActivity {
    private final String TAG = "CreateNote";

    private FirebaseUser user;

    @BindView(R.id.recorderLayout) ViewGroup recorderParent;
    @BindView(R.id.startRecordButton) Button btnStartRecord;
    @BindView(R.id.stopRecordButton) Button btnStopRecord;
    @BindView(R.id.pauseButton) Button btnPauseRecord;
    @BindView(R.id.timer) TextView timerTextView;
    @BindView(R.id.iconRecording) ImageView iconRecordingState;
    @BindView(R.id.inputTitleEditText) TextInputEditText titleInput;
    @BindView(R.id.inputDescriptionEditText) TextInputEditText descInput;
    @BindView(R.id.recordingList) RecyclerView recordingListView;
    @BindView(R.id.cloudIcon) ImageView cloudIcon;
    @BindView(R.id.folderName) TextView folderNameView;

    private final int REQUEST_CODE_WRITE_EXTERNAL = 10;
    private final int SELECT_FOLDER_RESULT_CODE = 212;

    private AppConfig appConfig;
    private MediaRecorder mRecorder;
//    private MediaPlayer mPlayer;
    private String mFileName;
    private RecordingListAdapter recListAdapter;

    private long startTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long milis = System.currentTimeMillis() - startTime;
            int seconds = (int) milis / 1000;
            int hundredOfSec = (int) (milis - seconds * 1000) / 10;
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format(Locale.US,"%02d:%02d.%02d", minutes, seconds, hundredOfSec));

            timerHandler.postDelayed(this, 10);
        }
    };

    private Transition slide;

    // saving data
    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private FirebaseStorage mStorage;
    private final long SAVING_DELAY = 1000;
    private Timer savingTimer = new Timer();
//    private boolean savedFlag = false;
    private String dataKey = "";
    private String folderKey = "";
    private void makeOnlineData(){
        long currentTime = Calendar.getInstance().getTimeInMillis();
        dataKey = mDatabase.child("notes/" + user.getUid()).push().getKey();
        mDatabase.child("notes/" + user.getUid() + "/" + dataKey + "/createdAt").setValue(currentTime);
        changeFolder("DEFAULT", "DEFAULT");
    }
    private Runnable saveRunnable = new Runnable() {
        @Override
        public void run() {
            String title = titleInput.getText().toString();
            String description = descInput.getText().toString();

            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("notes/"+ user.getUid() +"/" + dataKey + "/title", title);
            childUpdates.put("notes/"+ user.getUid() +"/" + dataKey + "/description", description);

            mDatabase.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: save data success.");
//                    savedFlag = true;
                    setSavedIcon(CLOUD_SAVED);
                    Toast.makeText(getApplicationContext(), "Data saved", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure: save data failed. " + e.getMessage());
                }
            });
        }
    };
    private void saveData(){
//        savedFlag = false;
        setSavedIcon(CLOUD_NOT_SAVED);
        if(TextUtils.isEmpty(dataKey)){
//            dataKey = mDatabase.child("notes/" + user.getUid()).push().getKey();
            makeOnlineData();
            saveRunnable.run();
        }
        else{
            saveRunnable.run();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null)
            finish();

        ButterKnife.bind(this);
        appConfig = new AppConfig();
        slide = new Slide(Gravity.END);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference();
//        mPlayer = new MediaPlayer();

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setLogo(R.drawable.ic_menu_record);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            // requesting permission
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_WRITE_EXTERNAL);
        }
        else {
            //initialRecording();
        }

        if(getIntent().getBooleanExtra("isEditNote", false)){
            dataKey = getIntent().getStringExtra("dataKey");
            folderKey = getIntent().getStringExtra("folderKey");
            if(folderKey == null || folderKey.isEmpty()){
                folderKey = "DEFAULT";
                changeFolder(folderKey, "DEFAULT");
            }
            else{
                folderNameView.setText(getIntent().getStringExtra("folderName"));
            }
            titleInput.setText(getIntent().getStringExtra("title"));
            descInput.setText(getIntent().getStringExtra("description"));

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

            setSavedIcon(CLOUD_SAVED);
            initializeListView();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(savingTimer != null){
//                    savedFlag = false;
                    setSavedIcon(CLOUD_NOT_SAVED);
                    savingTimer.cancel();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                savingTimer = new Timer();
                savingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        saveData();
                    }
                }, SAVING_DELAY);
            }
        };

        titleInput.addTextChangedListener(inputWatcher);
        descInput.addTextChangedListener(inputWatcher);
    }

    private boolean listInitialized = false;
    private void initializeListView(){
        if(!listInitialized){
            recListAdapter =
                    new RecordingListAdapter(this, appConfig, mDatabase, storageRef, dataKey, user.getUid());
            recordingListView.setLayoutManager(new LinearLayoutManager(this));
            recordingListView.setAdapter(recListAdapter);
            listInitialized = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults){
        switch (requestCode){
            case REQUEST_CODE_WRITE_EXTERNAL: {
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    initialRecording();
                }
                else {
                    finish();
                }
            }
        }
    }

    @OnClick(R.id.folderChooser)
    void chooseFolder(){
        Intent intent = new Intent(this, SelectFolderActivity.class);
        startActivityForResult(intent, SELECT_FOLDER_RESULT_CODE);
    }

    @OnClick(R.id.startRecordButton)
    void initialRecording(){
        if(appConfig.getStorageWriteable()){
//            if(TextUtils.isEmpty(dataKey)){
//                makeOnlineData();
//            }

            TransitionManager.beginDelayedTransition(recorderParent, slide);
            btnStartRecord.setVisibility(View.GONE);
            btnPauseRecord.setVisibility(View.VISIBLE);
            btnStopRecord.setVisibility(View.VISIBLE);

//            iconRecordingState.tint

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            mRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() +
//                    Calendar.getInstance().getTimeInMillis() + ".aac");
            File dir = new File(appConfig.getLocalDir());
//            mFileName = dir.getPath() + File.separator + Calendar.getInstance().getTimeInMillis() + ".aac";
            mFileName = Calendar.getInstance().getTimeInMillis() + ".aac";
            if(dir.exists() && dir.isDirectory()){
                Log.d("Recording", "initialRecording: saving file to " + dir.getAbsolutePath());
                mRecorder.setOutputFile(dir.getPath() + File.separator + mFileName);
            }
            else{
                if(dir.mkdir())
                    mRecorder.setOutputFile(dir.getPath() + File.separator + mFileName);
            }

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mRecorder.start();

            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
//            btnStopRecord.setBackgroundTintList(getResources().getColor(R.color.colorAccent));
        }
        else{
            // Storage not accessible
            Log.e("VoiceRecorder","Storage not available");
            finish();
        }
    }

//    @OnClick(R.id.buttonPlay)
//    void buttonPlay(){
//        playRecording(0);
//    }

//    private void playRecording(int pos){
//        mPlayer = new MediaPlayer();
//        if(!TextUtils.isEmpty(mFileName.get(pos))){
//            try {
//                mPlayer.setDataSource(mFileName.get(pos));
//                mPlayer.prepare();
//                mPlayer.start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void stopPlaying(){
//        mPlayer.release();
//        mPlayer = null;
//    }

    private void stopRecording() {
        if(mRecorder != null){
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            TransitionManager.beginDelayedTransition(recorderParent, slide);
            btnStartRecord.setVisibility(View.VISIBLE);
            btnPauseRecord.setVisibility(View.GONE);
            btnStopRecord.setVisibility(View.GONE);

            uploadRecording(mFileName);
        }
    }

    private void uploadRecording(final String fileName){
//        savedFlag = false;
        setSavedIcon(CLOUD_NOT_SAVED);
        Uri file = Uri.fromFile(new File(appConfig.getLocalDir() + File.separator + fileName));
        final StorageReference recRef = storageRef.child("recordings/" + user.getUid() + "/" + fileName);
        UploadTask uploadTask = recRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: upload recording failed. " + e.getMessage());
            }
        }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if(!task.isSuccessful())
                    if(task.getException() != null)
                        throw task.getException();

                return recRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if(task.isSuccessful()) {
                    Uri downloadUri = task.getResult();

                    if(TextUtils.isEmpty(dataKey)){
//                        dataKey = mDatabase.child("notes/" + user.getUid()).push().getKey();
                        makeOnlineData();
//                        saveData();
//                        initializeListView();
                    }
                    writeToDatabase(downloadUri, fileName);
                }
                else{
                    Log.e(TAG, "onFailure: upload recording failed. Unknown.");
                }
            }
        });
    }

    private void writeToDatabase(Uri downloadUri, String fileName){
        String key = mDatabase.child("notes/" + user.getUid() + "/" + dataKey + "/recordings").push().getKey();
        Recording rec = new Recording(fileName, downloadUri.toString());
        Map<String, Object> recValue = rec.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("notes/" + user.getUid() + "/" + dataKey + "/recordings/" + key, recValue);

        mDatabase.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: upload recording success.");
//                savedFlag = true;
                setSavedIcon(CLOUD_SAVED);
                Toast.makeText(getApplicationContext(), "Recording saved", Toast.LENGTH_SHORT).show();
//                saveData();
                initializeListView();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: write recording data failed. " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Failed to save recording", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                timerHandler.removeCallbacks(timerRunnable);
                stopRecording();
                if(recListAdapter != null)
                    recListAdapter.stopPlaying();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        timerHandler.removeCallbacks(timerRunnable);
        stopRecording();
        if(recListAdapter != null)
            recListAdapter.stopPlaying();
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if(recListAdapter != null)
            recListAdapter.stopPlaying();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(recListAdapter != null)
            recListAdapter.stopPlaying();
    }

    @OnClick(R.id.stopRecordButton)
    void btnStopRecording(){
        timerHandler.removeCallbacks(timerRunnable);
        stopRecording();
//        mRecorder.stop();
//        btnStopRecord.setDrawingCacheBackgroundColor(getResources().getColor(R.color.grey));
        Log.d("VoiceRecorder", "Stop recording!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SELECT_FOLDER_RESULT_CODE){
            if(resultCode == RESULT_OK){
                changeFolder(
                        data.getStringExtra("folderKey"),
                        data.getStringExtra("folderName")
                );
            }
        }
    }

    private void changeFolder(final String key, final String name){
        setSavedIcon(CLOUD_NOT_SAVED);
        mDatabase.child("folders/"+ user.getUid() +"/" + folderKey + "/noteList/" + dataKey).removeValue();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("notes/"+ user.getUid() +"/" + dataKey + "/folderKey", key);
        childUpdates.put("folders/"+ user.getUid() +"/" + key + "/noteList/" + dataKey, true);
        if(key.equals("DEFAULT"))
            childUpdates.put("folders/"+ user.getUid() +"/" + key + "/name", "DEFAULT");

        mDatabase.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                folderNameView.setText(name);
                folderKey = key;
                setSavedIcon(CLOUD_SAVED);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                setSavedIcon(CLOUD_UNAVAILABLE);
            }
        });
    }

    private final int CLOUD_NOT_SAVED = 0;
    private final int CLOUD_SAVED = 1;
    private final int CLOUD_UNAVAILABLE = 2;
    private void setSavedIcon(int status){
        ColorStateList csl = AppCompatResources.getColorStateList(this, R.color.cloud_not_saved);
        switch (status){
            case 0:
                csl = AppCompatResources.getColorStateList(this, R.color.cloud_not_saved);
                break;
            case 1:
                csl = AppCompatResources.getColorStateList(this, R.color.colorPrimary);
                break;
            case 2:
                csl = AppCompatResources.getColorStateList(this, R.color.cloud_unavailable);
                break;
        }
        cloudIcon.setImageTintList(csl);
    }
}
