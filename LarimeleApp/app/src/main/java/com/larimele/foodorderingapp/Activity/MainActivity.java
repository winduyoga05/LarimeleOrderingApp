package com.larimele.foodorderingapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.larimele.foodorderingapp.Adapter.BestFoodAdapter;
import com.larimele.foodorderingapp.Adapter.CategoryAdapter;
import com.larimele.foodorderingapp.Domain.Category;
import com.larimele.foodorderingapp.Domain.Foods;
import com.larimele.foodorderingapp.Domain.Location;
import com.larimele.foodorderingapp.Domain.Price;
import com.larimele.foodorderingapp.Domain.Time;
import com.larimele.foodorderingapp.R;
import com.larimele.foodorderingapp.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private ArrayList<Location> locationList;
    private ArrayList<Time> timeList;
    private ArrayList<Price> priceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initLocation();
        initTime();
        initPrice();
        initBestFood();
        initCategory();
        setVariable();
    }

    private void setVariable() {
        binding.logoutBtn.setOnClickListener(v -> {
            // Sign out dari Firebase
            FirebaseAuth.getInstance().signOut();

            // Sign out dari Google Sign-In
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build());
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Arahkan ke LoginActivity dan bersihkan tumpukan aktivitas
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Pastikan MainActivity ditutup
            });
        });

        binding.searchBtn.setOnClickListener(v -> {
            String text = binding.searchEdit.getText().toString().trim();
            if (!text.isEmpty()) {
                // Ubah teks pencarian ke huruf kecil untuk case-insensitive
                text = text.toLowerCase();
                Intent intent = new Intent(MainActivity.this, ListFoodsActivity.class);
                intent.putExtra("text", text);
                intent.putExtra("isSearch", true);
                startActivity(intent);
            }
        });

        binding.cartBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CartActivity.class)));

        binding.textView12.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListFoodsActivity.class);
            intent.putExtra("showAll", true);
            startActivity(intent);
        });

        // Tambahkan ini untuk tombol filter
        binding.filterBtn.setOnClickListener(v -> {
            String selectedLocationText = binding.locationSp.getSelectedItem().toString();
            String selectedTimeText = binding.timeSp.getSelectedItem().toString();
            String selectedPriceText = binding.priceSp.getSelectedItem().toString();

            // Tentukan ID berdasarkan pilihan, gunakan -1 untuk "-"
            int locationId = "-".equals(selectedLocationText) ? -1 : findLocationId(selectedLocationText);
            int timeId = "-".equals(selectedTimeText) ? -1 : findTimeId(selectedTimeText);
            int priceId = "-".equals(selectedPriceText) ? -1 : findPriceId(selectedPriceText);

            Intent intent = new Intent(MainActivity.this, ListFoodsActivity.class);
            intent.putExtra("CategoryId", 0); // 0 berarti semua kategori
            intent.putExtra("CategoryName", "Filter Results");
            intent.putExtra("locationId", locationId);
            intent.putExtra("timeId", timeId);
            intent.putExtra("priceId", priceId);
            startActivity(intent);
        });
    }

    // Bagian lainnya tetap tidak diubah
    private int findLocationId(String locationText) {
        for (Location location : locationList) {
            if (location.toString().equals(locationText)) {
                return location.getId();
            }
        }
        return -1; // Default jika tidak ditemukan
    }

    private int findTimeId(String timeText) {
        for (Time time : timeList) {
            if (time.toString().equals(timeText)) {
                return time.getId();
            }
        }
        return -1; // Default jika tidak ditemukan
    }

    private int findPriceId(String priceText) {
        for (Price price : priceList) {
            if (price.toString().equals(priceText)) {
                return price.getId();
            }
        }
        return -1; // Default jika tidak ditemukan
    }

    private void initBestFood() {
        DatabaseReference myRef = database.getReference("Foods");
        binding.progressBarBestFood.setVisibility(View.VISIBLE);
        ArrayList<Foods> list = new ArrayList<>();
        Query query = myRef.orderByChild("BestFood").equalTo(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        list.add(issue.getValue(Foods.class));
                    }
                    if(!list.isEmpty()) {
                        binding.bestFoodView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        RecyclerView.Adapter<BestFoodAdapter.viewHolder> adapter = new BestFoodAdapter(list);
                        binding.bestFoodView.setAdapter(adapter);
                    }
                    binding.progressBarBestFood.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initCategory() {
        DatabaseReference myRef = database.getReference("Category");
        binding.progressBarCategory.setVisibility(View.VISIBLE);
        ArrayList<Category> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        list.add(issue.getValue(Category.class));
                    }
                    if(!list.isEmpty()) {
                        binding.categoryView.setLayoutManager(new GridLayoutManager(MainActivity.this, 4));
                        RecyclerView.Adapter<CategoryAdapter.viewHolder> adapter = new CategoryAdapter(list);
                        binding.categoryView.setAdapter(adapter);
                    }
                    binding.progressBarCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initLocation() {
        DatabaseReference myRef = database.getReference("Location");
        locationList = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        locationList.add(issue.getValue(Location.class));
                    }
                    ArrayList<String> locationNames = new ArrayList<>();
                    locationNames.add("All Location"); // Opsi default
                    for (Location location : locationList) {
                        locationNames.add(location.toString());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, locationNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.locationSp.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initTime() {
        DatabaseReference myRef = database.getReference("Time");
        timeList = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        timeList.add(issue.getValue(Time.class));
                    }
                    ArrayList<String> timeNames = new ArrayList<>();
                    timeNames.add("All Time"); // Opsi default
                    for (Time time : timeList) {
                        timeNames.add(time.toString());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, timeNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.timeSp.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initPrice() {
        DatabaseReference myRef = database.getReference("Price");
        priceList = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot issue : snapshot.getChildren()) {
                        priceList.add(issue.getValue(Price.class));
                    }
                    ArrayList<String> priceNames = new ArrayList<>();
                    priceNames.add("All Price"); // Opsi default
                    for (Price price : priceList) {
                        priceNames.add(price.toString());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, priceNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.priceSp.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}