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
import com.harispraba.voicediary.CreateNoteActivity;
import com.harispraba.voicediary.R;
import com.harispraba.voicediary.model.Note;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteListAdapter extends RecyclerView.Adapter<NoteListAdapter.ViewHolder> {
    private Activity mActivity;
    private LayoutInflater mInflater;
    private ArrayList<DataSnapshot> mSnapshotList;
    private ArrayList<String> mSnapshotKeys;
    private String folderName;

    public NoteListAdapter(Activity activity, String folderName, String folderKey){
        mActivity = activity;
        this.folderName = folderName;
        mSnapshotList = new ArrayList<>();
        mSnapshotKeys = new ArrayList<>();
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            Query notesQuery = mDatabase.child("notes/" + user.getUid()).orderByChild("folderKey").equalTo(folderKey);
            notesQuery.addChildEventListener(new ChildEventListener() {
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

                    notifyItemRemoved(getItemCount() - (index + 1));
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
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.noteTitle) TextView noteTitle;
        @BindView(R.id.noteDesc) TextView noteDesc;
        @BindView(R.id.mainLayout) ConstraintLayout mainLayout;

        ViewHolder(View view){
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.note_list_item, parent, false);
        return new ViewHolder(v);
    }

    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

        holder.noteTitle.setText(title);
        holder.noteDesc.setText(desc);

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity, CreateNoteActivity.class);
                intent.putExtra("isEditNote", true);
                intent.putExtra("dataKey", mSnapshotKeys.get(getItemCount() - (pos + 1)));
//                intent.putExtra("dataKey", mSnapshotKeys.get(pos));
                intent.putExtra("folderKey", note.getFolderKey());
                intent.putExtra("folderName", folderName);
                intent.putExtra("title", note.getTitle());
                intent.putExtra("description", note.getDescription());
                mActivity.startActivity(intent);
            }
        });
    }

    public int getItemCount() {
        return mSnapshotList.size();
    }

    private Note getItem(int position){
        DataSnapshot data = mSnapshotList.get(getItemCount() - (position + 1));
        return data.getValue(Note.class);
    }
}
