package edu.calpoly.android.amclark;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SongPlayer extends Activity implements OnClickListener {
    
    private TextView mSongArtist;
    private TextView mSongTitle;
    private TextView mSongAlbum;
    private Button mPrevButton;
    private Button mPlayPauseButton;
    private Button mNextButton;
    private ProgressBar mProgress;
    protected static MediaPlayer mMediaPlayer;
    private ArrayList<Song> mPlaylist;
    private String[] mData;
    private String[] mTitles;
    private String[] mArtists;
    private String[] mAlbums;
    private int[] mDurations;
    private int mSongIndex;
    private Handler mHandler;
    private Update mUpdate;
    
    public static final String SONG_KEY = "song";
    public static final String DATA_KEY = "data";
    public static final String ARTIST_KEY = "artist";
    public static final String ALBUM_KEY = "album";
    public static final String DURATION_KEY = "duration";
    public static final String CURRENT_KEY = "current";
    public static final int NOTIFICATION = 1;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.songplayer);
        
        Intent info = getIntent();
        mData = info.getStringArrayExtra(DATA_KEY);
        mTitles = info.getStringArrayExtra(SONG_KEY);
        mArtists = info.getStringArrayExtra(ARTIST_KEY);
        mAlbums = info.getStringArrayExtra(ALBUM_KEY);
        mDurations = info.getIntArrayExtra(DURATION_KEY);
        mPlaylist = new ArrayList<Song>();
        for (int i = 0; i < mData.length; i++) {
            mPlaylist.add(new Song(mData[i], mTitles[i], mArtists[i], mAlbums[i], mDurations[i]));
        }
        
        mSongArtist = (TextView) findViewById(R.id.song_artist);
        mSongTitle = (TextView) findViewById(R.id.song_name);
        mSongAlbum = (TextView) findViewById(R.id.song_album);
        
        mPrevButton = (Button) findViewById(R.id.previous);
        mPrevButton.setOnClickListener(this);
        
        mPlayPauseButton = (Button) findViewById(R.id.playpause);
        mPlayPauseButton.setOnClickListener(this);
        
        mNextButton = (Button) findViewById(R.id.next);
        mNextButton.setOnClickListener(this);
        
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mProgress.setProgress(0);
        
        if (mMediaPlayer == null) mMediaPlayer = new MediaPlayer();
        else mMediaPlayer.reset();
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mSongIndex++;
                if (mSongIndex == mPlaylist.size()) mSongIndex = 0;
                playSong();
            }
        });

        mSongIndex = 0;
        playSong();
        
        mHandler = new Handler();
        mUpdate = new Update();
        mHandler.post(mUpdate);
    }
    
    private class Song {
        public String data;
        public String title;
        public String artist;
        public String album;
        public int duration;
        
        public Song(String d, String t, String a, String a2, int d2) {
            data = d;
            title = t;
            artist = a;
            album = a2;
            duration = d2;
        }
        
        @Override
        public String toString() {
            return title + " " + artist;
        }
    }
    
    private void playSong() {
        Song current = mPlaylist.get(mSongIndex);
        mSongArtist.setText(current.artist);
        mSongTitle.setText(current.title);
        mSongAlbum.setText(current.album);
        mProgress.setProgress(0);
        mProgress.setMax(current.duration);
        if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(current.data);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Unable to play song", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mPrevButton)) {
            if (mMediaPlayer.getCurrentPosition() > 5000) {
                mMediaPlayer.seekTo(0);
                mProgress.setProgress(0);
            }
            mSongIndex = mSongIndex - 1;
            if (mSongIndex < 0) mSongIndex = mPlaylist.size() - 1;
            playSong();
        }
        else if (v.equals(mPlayPauseButton)) {
            if (mPlayPauseButton.getText().equals("Play")) {
                mMediaPlayer.start();
                mPlayPauseButton.setText("Pause");
            }
            else {
                mMediaPlayer.pause();
                mPlayPauseButton.setText("Play");
            }
        }
        else if (v.equals(mNextButton)) {
            mSongIndex++;
            if (mSongIndex >= mPlaylist.size()) mSongIndex = 0;
            playSong();
        }
    }
    
    /**
     * Updates the time displayed in the TextView to the current time and
     * schedules another update after a delay.
     */
    private class Update implements Runnable {
        public void run() {
            mProgress.setProgress(mMediaPlayer.getCurrentPosition());

            mHandler.postDelayed(mUpdate, 500);
        }
    }
}
