package com.vypeensoft.contactsmscallbackup.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import com.vypeensoft.contactsmscallbackup.R;
import com.vypeensoft.contactsmscallbackup.storage.StorageManager;
import com.vypeensoft.contactsmscallbackup.workers.CallLogBackupWorker;
import com.vypeensoft.contactsmscallbackup.workers.ContactBackupWorker;
import com.vypeensoft.contactsmscallbackup.workers.FullBackupWorker;
import com.vypeensoft.contactsmscallbackup.workers.SmsBackupWorker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawer;
    private StorageManager storageManager;

    private TextView tvSelectedFolder;
    private TextView tvStatusOperation;
    private TextView tvLastBackupTime;
    private TextView tvStatusMessage;
    private ProgressBar progressBar;

    private CheckBox cbCsv;
    private CheckBox cbJson;
    private CheckBox cbXml;

    private static final String PREFS_NAME = "BackupPrefs";
    private static final String KEY_LAST_BACKUP = "last_backup_time";

    // Permission request launcher
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) allGranted = false;
                }
                if (allGranted) {
                    Toast.makeText(this, "Permissions granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Required permissions denied. Backups may fail.", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storageManager = StorageManager.getInstance(this);

        // Permissions Check
        checkAndRequestPermissions();

        // UI Setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) {
                startActivity(new Intent(MainActivity.this, BackupHistoryActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(MainActivity.this, HelpActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        tvSelectedFolder = findViewById(R.id.tvSelectedFolder);
        tvStatusOperation = findViewById(R.id.tvStatusOperation);
        tvLastBackupTime = findViewById(R.id.tvLastBackupTime);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        progressBar = findViewById(R.id.progressBar);

        cbCsv = findViewById(R.id.cbCsv);
        cbJson = findViewById(R.id.cbJson);
        cbXml = findViewById(R.id.cbXml);

        MaterialCardView cardBackupContacts = findViewById(R.id.cardBackupContacts);
        MaterialCardView cardBackupSms = findViewById(R.id.cardBackupSms);
        MaterialCardView cardBackupCallLogs = findViewById(R.id.cardBackupCallLogs);
        MaterialCardView cardBackupAll = findViewById(R.id.cardBackupAll);

        updateDestinationUI();
        loadLastBackupTime();

        cardBackupContacts.setOnClickListener(v -> runBackup(ContactBackupWorker.class, "Contacts"));
        cardBackupSms.setOnClickListener(v -> runBackup(SmsBackupWorker.class, "SMS"));
        cardBackupCallLogs.setOnClickListener(v -> runBackup(CallLogBackupWorker.class, "CallLogs"));
        cardBackupAll.setOnClickListener(v -> runBackup(FullBackupWorker.class, "FullBackup"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDestinationUI();
    }

    private void checkAndRequestPermissions() {
        List<String> list = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.READ_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.READ_CALL_LOG);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!list.isEmpty()) {
            requestPermissionsLauncher.launch(list.toArray(new String[0]));
        }
    }

    private void updateDestinationUI() {
        String desc = storageManager.getBackupFolderDescription();
        desc = desc.replace("External SAF Path: ", "")
                   .replace("Internal Storage: ", "");
        tvSelectedFolder.setText(desc);
    }

    private void loadLastBackupTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastTime = prefs.getString(KEY_LAST_BACKUP, "Never");
        tvLastBackupTime.setText("Last Backup: " + lastTime);
    }

    private void saveLastBackupTime(String time) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_BACKUP, time).apply();
    }

    private void runBackup(Class<? extends androidx.work.ListenableWorker> workerClass, String tag) {
        boolean csv = cbCsv.isChecked();
        boolean json = cbJson.isChecked();
        boolean xml = cbXml.isChecked();

        if (!csv && !json && !xml) {
            Toast.makeText(this, "Please select at least one format.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatusOperation.setText("Current Operation: Backing up " + tag + "...");
        progressBar.setVisibility(View.VISIBLE);
        tvStatusMessage.setVisibility(View.GONE);

        Data inputData = new Data.Builder()
                .putBoolean("format_csv", csv)
                .putBoolean("format_json", json)
                .putBoolean("format_xml", xml)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(workerClass)
                .setInputData(inputData)
                .addTag(tag)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                tag,
                ExistingWorkPolicy.REPLACE,
                request
        );

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        if (workInfo.getState().isFinished()) {
                            progressBar.setVisibility(View.GONE);
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                tvStatusOperation.setText("Current Operation: Idle");
                                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                tvLastBackupTime.setText("Last Backup: " + timeStamp);
                                saveLastBackupTime(timeStamp);

                                Data output = workInfo.getOutputData();
                                int count = output.getInt("count", 0);
                                Toast.makeText(this, tag + " Backup completed! Items: " + count, Toast.LENGTH_LONG).show();
                            } else {
                                tvStatusOperation.setText("Current Operation: Failed");
                                tvStatusMessage.setText("Error: Backup failed. Check permissions/storage.");
                                tvStatusMessage.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
