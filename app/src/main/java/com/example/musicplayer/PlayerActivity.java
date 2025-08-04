package com.example.musicplayer;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.example.musicplayer.databinding.ActivityPlayerBinding;
import com.frolo.waveformseekbar.WaveformSeekBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ExoPlayer player;
    private final Handler handler = new Handler();

    private List<Song> songList = new ArrayList<>();
    private List<Song> shuffledList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();

                if (duration > 0) {
                    float percent = (float) currentPosition / duration;
                    binding.waveformSeekBar.setProgressInPercentage(percent);
                    binding.textElapsed.setText(formatTime((int) (currentPosition / 1000)));
                    binding.textDuration.setText(formatTime((int) (duration / 1000)));
                }

                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        songList = getIntent().getParcelableArrayListExtra("songList");
        currentIndex = getIntent().getIntExtra("position", 0);

        if (songList == null || songList.isEmpty()) {
            Toast.makeText(this, "No songs found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        shuffledList = new ArrayList<>(songList);

        binding.waveformSeekBar.setWaveform(createWaveForm(), true);

        initPlayer();
        playSong(currentIndex);
        setupControls();

        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void setupControls() {
        binding.ButtonPlayPause.setOnClickListener(v -> togglePlayPause());
        binding.buttonNext.setOnClickListener(v -> playNext());
        binding.buttonPrev.setOnClickListener(v -> playPrevious());
        binding.buttonShuffle.setOnClickListener(v -> toggleShuffle());
        binding.buttonRepeat.setOnClickListener(v -> toggleRepeat());

        binding.waveformSeekBar.setCallback(new WaveformSeekBar.Callback() {
            @Override
            public void onProgressChanged(WaveformSeekBar seekBar, float percent, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long seekPos = (long) (percent * duration);
                    player.seekTo(seekPos);
                    binding.textElapsed.setText(formatTime((int) (seekPos / 1000)));
                }
            }

            @Override
            public void onStartTrackingTouch(WaveformSeekBar seekBar) {
                handler.removeCallbacks(updateRunnable);
            }

            @Override
            public void onStopTrackingTouch(WaveformSeekBar seekBar) {
                handler.postDelayed(updateRunnable, 0);
            }
        });
    }

    private void toggleRepeat() {
        isRepeat = !isRepeat;
        player.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        binding.buttonRepeat.setColorFilter(isRepeat ? getColor(R.color.purple) : null);
    }

    private void toggleShuffle() {
        isShuffle = !isShuffle;
        if (isShuffle) {
            Collections.shuffle(shuffledList);
            Song currentSong = songList.get(currentIndex);
            currentIndex = shuffledList.indexOf(currentSong);
            binding.buttonShuffle.setColorFilter(getColor(R.color.purple));
        } else {
            Song currentSong = shuffledList.get(currentIndex);
            currentIndex = songList.indexOf(currentSong);
            shuffledList = new ArrayList<>(songList);
            binding.buttonShuffle.clearColorFilter();
        }
        playSong(currentIndex);
    }

    private void playPrevious() {
        List<Song> list = isShuffle ? shuffledList : songList;
        currentIndex = (currentIndex - 1 + list.size()) % list.size();
        playSong(currentIndex);
    }

    private void playNext() {
        List<Song> list = isShuffle ? shuffledList : songList;
        currentIndex = (currentIndex + 1) % list.size();
        playSong(currentIndex);
    }

    private void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
            handler.removeCallbacks(updateRunnable);
        } else {
            player.play();
            handler.postDelayed(updateRunnable, 0);
        }
        updatePlayPauseButtonIcon();
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                updatePlayPauseButtonIcon();
                if (state == Player.STATE_READY) {
                    binding.textDuration.setText(formatTime((int) (player.getDuration() / 1000)));
                    handler.postDelayed(updateRunnable, 0);
                } else if (state == Player.STATE_ENDED) {
                    playNext();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playSong(int index) {
        List<Song> list = isShuffle ? shuffledList : songList;
        Song song = list.get(index);

        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(song.data));
        player.prepare();
        player.play();

        updateUi(song);
        updatePlayPauseButtonIcon();
    }

    private void updateUi(Song song) {
        binding.textTitle.setText(song.title != null ? song.title : "Unknown Title");
        binding.textArtist.setText(song.artist != null ? song.artist : "Unknown Artist");
        setTitle(song.title);

        if (song.albumArt != null) {
            Glide.with(this)
                    .load(song.albumArt)
                    .circleCrop()
                    .placeholder(R.drawable.baseline_music_note_24)
                    .error(R.drawable.baseline_music_note_24)
                    .into(binding.imageAlbumArtPlayer);

            Glide.with(this)
                    .load(song.albumArt)
                    .apply(bitmapTransform(new BlurTransformation(25, 3)))
                    .placeholder(R.drawable.baseline_music_note_24)
                    .error(R.drawable.baseline_music_note_24)
                    .into(binding.bgAlbumArt);
        } else {
            binding.imageAlbumArtPlayer.setImageResource(R.drawable.baseline_music_note_24);
            binding.bgAlbumArt.setImageResource(R.drawable.baseline_music_note_24);
        }
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private int[] createWaveForm() {
        Random random = new Random();
        int[] waveform = new int[50];
        for (int i = 0; i < waveform.length; i++) {
            waveform[i] = 5 + random.nextInt(50);
        }
        return waveform;
    }

    private void updatePlayPauseButtonIcon() {
        binding.ButtonPlayPause.setImageResource(
                player != null && player.isPlaying()
                        ? R.drawable.baseline_pause_24
                        : R.drawable.baseline_play_arrow_24
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
