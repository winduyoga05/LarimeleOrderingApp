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
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.larimele.foodorderingapp.Activity.DetailActivity;
import com.larimele.foodorderingapp.Domain.Foods;
import com.larimele.foodorderingapp.R;

import java.util.ArrayList;

public class BestFoodAdapter extends RecyclerView.Adapter<BestFoodAdapter.viewHolder> {
    ArrayList<Foods> items;
    Context context;

    public BestFoodAdapter(ArrayList<Foods> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public BestFoodAdapter.viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View inflate = LayoutInflater.from(context).inflate(R.layout.viewholder_best_deal, parent, false);
        return new viewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull BestFoodAdapter.viewHolder holder, int position) {
        holder.titleTxt.setText(items.get(position).getTitle());
        holder.priceTxt.setText("$" + items.get(position).getPrice());
        holder.timeTxt.setText(items.get(position).getTimeValue() + " min");
        holder.starTxt.setText("" + items.get(position).getStar());

        try {
            String imagePath = items.get(position).getImagePath();
            Log.d("BestFoodAdapter", "Loading image: " + imagePath + " for " + items.get(position).getTitle());
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
                        .transform(new CenterCrop(), new RoundedCorners(30))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_info_details)
                        .into(holder.pic);
            } else {
                Log.e("BestFoodAdapter", "Image not found: " + imagePath);
                Glide.with(context)
                        .load(android.R.drawable.ic_menu_info_details)
                        .into(holder.pic);
            }
        } catch (Exception e) {
            Log.e("BestFoodAdapter", "Error loading image", e);
            Glide.with(context)
                    .load(android.R.drawable.ic_menu_info_details)
                    .into(holder.pic);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("object", items.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt, priceTxt, starTxt, timeTxt;
        ImageView pic;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            priceTxt = itemView.findViewById(R.id.priceTxt);
            starTxt = itemView.findViewById(R.id.starTxt);
            timeTxt = itemView.findViewById(R.id.timeTxt);
            pic = itemView.findViewById(R.id.pic);
        }
    }
}