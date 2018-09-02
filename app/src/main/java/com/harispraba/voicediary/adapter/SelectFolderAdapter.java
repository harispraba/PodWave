package com.harispraba.voicediary.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.harispraba.voicediary.NoteListActivity;
import com.harispraba.voicediary.R;
import com.harispraba.voicediary.model.Folder;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SelectFolderAdapter extends RecyclerView.Adapter<SelectFolderAdapter.ViewHolder> {
    private Activity mActivity;
    private LayoutInflater mInflater;
    private ArrayList<DataSnapshot> mSnapshotList;
    private ArrayList<String> mSnapshotKeys;

    public SelectFolderAdapter (Activity activity){
        mActivity = activity;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mSnapshotList = new ArrayList<>();
        mSnapshotKeys = new ArrayList<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference folderRef = mDatabase.child("folders/"+user.getUid());
        folderRef.addChildEventListener(new ChildEventListener() {
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

                mSnapshotList.set(index, dataSnapshot);
                mSnapshotKeys.set(index, key);
                notifyItemChanged(getItemCount() - (index + 1));
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();
                int index = mSnapshotKeys.indexOf(key);

                mSnapshotList.remove(index);
                mSnapshotKeys.remove(index);
                notifyItemRemoved(getItemCount() - (index + 1));
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.folderName) TextView folderName;
        @BindView(R.id.folderItemNum) TextView folderSize;
        @BindView(R.id.mainLayout) ConstraintLayout mainLayout;

        ViewHolder(View view){
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.select_folder_item, parent, false);
        return new ViewHolder(v);
    }

    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Folder folder = getItem(position);
        final int pos = position;

        final String name = folder.getName();
        String noteCount = "";

        if (folder.getNoteList() != null)
            noteCount = "(" + folder.getNoteList().size() + ")";
        else{
            noteCount = "(0)";
        }

        holder.folderName.setText(name);
        holder.folderSize.setText(noteCount);
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("folderName", name);
                intent.putExtra("folderKey", mSnapshotKeys.get(getItemCount() - (pos + 1)));
                mActivity.setResult(Activity.RESULT_OK, intent);
                mActivity.finish();
//                Toast.makeText(mActivity, name + " pressed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getItemCount() {
        return mSnapshotList.size();
    }

    private Folder getItem(int pos){
        DataSnapshot data = mSnapshotList.get(getItemCount() - (pos + 1));
        return data.getValue(Folder.class);
    }
}
