package com.harispraba.voicediary.authentication;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.harispraba.voicediary.MainActivity;
import com.harispraba.voicediary.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1234;

    @BindView(R.id.mainLayout) ScrollView mainLayout;
//    @BindView(R.id.termService) TextView termServiceButton;
//    @BindView(R.id.buttonLogin) Button buttonLogin;
//    @BindView(R.id.loadingLogin) ConstraintLayout loadingLogin;
//    @BindView(R.id.email_input) TextInputEditText emailInput;
//    @BindView(R.id.password_input) TextInputEditText passInput;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) actionBar.hide();
    }

    @Override
    protected void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
    }

    @OnClick(R.id.skipLogin)
    void skipSignIn(){
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            }
        });
    }

    @OnClick(R.id.bgGoogleLogin)
    void attemptGoogleLogin(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account, account.getDisplayName(), account.getPhotoUrl());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("Motive", "Google sign in failed", e);
                // [START_EXCLUDE]
//                updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }
//
    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, final String displayName, final Uri photoUri) {
        Log.d("Motive", "firebaseAuthWithGoogle:" + acct.getIdToken());
        // [START_EXCLUDE silent]
        showProgress(true);
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Motive", "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .setPhotoUri(photoUri)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                }
                            });
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Motive", "signInWithCredential:failure", task.getException());
//                            Snackbar.make(mainLayout, "Berhasil masuk dengan Google.", Snackbar.LENGTH_SHORT).show();
                            Toast.makeText(getApplicationContext(), "Google sign in failed.",
                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        showProgress(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]
//
//    @OnClick(R.id.buttonLogin)
//    void attemptLogin(){
//        // Reset errors.
//        emailInput.setError(null);
//        passInput.setError(null);
//
//        // Store values at the time of the login attempt.
//        String email = emailInput.getText().toString();
//        String password = passInput.getText().toString();
//
//
//        boolean cancel = false;
//        View focusView = null;
//
//        // Check for a valid password, if the user entered one.
//        if (!isPasswordValid(password)) {
//            passInput.setError(getString(R.string.error_empty_password));
//            focusView = passInput;
//            cancel = true;
//        }
//
//        // Check for a valid email address.
//        if (TextUtils.isEmpty(email)) {
//            emailInput.setError(getString(R.string.error_field_required));
//            focusView = emailInput;
//            cancel = true;
//        } else if (!isEmailValid(email)) {
//            emailInput.setError(getString(R.string.error_invalid_email));
//            focusView = emailInput;
//            cancel = true;
//        }
//
//        if (cancel) {
//            // There was an error; don't attempt login and focus the first
//            // form field with an error.
//            focusView.requestFocus();
//        } else {
//            // Show a progress spinner, and kick off a background task to
//            // perform the user login attempt.
//            showProgress(true);
//            mAuth.signInWithEmailAndPassword(email, password)
//                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<AuthResult> task) {
//                            if (task.isSuccessful()) {
//                                // Sign in success, update UI with the signed-in user's information
//                                Log.d("Motive", "signInWithEmail:success");
////                                FirebaseUser user = mAuth.getCurrentUser();
//                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
//                                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                startActivity(i);
//                            } else {
//                                // If sign in fails, display a message to the user.
//                                Log.w("Motive", "signInWithEmail:failure", task.getException());
//                                try {
//                                    throw task.getException();
////                                } catch(FirebaseAuthInvalidCredentialsException e) {
//////                                    emailInput.setError(getString(R.string.error_invalid_email));
//////                                    emailInput.requestFocus();
////                                    Snackbar.make(mainLayout, "Email dan password tidak cocok", Snackbar.LENGTH_SHORT).show();
////                                    passInput.setError("Password salah");
////                                    passInput.requestFocus();
////                                    passInput.setText("");
//                                } catch(Exception e) {
//                                    Log.e("Motive", e.getMessage());
//                                    String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
//                                    switch (errorCode) {
//                                        case "ERROR_INVALID_CREDENTIAL":
//                                            emailInput.setError(getString(R.string.error_invalid_email));
//                                            emailInput.requestFocus();
//                                            break;
//
//                                        case "ERROR_INVALID_EMAIL":
//                                            emailInput.setError(getString(R.string.error_invalid_email));
//                                            emailInput.requestFocus();
//                                            break;
//
//                                        case "ERROR_WRONG_PASSWORD":
//                                            Snackbar.make(mainLayout, "Email dan password tidak cocok", Snackbar.LENGTH_SHORT).show();
//                                            passInput.setError("Password tidak cocok");
//                                            passInput.requestFocus();
//                                            passInput.setText("");
//                                            break;
//
//                                        case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
//                                            Snackbar.make(mainLayout, "Email sudah terdaftar melalui metode lain", Snackbar.LENGTH_SHORT).show();
//                                            break;
//
//                                        case "ERROR_USER_DISABLED":
//                                            Snackbar.make(mainLayout, "User ini tidak aktif", Snackbar.LENGTH_SHORT).show();
//                                            break;
//
//                                        case "ERROR_USER_NOT_FOUND":
//                                            Snackbar.make(mainLayout, "User tidak ditemukan.", Snackbar.LENGTH_SHORT).show();
//                                            break;
//
//                                        default:
//                                            Snackbar.make(mainLayout, "Gagal masuk. Coba lagi.", Snackbar.LENGTH_SHORT).show();
//                                            break;
//                                    }
//                                }
//
//                                showProgress(false);
//                            }
//                        }
//                    });
//        }
//    }

    private void showProgress(final boolean show){
//        loadingLogin.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
//        buttonLogin.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
//        emailInput.setEnabled(!show);
//        passInput.setEnabled(!show);
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 0;
    }

//    @OnClick(R.id.signUpEmail)
//    void showSignUpUI(){
//        Intent i = new Intent(this, SignUpActivity.class);
//        startActivity(i);
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finishAffinity();
    }

//    @OnClick(R.id.forgotPassword)
//    void showResetPassUI(){
//        Intent intent = new Intent(this, ForgotPasswordActivity.class);
//        intent.putExtra("email", emailInput.getText().toString());
//        startActivity(intent);
//    }
}
