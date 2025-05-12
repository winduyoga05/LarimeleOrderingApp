package com.larimele.foodorderingapp.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.larimele.foodorderingapp.Adapter.FoodListAdapter;
import com.larimele.foodorderingapp.Domain.Foods;
import com.larimele.foodorderingapp.R;
import com.larimele.foodorderingapp.databinding.ActivityListFoodsBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListFoodsActivity extends BaseActivity {
    ActivityListFoodsBinding binding;
    private RecyclerView.Adapter adapterListFood;
    private int categoryId;
    private String categoryName;
    private String searchText;
    private boolean isSearch;
    private boolean showAll;
    private int locationId = -1;
    private int timeId = -1;
    private int priceId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListFoodsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getIntentExtra();
        initList();
        setVariable();
    }

    private void setVariable() {
        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void initList() {
        DatabaseReference myRef = database.getReference("Foods");
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyResultTxt.setVisibility(View.GONE);
        binding.foodListView.setVisibility(View.VISIBLE);
        ArrayList<Foods> list = new ArrayList<>();

        // Ambil semua data tanpa query filter awal
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        Foods food = issue.getValue(Foods.class);
                        if (food != null && food.getImagePath() != null) {
                            boolean matchesFilter = true;

                            // Filter berdasarkan pencarian
                            if (isSearch && searchText != null) {
                                // Pencarian case-insensitive dan partial matching
                                if (food.getTitle() == null || !food.getTitle().toLowerCase().contains(searchText)) {
                                    matchesFilter = false;
                                }
                            }

                            // Filter berdasarkan kategori
                            if (categoryId > 0) {
                                if (food.getCategoryId() != categoryId) {
                                    matchesFilter = false;
                                }
                            }

                            // Filter berdasarkan showAll
                            if (showAll) {
                                matchesFilter = true; // Tidak perlu filter tambahan
                            }

                            // Apply location filter
                            if (locationId != -1 && food.getLocationId() != locationId) {
                                matchesFilter = false;
                            }

                            // Apply time filter
                            if (timeId != -1 && food.getTimeId() != timeId) {
                                matchesFilter = false;
                            }

                            // Apply price filter
                            if (priceId != -1 && food.getPriceId() != priceId) {
                                matchesFilter = false;
                            }

                            if (matchesFilter) {
                                Log.d("ListFoodsActivity", "Adding food: " + food.getTitle() + " with ImagePath: " + food.getImagePath());
                                list.add(food);
                            }
                        } else {
                            Log.e("ListFoodsActivity", "Invalid food object or ImagePath for snapshot: " + issue.getKey());
                        }
                    }
                }

                // Urutkan list berdasarkan ID untuk konsistensi
                Collections.sort(list, new Comparator<Foods>() {
                    @Override
                    public int compare(Foods o1, Foods o2) {
                        return Integer.compare(o1.getId(), o2.getId());
                    }
                });

                Log.d("ListFoodsActivity", "Sorted list size: " + list.size());
                for (int i = 0; i < list.size(); i++) {
                    Log.d("ListFoodsActivity", "Position " + i + ": " + list.get(i).getTitle() + " with ImagePath: " + list.get(i).getImagePath());
                }

                binding.progressBar.setVisibility(View.GONE);

                if (list.isEmpty()) {
                    binding.emptyResultTxt.setVisibility(View.VISIBLE);
                    binding.foodListView.setVisibility(View.GONE);
                    Toast.makeText(ListFoodsActivity.this, "No items match your filters", Toast.LENGTH_SHORT).show();
                } else {
                    binding.emptyResultTxt.setVisibility(View.GONE);
                    binding.foodListView.setVisibility(View.VISIBLE);

                    binding.foodListView.setLayoutManager(new GridLayoutManager(ListFoodsActivity.this, 2));
                    adapterListFood = new FoodListAdapter(list);
                    binding.foodListView.setAdapter(adapterListFood);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyResultTxt.setVisibility(View.VISIBLE);
                binding.foodListView.setVisibility(View.GONE);
                Toast.makeText(ListFoodsActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getIntentExtra() {
        categoryId = getIntent().getIntExtra("CategoryId", 0);
        categoryName = getIntent().getStringExtra("Category");
        searchText = getIntent().getStringExtra("text");
        isSearch = getIntent().getBooleanExtra("isSearch", false);
        showAll = getIntent().getBooleanExtra("showAll", false);
        locationId = getIntent().getIntExtra("locationId", -1);
        timeId = getIntent().getIntExtra("timeId", -1);
        priceId = getIntent().getIntExtra("priceId", -1);

        if (showAll) {
            binding.titleTxt.setText("All Foods");
        } else if (isSearch) {
            binding.titleTxt.setText("Search Results");
        } else if (locationId != -1 || timeId != -1 || priceId != -1) {
            binding.titleTxt.setText("Filter Results");
        } else {
            binding.titleTxt.setText(categoryName);
        }
    }
}