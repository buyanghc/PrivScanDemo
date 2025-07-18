package com.example.mobilecpp4app;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

public class RewardsFragment extends Fragment {
    private RecyclerView recyclerView;
    private RewardAdapter adapter;
    private List<RewardItem> rewardList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rewards, container, false);

        recyclerView = view.findViewById(R.id.recycler_rewards);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        rewardList = new ArrayList<>();
        rewardList.add(new RewardItem("Sports Bottle", 500, R.drawable.ic_cup));
        rewardList.add(new RewardItem("Sports Towel", 300, R.drawable.ic_towel));
        rewardList.add(new RewardItem("Mountain Pole", 800, R.drawable.ic_pole));
        rewardList.add(new RewardItem("Sports Band", 200, R.drawable.ic_band));
        rewardList.add(new RewardItem("Yoga Mat", 1000, R.drawable.ic_matyoga));
        rewardList.add(new RewardItem("Dumbbell", 1200, R.drawable.ic_dumbbell));

        adapter = new RewardAdapter(rewardList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}