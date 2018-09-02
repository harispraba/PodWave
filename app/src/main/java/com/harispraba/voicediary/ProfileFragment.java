package com.harispraba.voicediary;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.harispraba.voicediary.model.CircleTransform;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {
    public final String TAG = "ProfileFragment";
    public final int GOOGLE_SIGN_IN = 3;

    @BindView(R.id.name) TextView name;
    @BindView(R.id.email) TextView email;
    @BindView(R.id.profilePic) ImageView profilePicture;
    @BindView(R.id.buttonGoogle) ConstraintLayout buttonSignIn;
    @BindView(R.id.loading) ConstraintLayout loading;
    @BindView(R.id.buttonSignOut) Button buttonSignOut;

    FirebaseUser user;
    FirebaseAuth mAuth;

    private Activity mActivity;
    private GoogleSignInClient mGoogleSignInClient;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, view);
        mActivity = getActivity();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if(user != null){
            updateUI(user);
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mActivity, gso);

        return view;
    }

    private void updateUI(@Nullable FirebaseUser user){
        if(user != null){
            if(user.isAnonymous()){
                name.setText(getResources().getString(R.string.user_anon));
                email.setText(getResources().getString(R.string.user_anon));
                buttonSignIn.setVisibility(View.VISIBLE);
//                buttonSignOut.setVisibility(View.GONE);
            }
            else{
                name.setText(user.getDisplayName());
                email.setText(user.getEmail());
                Picasso.get().load(user.getPhotoUrl())
                        .centerCrop()
                        .transform(new CircleTransform())
                        .resize(80,
                                80)
                        .into(profilePicture);
//                Picasso.get()
//                        .load("http://www.skywardimaging.com/wp-content/uploads/2015/11/default-user-image.png")
//                        .into(profilePicture);
//                buttonSignOut.setVisibility(View.VISIBLE);
                buttonSignIn.setVisibility(View.GONE);
            }
        }
    }

    private void showProgress(final boolean show){
        loading.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        buttonSignIn.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    @OnClick(R.id.bgGoogleLogin)
    void googleSignIn(){
        showProgress(true);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                linkAccount(account.getIdToken(), account.getDisplayName(), account.getPhotoUrl());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(getContext(), "Google sign in failed.",
                        Toast.LENGTH_SHORT).show();
                showProgress(false);
                // [START_EXCLUDE]
//                updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }

    private void linkAccount(String idToken, final String displayName, final Uri photoUri) {
        // Link the anonymous user to the email credential
//        showProgressDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // [START link_credential]
        user.linkWithCredential(credential)
                .addOnCompleteListener(mActivity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "linkWithCredential:success");
                            final FirebaseUser user = task.getResult().getUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .setPhotoUri(photoUri)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    showProgress(false);
                                    updateUI(user);
                                }
                            });
                        } else {
                            Log.w(TAG, "linkWithCredential:failure", task.getException());
                            if(task.getException() != null)
                                Toast.makeText(getContext(), task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    showProgress(false);
                                    updateUI(null);
                                }
                            });
                        }

                        // [START_EXCLUDE]
//                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END link_credential]
    }

    @OnClick(R.id.buttonSignOut)
    void buttonSignOut(){
        FirebaseAuth.getInstance().signOut();
        if(!user.isAnonymous()){
            mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    startActivity(intent);
                }
            });
        }
        else{
            Intent intent = new Intent(getContext(), MainActivity.class);
            startActivity(intent);
        }
    }
}
