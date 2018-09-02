package com.harispraba.voicediary.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.harispraba.voicediary.CreateNoteActivity;
import com.harispraba.voicediary.R;
import com.harispraba.voicediary.model.Note;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecentViewAdapter extends RecyclerView.Adapter<RecentViewAdapter.ViewHolder> {
    private Activity mActivity;
    private ArrayList<DataSnapshot> mSnapshotList;
    private ArrayList<String> mSnapshotKeys;
    private LayoutInflater mInflater;
    private DatabaseReference mDatabase;
    private FirebaseUser user;

    public RecentViewAdapter(Activity activity){
        mActivity = activity;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mSnapshotList = new ArrayList<>();
        mSnapshotKeys = new ArrayList<>();
        user = FirebaseAuth.getInstance().getCurrentUser();

        Query recentQuery = mDatabase.child("notes/" + user.getUid()).limitToLast(10);
        recentQuery.addChildEventListener(new ChildEventListener() {
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

//                if(index > -1){
                    mSnapshotList.set(index, dataSnapshot);
                    mSnapshotKeys.set(index, key);
                    notifyItemChanged(getItemCount() - (index + 1));
//                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();
                int index = mSnapshotKeys.indexOf(key);

//                if(index > -1){
                    mSnapshotList.remove(index);
                    mSnapshotKeys.remove(index);
                    notifyItemRemoved(getItemCount() - (index + 1));
//                }
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
        @BindView(R.id.noteTitle) TextView noteTitle;
        @BindView(R.id.noteDesc) TextView noteDescription;
        @BindView(R.id.mainLayout) ConstraintLayout mainLayout;
        @BindView(R.id.folderName) TextView folderName;

        ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = mInflater.inflate(R.layout.note_preview, parent, false);

        return new ViewHolder(v);
    }

    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Note note = getItem(position);
        final int pos = position;

        String title = note.getTitle();
        String desc = note.getDescription();

        if (TextUtils.isEmpty(title))
            title = mActivity.getString(R.string.no_title);
        else
            title = note.getTitle();

        if (TextUtils.isEmpty(desc))
            desc = mActivity.getString(R.string.no_desc);
        else{
            desc = note.getDescription();
            desc = desc.replace("\n\n", " ").replace("\n", " ");
            if(desc.length() > 40)
                desc = desc.substring(0, 40) + "...";
        }

        final String folderName = "DEFAULT";
        if(note.getFolderKey() == null){ // || note.getFolderKey().equals("DEFAULT") || note.getFolderKey().isEmpty()){
//            folderName = "DEFAULT";
            holder.folderName.setText(folderName);
        }
        else{
            if(note.getFolderKey().isEmpty()){
                holder.folderName.setText(folderName);
            }
            else{
                mDatabase.child("folders/"+ user.getUid() +"/" + note.getFolderKey() + "/name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String name = dataSnapshot.getValue(String.class);
                        if(name != null)
                            holder.folderName.setText(name);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }

        holder.noteTitle.setText(title);
        holder.noteDescription.setText(desc);

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity, CreateNoteActivity.class);
                intent.putExtra("isEditNote", true);
                intent.putExtra("dataKey", mSnapshotKeys.get(getItemCount() - (pos + 1)));
//                intent.putExtra("dataKey", mSnapshotKeys.get(pos));
                intent.putExtra("folderKey", note.getFolderKey());
                intent.putExtra("folderName", holder.folderName.getText());
                intent.putExtra("title", note.getTitle());
                intent.putExtra("description", note.getDescription());
                mActivity.startActivity(intent);
            }
        });
    }

    public int getItemCount() {
        return mSnapshotList.size();
    }

    private Note getItem(int pos){
        DataSnapshot data = mSnapshotList.get(getItemCount() - (pos + 1));
//        DataSnapshot data = mSnapshotList.get(pos);
        return data.getValue(Note.class);
    }
}
