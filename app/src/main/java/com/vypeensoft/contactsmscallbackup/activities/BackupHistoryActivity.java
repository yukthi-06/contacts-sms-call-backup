package com.vypeensoft.contactsmscallbackup.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import com.vypeensoft.contactsmscallbackup.R;
import com.vypeensoft.contactsmscallbackup.adapters.BackupHistoryAdapter;
import com.vypeensoft.contactsmscallbackup.storage.StorageManager;

import java.io.File;
import java.util.List;

public class BackupHistoryActivity extends AppCompatActivity {

    private StorageManager storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_history);

        storage = StorageManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Backup History");
        }

        MaterialButton btnOpenFolder = findViewById(R.id.btnOpenFolder);
        RecyclerView rvBackupHistory = findViewById(R.id.rvBackupHistory);
        TextView tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        rvBackupHistory.setLayoutManager(new LinearLayoutManager(this));

        List<String> files = storage.listBackupFiles();
        if (files == null || files.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvBackupHistory.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvBackupHistory.setVisibility(View.VISIBLE);
            rvBackupHistory.setAdapter(new BackupHistoryAdapter(files));
        }

        btnOpenFolder.setOnClickListener(v -> {
            String destType = storage.getDestinationType();
            if (StorageManager.DEST_EXTERNAL.equals(destType) && storage.getExternalPath() != null) {
                try {
                    File folder = new File(storage.getExternalPath());
                    Uri uri = Uri.fromFile(folder);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "resource/folder");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        startActivity(Intent.createChooser(intent, "Open Folder"));
                    } catch (Exception ex) {
                        Toast.makeText(this, "Could not open folder automatically", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Internal backups are private. Select custom folder to view via File Manager.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
