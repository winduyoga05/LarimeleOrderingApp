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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.larimele.foodorderingapp.Adapter.CartAdapter;
import com.larimele.foodorderingapp.R;
import com.larimele.foodorderingapp.databinding.ActivityCartBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CartActivity extends BaseActivity {
    private static final String TAG = "CartActivity"; // Tag untuk logging
    private ActivityCartBinding binding;
    private RecyclerView.Adapter adapter;
    private double tax;
    private DatabaseReference cartRef; // Tambahkan variabel untuk menyimpan referensi
    private ValueEventListener cartListener; // Tambahkan variabel untuk menyimpan listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setVariable();
        calculateCart();
        initList();
    }

    private void initList() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in");
            return;
        }
        String userId = currentUser.getUid();
        cartRef = database.getReference("Cart").child(userId); // Ubah dari "user_1" ke userId
        Log.d(TAG, "Loading cart for user: " + userId + ", path: Cart/" + userId);
        binding.progressBar.setVisibility(View.VISIBLE);
        ArrayList<HashMap<String, Object>> cartList = new ArrayList<>();

        cartListener = cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        HashMap<String, Object> item = (HashMap<String, Object>) issue.getValue();
                        item.put("itemId", issue.getKey());
                        cartList.add(item);
                    }
                }

                binding.progressBar.setVisibility(View.GONE);
                if (cartList.isEmpty()) {
                    binding.emptyTxt.setVisibility(View.VISIBLE);
                    binding.scrollviewCart.setVisibility(View.GONE);
                } else {
                    binding.emptyTxt.setVisibility(View.GONE);
                    binding.scrollviewCart.setVisibility(View.VISIBLE);
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(CartActivity.this, LinearLayoutManager.VERTICAL, false);
                    binding.cardView.setLayoutManager(linearLayoutManager);
                    adapter = new CartAdapter(cartList);
                    binding.cardView.setAdapter(adapter);
                    calculateCart(); // Perbarui total setelah data dimuat
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading cart: " + error.getMessage());
                // Hapus Toast untuk mencegah notifikasi saat logout
                // Toast.makeText(CartActivity.this, "Error loading cart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateCart() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in");
            return;
        }
        String userId = currentUser.getUid();
        DatabaseReference cartRef = database.getReference("Cart").child(userId); // Ubah dari "user_1" ke userId
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalFee = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        HashMap<String, Object> item = (HashMap<String, Object>) issue.getValue();
                        double price = Double.parseDouble(item.get("price").toString());
                        int quantity = Integer.parseInt(item.get("quantity").toString());
                        totalFee += price * quantity;
                    }
                }

                double percentTax = 0.02; // 2% Tax
                double delivery = 10; // $10.00 delivery

                tax = Math.round(totalFee * percentTax * 100.0) / 100.0;

                double total = Math.round((totalFee + tax + delivery) * 100.0) / 100.0;
                double itemTotal = Math.round(totalFee * 100.0) / 100.0;

                binding.totalFeeTxt.setText("$" + itemTotal);
                binding.deliveryTxt.setText("$" + delivery);
                binding.taxTxt.setText("$" + tax);
                binding.totalTxt.setText("$" + total);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Error calculating cart", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error calculating cart: " + error.getMessage());
            }
        });
    }

    private void setVariable() {
        binding.backBtn.setOnClickListener(v -> finish());

        // Tambahkan listener untuk tombol Place Order
        binding.button2.setOnClickListener(v -> {
            Log.d(TAG, "Place Order button clicked");
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "User not logged in");
                return;
            }
            String userId = currentUser.getUid();
            DatabaseReference cartRef = database.getReference("Cart").child(userId); // Ubah dari "user_1" ke userId
            cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "Cart is not empty, proceeding to place order");
                        placeOrder();
                    } else {
                        Toast.makeText(CartActivity.this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Cart is empty");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(CartActivity.this, "Failed to check cart: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to check cart: " + error.getMessage());
                }
            });
        });
    }

    private void placeOrder() {
        Log.d(TAG, "placeOrder() started");
        // Dapatkan user yang sedang login
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User not logged in");
            return;
        }
        String userId = currentUser.getUid();
        Log.d(TAG, "Current user ID: " + userId);

        // Buat reference ke database
        DatabaseReference ordersRef = database.getReference("Orders");
        String orderId = ordersRef.push().getKey();
        if (orderId == null) {
            Toast.makeText(this, "Failed to generate order ID", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to generate order ID");
            return;
        }
        Log.d(TAG, "Order ID generated: " + orderId);

        // Ambil data dari Cart
        DatabaseReference cartRef = database.getReference("Cart").child(userId); // Ubah dari "user_1" ke userId
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Cart snapshot retrieved: " + snapshot.exists());
                if (snapshot.exists()) {
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("userId", userId);
                    orderData.put("items", snapshot.getValue());
                    orderData.put("subtotal", calculateSubtotal(snapshot));
                    orderData.put("tax", tax);
                    orderData.put("deliveryFee", 10);
                    orderData.put("total", calculateTotal(snapshot));
                    orderData.put("timestamp", System.currentTimeMillis());
                    orderData.put("status", "pending");

                    Log.d(TAG, "Saving order to Orders/" + orderId);
                    ordersRef.child(orderId).setValue(orderData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Order saved successfully");
                                // Berhasil menyimpan pesanan, hapus keranjang
                                cartRef.removeValue()
                                        .addOnSuccessListener(aVoid2 -> {
                                            Log.d(TAG, "Cart cleared successfully for user: " + userId);
                                            Toast.makeText(CartActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                                            finish(); // Kembali ke halaman sebelumnya
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to clear cart: " + e.getMessage());
                                            Toast.makeText(CartActivity.this, "Failed to clear cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to place order: " + e.getMessage());
                                Toast.makeText(CartActivity.this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Log.d(TAG, "Cart snapshot does not exist");
                    Toast.makeText(CartActivity.this, "Cart is empty", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error placing order: " + error.getMessage());
                Toast.makeText(CartActivity.this, "Error placing order: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double calculateSubtotal(DataSnapshot snapshot) {
        double subtotal = 0;
        for (DataSnapshot issue : snapshot.getChildren()) {
            HashMap<String, Object> item = (HashMap<String, Object>) issue.getValue();
            double price = Double.parseDouble(item.get("price").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            subtotal += price * quantity;
        }
        return Math.round(subtotal * 100.0) / 100.0;
    }

    private double calculateTotal(DataSnapshot snapshot) {
        double subtotal = calculateSubtotal(snapshot);
        return Math.round((subtotal + tax + 10) * 100.0) / 100.0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan listener saat aktivitas dihancurkan
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
    }
}