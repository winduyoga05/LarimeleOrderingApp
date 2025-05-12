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
import com.bumptech.glide.request.RequestOptions;
import com.larimele.foodorderingapp.Activity.DetailActivity;
import com.larimele.foodorderingapp.Domain.Foods;
import com.larimele.foodorderingapp.R;

import java.util.ArrayList;

public class FoodListAdapter extends RecyclerView.Adapter<FoodListAdapter.viewholder> {
    ArrayList<Foods> items;
    Context context;

    public FoodListAdapter(ArrayList<Foods> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View inflate = LayoutInflater.from(context).inflate(R.layout.viewholder_list_food, parent, false);
        return new viewholder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, int position) {
        // Nonaktifkan recycling untuk mencegah gambar tertukar
        holder.setIsRecyclable(false);

        holder.titleTxt.setText(items.get(position).getTitle());
        holder.priceTxt.setText("$" + items.get(position).getPrice());
        holder.rateTxt.setText("" + items.get(position).getStar());
        holder.timeTxt.setText(items.get(position).getTimeValue() + " min");

        try {
            String imagePath = items.get(position).getImagePath();
            Log.d("FoodListAdapter", "Loading image for position " + position + ": " + imagePath + " for " + items.get(position).getTitle());
            int drawableResourceId = context.getResources().getIdentifier(
                    imagePath,
                    "drawable",
                    context.getPackageName()
            );

            // Reset ImageView sebelum memuat gambar baru
            holder.pic.setImageDrawable(null);

            if (drawableResourceId != 0) {
                RequestOptions options = new RequestOptions()
                        .transform(new CenterCrop(), new RoundedCorners(30))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_info_details)
                        .skipMemoryCache(true) // Skip cache untuk memastikan gambar selalu dimuat ulang
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE); // Nonaktifkan disk cache

                Glide.with(context)
                        .load(drawableResourceId)
                        .apply(options)
                        .into(holder.pic);
            } else {
                Log.e("FoodListAdapter", "Image not found for position " + position + ": " + imagePath);
                Glide.with(context)
                        .load(android.R.drawable.ic_menu_info_details)
                        .into(holder.pic);
            }
        } catch (Exception e) {
            Log.e("FoodListAdapter", "Error loading image at position " + position + " for " + items.get(position).getTitle(), e);
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

    public class viewholder extends RecyclerView.ViewHolder {
        TextView titleTxt, priceTxt, rateTxt, timeTxt;
        ImageView pic;

        public viewholder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            priceTxt = itemView.findViewById(R.id.priceTxt);
            rateTxt = itemView.findViewById(R.id.rateTxt);
            timeTxt = itemView.findViewById(R.id.timeTxt);
            pic = itemView.findViewById(R.id.img);
        }
    }
}