package com.larimele.foodorderingapp.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.larimele.foodorderingapp.Domain.Foods;
import com.larimele.foodorderingapp.R;

import java.util.ArrayList;
import java.util.HashMap;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> items;
    private final HashMap<Integer, Foods> foodsMap = new HashMap<>();

    public CartAdapter(ArrayList<HashMap<String, Object>> items) {
        this.items = items;
        // Ambil data Foods dari Firebase saat adapter dibuat
        fetchFoodsData();
    }

    private void fetchFoodsData() {
        DatabaseReference foodsRef = FirebaseDatabase.getInstance().getReference("Foods");
        foodsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        Foods food = issue.getValue(Foods.class);
                        if (food != null) {
                            foodsMap.put(food.getId(), food);
                        }
                    }
                    // Refresh adapter setelah data diambil
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CartAdapter", "Failed to fetch Foods data: " + error.getMessage());
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Nonaktifkan recycling untuk mencegah gambar tertukar
        holder.setIsRecyclable(false);

        HashMap<String, Object> item = items.get(position);
        holder.titleTextView.setText(item.get("title").toString());
        holder.priceTextView.setText(String.format("$%.2f", Double.parseDouble(item.get("price").toString())));
        holder.quantityTextView.setText(item.get("quantity").toString());

        // Ambil foodId dan cari ImagePath dari foodsMap
        int foodId = Integer.parseInt(item.get("foodId").toString());
        Foods food = foodsMap.get(foodId);
        if (food != null && food.getImagePath() != null) {
            String imagePath = food.getImagePath();
            Log.d("CartAdapter", "Loading image for position " + position + ": " + imagePath + " for foodId: " + foodId);
            int drawableResourceId = holder.itemView.getContext().getResources().getIdentifier(
                    imagePath,
                    "drawable",
                    holder.itemView.getContext().getPackageName()
            );

            // Reset ImageView sebelum memuat gambar baru
            holder.productImage.setImageDrawable(null);

            if (drawableResourceId != 0) {
                RequestOptions options = new RequestOptions()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .skipMemoryCache(true) // Skip cache untuk memastikan gambar selalu dimuat ulang
                        .diskCacheStrategy(DiskCacheStrategy.NONE); // Nonaktifkan disk cache

                Glide.with(holder.itemView.getContext())
                        .load(drawableResourceId)
                        .apply(options)
                        .into(holder.productImage);
            } else {
                Log.e("CartAdapter", "Image not found: " + imagePath);
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.error_image)
                        .into(holder.productImage);
            }
        } else {
            Log.e("CartAdapter", "Food data not found for foodId: " + foodId);
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.error_image)
                    .into(holder.productImage);
        }

        String itemId = item.get("itemId").toString();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("CartAdapter", "User not logged in");
            return;
        }
        String userId = currentUser.getUid();

        // Tombol plus untuk menambah jumlah item
        holder.plusButton.setOnClickListener(v -> {
            int newQty = Integer.parseInt(item.get("quantity").toString()) + 1;
            DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart").child(userId).child(itemId);
            cartRef.child("quantity").setValue(newQty);
            holder.quantityTextView.setText(String.valueOf(newQty));
            item.put("quantity", newQty);
        });

        // Tombol minus untuk mengurangi jumlah item
        holder.minusButton.setOnClickListener(v -> {
            int newQty = Integer.parseInt(item.get("quantity").toString());
            if (newQty > 1) {
                newQty--;
                DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart").child(userId).child(itemId);
                cartRef.child("quantity").setValue(newQty);
                holder.quantityTextView.setText(String.valueOf(newQty));
                item.put("quantity", newQty);
            }
        });

        // Tombol hapus untuk menghapus item dari keranjang
        holder.deleteButton.setOnClickListener(v -> {
            DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("Cart").child(userId).child(itemId);
            cartRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (position >= 0 && position < items.size()) { // Tambahkan pengecekan indeks
                            items.remove(position);
                            notifyItemRemoved(position);
                            Log.d("CartAdapter", "Item deleted from Cart/" + userId + "/" + itemId);
                        } else {
                            Log.w("CartAdapter", "Invalid position " + position + " for deletion, items size: " + items.size());
                        }
                    })
                    .addOnFailureListener(e -> Log.e("CartAdapter", "Failed to delete item: " + e.getMessage()));
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView priceTextView;
        private final TextView quantityTextView;
        private final Button plusButton;
        private final Button minusButton;
        private final Button deleteButton;
        private final ImageView productImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTxt);
            priceTextView = itemView.findViewById(R.id.feeEachItem);
            quantityTextView = itemView.findViewById(R.id.numberItemTxt);
            plusButton = itemView.findViewById(R.id.plusCartBtn);
            minusButton = itemView.findViewById(R.id.minusCartBtn);
            deleteButton = itemView.findViewById(R.id.deleteBtn);
            productImage = itemView.findViewById(R.id.productImage);
        }
    }
}