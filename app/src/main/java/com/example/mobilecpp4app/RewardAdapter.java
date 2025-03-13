package com.example.mobilecpp4app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.ViewHolder> {
    private List<RewardItem> rewardList;

    public RewardAdapter(List<RewardItem> rewardList) {
        this.rewardList = rewardList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RewardItem item = rewardList.get(position);
        holder.itemName.setText(item.getName());
        holder.itemPoints.setText("Need " + item.getPoints() + " Points");
        holder.itemImage.setImageResource(item.getImageResource());
    }

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemPoints;
        ImageView itemImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemPoints = itemView.findViewById(R.id.item_points);
            itemImage = itemView.findViewById(R.id.item_image);
        }
    }
}