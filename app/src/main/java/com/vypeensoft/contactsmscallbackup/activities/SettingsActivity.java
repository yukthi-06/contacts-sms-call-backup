package com.vypeensoft.contactsmscallbackup.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

import com.vypeensoft.contactsmscallbackup.R;
import com.vypeensoft.contactsmscallbackup.storage.StorageManager;

public class SettingsActivity extends AppCompatActivity {

    private StorageManager storage;
    private TextView tvCurrentPath;

    // SAF directory picking launcher
    private final ActivityResultLauncher<Uri> openDocumentTreeLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    storage.persistUriPermission(uri);
                    updatePathUI();
                } else {
                    Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        storage = StorageManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }

        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        MaterialButton btnChangeFolder = findViewById(R.id.btnChangeFolder);
        MaterialButton btnSaveSettings = findViewById(R.id.btnSaveSettings);

        updatePathUI();

        btnChangeFolder.setOnClickListener(v -> openDocumentTreeLauncher.launch(null));
        btnSaveSettings.setOnClickListener(v -> {
            storage.saveSettings();
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updatePathUI() {
        tvCurrentPath.setText(storage.getBackupFolderDescription());
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
