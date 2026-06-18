package com.vypeensoft.contactsmscallbackup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vypeensoft.contactsmscallbackup.R;

import java.io.File;
import java.util.List;

public class BackupHistoryAdapter extends RecyclerView.Adapter<BackupHistoryAdapter.ViewHolder> {

    private final List<String> fileList;

    public BackupHistoryAdapter(List<String> fileList) {
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String fullPath = fileList.get(position);
        
        String displayName;
        String pathDetails;

        if (fullPath.contains(" (Tree: ")) {
            // It's a SAF file item description
            int idx = fullPath.indexOf(" (Tree: ");
            displayName = fullPath.substring(0, idx);
            pathDetails = fullPath.substring(idx + 1);
        } else {
            // It's an internal file path
            File file = new File(fullPath);
            displayName = file.getName();
            pathDetails = file.getParent();
        }

        holder.tvFileName.setText(displayName);
        holder.tvFilePath.setText(pathDetails);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        TextView tvFilePath;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFilePath = itemView.findViewById(R.id.tvFilePath);
        }
    }
}
