package com.larimele.foodorderingapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.larimele.foodorderingapp.Activity.ListFoodsActivity;
import com.larimele.foodorderingapp.Domain.Category;
import com.larimele.foodorderingapp.R;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.viewHolder> {
    ArrayList<Category> items;
    Context context;

    // Array untuk menyimpan background drawable berdasarkan posisi
    private final int[] categoryBackgrounds = {
            R.drawable.cat_0_background,
            R.drawable.cat_1_background,
            R.drawable.cat_2_background,
            R.drawable.cat_3_background,
            R.drawable.cat_4_background,
            R.drawable.cat_5_background,
            R.drawable.cat_6_background,
            R.drawable.cat_7_background
    };

    public CategoryAdapter(ArrayList<Category> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CategoryAdapter.viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View inflate = LayoutInflater.from(context).inflate(R.layout.viewholder_category, parent, false);
        return new viewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.viewHolder holder, int position) {
        holder.titleTxt.setText(items.get(position).getName());

        // Atur background berdasarkan posisi kategori
        if (position < categoryBackgrounds.length) {
            holder.pic.setBackgroundResource(categoryBackgrounds[position]);
        } else {
            // Jika posisi melebihi jumlah background, gunakan background default atau yang terakhir
            holder.pic.setBackgroundResource(categoryBackgrounds[categoryBackgrounds.length - 1]);
        }

        // Muat gambar kategori menggunakan Glide
        try {
            String imagePath = items.get(position).getImagePath();
            Log.d("CategoryAdapter", "Loading image: " + imagePath + " for " + items.get(position).getName());
            int drawableResourceId = context.getResources().getIdentifier(
                    imagePath,
                    "drawable",
                    context.getPackageName()
            );

            // Reset ImageView sebelum memuat gambar baru
            holder.pic.setImageDrawable(null);

            if (drawableResourceId != 0) {
                Glide.with(context)
                        .load(drawableResourceId)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_info_details)
                        .into(holder.pic);
            } else {
                Log.e("CategoryAdapter", "Image not found: " + imagePath);
                Glide.with(context)
                        .load(android.R.drawable.ic_menu_info_details)
                        .into(holder.pic);
            }
        } catch (Exception e) {
            Log.e("CategoryAdapter", "Error loading image", e);
            Glide.with(context)
                    .load(android.R.drawable.ic_menu_info_details)
                    .into(holder.pic);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListFoodsActivity.class);
            intent.putExtra("CategoryId", items.get(position).getId());
            intent.putExtra("CategoryName", items.get(position).getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt;
        ImageView pic;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.catNameTxt);
            pic = itemView.findViewById(R.id.imgCat);
        }
    }
}