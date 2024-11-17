package com.example.project8_2;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<ImageData> imageList;
    private MainActivity mainActivity;

    public ImageAdapter(List<ImageData> imageList, MainActivity mainActivity) {
        this.imageList = imageList;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageData imageData = imageList.get(position);
        holder.imageView.setImageBitmap(imageData.getImage());
        holder.titleTextView.setText(imageData.getTitle());

        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(mainActivity)
                    .setTitle("삭제 확인")
                    .setMessage("이 게시물을 삭제하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> mainActivity.deletePost(imageData.getId(), position))  // ID 전달
                    .setNegativeButton("아니요", null)
                    .show();
        });

        holder.titleTextView.setOnClickListener(v -> {
            new AlertDialog.Builder(mainActivity)
                    .setTitle(imageData.getTitle())
                    .setMessage(imageData.getText())
                    .setPositiveButton("닫기", null)
                    .show();
        });
    }


    @Override
    public int getItemCount() {
        return imageList.size();
    }

    // 특정 아이템을 목록에서 제거하는 메서드
    public void removeItem(int position) {
        imageList.remove(position);
        notifyItemRemoved(position);
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        Button deleteButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            deleteButton = itemView.findViewById(R.id.deleteButton); // 삭제 버튼 추가
        }
    }
}
