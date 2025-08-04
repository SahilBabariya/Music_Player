package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.databinding.ItemSongBinding;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private final List<Song> songs;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public SongAdapter(List<Song> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSongBinding binding = ItemSongBinding.inflate(inflater, parent, false);
        return new SongViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.binding.textTitle.setText(song.title != null ? song.title : "Unknown Title");
        holder.binding.textArtist.setText(song.artist != null ? song.artist : "Unknown Artist");

        if (song.albumArt != null) {
            Glide.with(holder.binding.getRoot().getContext())
                    .load(song.albumArt)
                    .circleCrop()
                    .placeholder(R.drawable.round_music_note_24)
                    .error(R.drawable.round_music_note_24)
                    .into(holder.binding.imageAlbumArt);
        } else {
            holder.binding.imageAlbumArt.setImageResource(R.drawable.round_music_note_24);
        }
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        final ItemSongBinding binding;

        public SongViewHolder(@NonNull ItemSongBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }
}
