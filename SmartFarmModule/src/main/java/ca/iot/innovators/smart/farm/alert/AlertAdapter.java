package ca.iot.innovators.smart.farm.alert;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import ca.iot.innovators.smart.farm.R;
import ca.iot.innovators.smart.farm.firestore.AlertModel;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {
    private final List<AlertModel> alerts;
    private final Context context;

    public AlertAdapter(Context context, List<AlertModel> alerts) {
        this.context = context;
        this.alerts = alerts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertModel alert = alerts.get(position);
        holder.tvTitle.setText(alert.getTitle());
        holder.tvMessage.setText(alert.getMessage());
        holder.tvStatus.setText(alert.getSeverity());
        holder.tvTime.setText(alert.getFormattedTime());

        // Dynamic Styling based on severity
        int color;
        int iconRes;
        
        String severity = alert.getSeverity() != null ? alert.getSeverity() : "Normal";
        
        switch (severity) {
            case "Critical":
                color = ContextCompat.getColor(context, R.color.status_error);
                iconRes = R.drawable.ic_alerts; // Should ideally be a critical icon
                break;
            case "Warning":
                color = ContextCompat.getColor(context, R.color.status_warning);
                iconRes = R.drawable.ic_alerts;
                break;
            default:
                color = ContextCompat.getColor(context, R.color.status_ok);
                iconRes = R.drawable.ic_alerts;
                break;
        }
        
        holder.card.setStrokeColor(color);
        holder.tvStatus.setTextColor(color);
        holder.ivIcon.setColorFilter(color);
        // holder.ivIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvTitle, tvMessage, tvStatus, tvTime;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvTitle = itemView.findViewById(R.id.alertTitle);
            tvMessage = itemView.findViewById(R.id.alertMessage);
            tvStatus = itemView.findViewById(R.id.alertStatus);
            tvTime = itemView.findViewById(R.id.alertTime);
            ivIcon = itemView.findViewById(R.id.alertIcon);
        }
    }
}
