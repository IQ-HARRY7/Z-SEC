package com.iq.zsec.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.iq.zsec.R;
import com.iq.zsec.db.FileRecord;
import com.iq.zsec.utils.FileUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

    public interface FileActionListener {
        void onViewFile(FileRecord record);
        void onDeleteFile(FileRecord record);
        void onSelectionChanged(List<FileRecord> selected);
    }

    private List<FileRecord>      data          = new ArrayList<FileRecord>();
    private Set<String>           selectedIds   = new HashSet<String>();
    private boolean               selectionMode = false;
    private final FileActionListener listener;
    private final SimpleDateFormat   sdf =
	new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public FileAdapter(FileActionListener listener) {
        this.listener = listener;
    }

    public void setFiles(List<FileRecord> files) {
        this.data = files;
        notifyDataSetChanged();
    }

    /** Exits multi-select mode and clears selection. */
    public void clearSelection() {
        selectionMode = false;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isInSelectionMode() {
        return selectionMode;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.item_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int pos) {
        final FileRecord r = data.get(pos);
        h.tvName.setText(r.getFileName());
        h.tvMeta.setText(
            FileUtils.formatSize(r.getFileSize()) + "  •  " + r.getMimeType());
        h.tvDate.setText(sdf.format(new Date(r.getDateAdded())));

        // Checkbox visibility + state
        boolean checked = selectedIds.contains(r.getStoredName());
        h.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        h.checkBox.setChecked(checked);

        // Highlight selected items
        h.itemView.setBackgroundColor(
            checked ? Color.parseColor("#1A2ECC71") : Color.parseColor("#161B22"));

        // ── Click: view (normal) or toggle (selection mode) ──────
        h.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (selectionMode) {
						toggleSelection(r);
					} else {
						listener.onViewFile(r);
					}
				}
			});

        // ── Long press: enter selection mode ─────────────────────
        h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (!selectionMode) {
						selectionMode = true;
					}
					toggleSelection(r);
					return true; // consume event
				}
			});

        // ── Delete (only shown outside selection mode) ────────────
        h.tvDelete.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
        h.tvDelete.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { listener.onDeleteFile(r); }
			});
    }

    private void toggleSelection(FileRecord r) {
        if (selectedIds.contains(r.getStoredName())) {
            selectedIds.remove(r.getStoredName());
        } else {
            selectedIds.add(r.getStoredName());
        }

        // Exit selection mode if nothing selected
        if (selectedIds.isEmpty()) selectionMode = false;

        notifyDataSetChanged();
        listener.onSelectionChanged(getSelectedRecords());
    }

    public List<FileRecord> getSelectedRecords() {
        List<FileRecord> result = new ArrayList<FileRecord>();
        for (int i = 0; i < data.size(); i++) {
            if (selectedIds.contains(data.get(i).getStoredName())) {
                result.add(data.get(i));
            }
        }
        return result;
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvName, tvMeta, tvDate, tvDelete;

        VH(View v) {
            super(v);
            checkBox = (CheckBox) v.findViewById(R.id.checkbox);
            tvName   = (TextView) v.findViewById(R.id.tv_file_name);
            tvMeta   = (TextView) v.findViewById(R.id.tv_file_meta);
            tvDate   = (TextView) v.findViewById(R.id.tv_date_added);
            tvDelete = (TextView) v.findViewById(R.id.btn_delete);
        }
    }
}
