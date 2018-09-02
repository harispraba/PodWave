package com.harispraba.voicediary;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.harispraba.voicediary.authentication.LoginActivity;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null){
            initializeUI();
        }
        else{
//            mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                @Override
//                public void onComplete(@NonNull Task<AuthResult> task) {
//                    if(task.isSuccessful()){
//                        initializeUI();
//                    }
//                }
//            });
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initializeUI(){
        bottomNavigationView = findViewById(R.id.main_navigation);
        bottomNavigationView.setSelectedItemId(R.id.recent);
        switchToRecent();
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.recent:
                        switchToRecent();
                        break;

                    case R.id.folder:
                        switchToFolder();
                        break;

                    case R.id.profile:
                        switchToProfile();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    void switchToRecent(){
        getSupportActionBar().setTitle(getString(R.string.main_recent_tape));
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.content, new RecentFragment()).commit();
    }

    void switchToFolder(){
        getSupportActionBar().setTitle(getString(R.string.main_folder_view));
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.content, new FolderFragment()).commit();
    }

    void switchToProfile(){
        getSupportActionBar().setTitle(getString(R.string.main_user_profile));
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.content, new ProfileFragment()).commit();
    }
}
