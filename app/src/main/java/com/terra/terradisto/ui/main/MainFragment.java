package com.terra.terradisto.ui.main;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.terra.terradisto.R;
import com.terra.terradisto.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {

    FragmentMainBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater, container, false);

        binding.btnConnectDisto.setOnClickListener(v -> {
            NavHostFragment.findNavController(MainFragment.this)
                    .navigate(R.id.action_mainFragment_to_connectDisto);
        });

        binding.btnSurveyDiameter.setOnClickListener(v -> {
            NavHostFragment.findNavController(MainFragment.this)
                    .navigate(R.id.action_mainFragment_to_surveyDiameterFragment);
        });

        return binding.getRoot();
    }
}