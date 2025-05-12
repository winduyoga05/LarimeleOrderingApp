package com.larimele.foodorderingapp.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.larimele.foodorderingapp.Domain.Foods;
import com.larimele.foodorderingapp.Helper.ManagmentCart;
import com.larimele.foodorderingapp.R;
import com.larimele.foodorderingapp.databinding.ActivityDetailBinding;

import java.util.HashMap;
import java.util.Map;

public class DetailActivity extends BaseActivity {
    ActivityDetailBinding binding;
    private Foods object;
    private int num = 1;
    private ManagmentCart managementCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getIntentExtra();
        setVariable();
    }

    private void setVariable() {
        managementCart = new ManagmentCart(this);
        binding.backBtn.setOnClickListener(v -> finish());

        try {
            if (object == null || object.getImagePath() == null || object.getImagePath().isEmpty()) {
                Log.e("DetailActivity", "Invalid food object or ImagePath for food: " + (object != null ? object.getTitle() : "null"));
                Glide.with(DetailActivity.this)
                        .load(android.R.drawable.ic_menu_info_details)
                        .into(binding.pic);
                return;
            }

            String imagePath = object.getImagePath();
            Log.d("DetailActivity", "Loading image: " + imagePath + " for " + object.getTitle());
            int drawableResourceId = getResources().getIdentifier(
                    imagePath,
                    "drawable",
                    getPackageName()
            );

            // Reset ImageView sebelum memuat gambar baru
            binding.pic.setImageDrawable(null);

            if (drawableResourceId != 0) {
                RequestOptions options = new RequestOptions()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_info_details)
                        .skipMemoryCache(true) // Skip cache untuk memastikan gambar selalu dimuat ulang
                        .diskCacheStrategy(DiskCacheStrategy.NONE); // Nonaktifkan disk cache

                Glide.with(DetailActivity.this)
                        .load(drawableResourceId)
                        .apply(options)
                        .into(binding.pic);
            } else {
                Log.e("DetailActivity", "Image not found: " + imagePath + " for " + object.getTitle());
                Glide.with(DetailActivity.this)
                        .load(android.R.drawable.ic_menu_info_details)
                        .into(binding.pic);
            }
        } catch (Exception e) {
            Log.e("DetailActivity", "Error loading image for " + (object != null ? object.getTitle() : "null"), e);
            Glide.with(DetailActivity.this)
                    .load(android.R.drawable.ic_menu_info_details)
                    .into(binding.pic);
        }

        binding.priceTxt.setText("$" + object.getPrice());
        binding.titleTxt.setText(object.getTitle());
        binding.descriptionTxt.setText(object.getDescription());
        binding.rateTxt.setText(object.getStar() + " Rating");
        binding.ratingBar.setRating((float) object.getStar());
        updateTotalPrice();

        binding.plusBtn.setOnClickListener(v -> {
            num = num + 1;
            binding.numTxt.setText(String.valueOf(num));
            updateTotalPrice();
        });

        binding.minusBtn.setOnClickListener(v -> {
            if (num > 1) {
                num = num - 1;
                binding.numTxt.setText(String.valueOf(num));
                updateTotalPrice();
            }
        });

        binding.addBtn.setOnClickListener(v -> {
            object.setNumberInCart(num);
            managementCart.insertFood(object);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(DetailActivity.this, "Please login to add to cart", Toast.LENGTH_SHORT).show();
                Log.e("AddToCart", "User not logged in");
                return;
            }
            String userId = currentUser.getUid();
            Log.d("AddToCart", "Adding item to cart for user: " + userId + ", path: Cart/" + userId);

            DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart").child(userId);
            String itemId = cartRef.push().getKey();
            if (itemId == null) {
                Log.e("AddToCart", "Failed to generate cart item ID");
                return;
            }

            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("foodId", object.getId());
            cartItem.put("title", object.getTitle());
            cartItem.put("quantity", num);
            cartItem.put("price", object.getPrice());

            cartRef.child(itemId).setValue(cartItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(DetailActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show();
                        Log.d("AddToCart", "Item added successfully to Cart/" + userId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(DetailActivity.this, "Failed to add to cart", Toast.LENGTH_SHORT).show();
                        Log.e("AddToCart", "Failed to add to cart: " + e.getMessage());
                    });
        });
    }

    private void updateTotalPrice() {
        binding.totalTxt.setText(String.format("$%.2f", num * object.getPrice()));
    }

    private void getIntentExtra() {
        object = (Foods) getIntent().getSerializableExtra("object");
        if (object == null) {
            Log.e("DetailActivity", "No food object received in intent");
            finish();
        }
    }
}