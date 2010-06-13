package edu.calpoly.android.amclark;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemLongClickListener;

public class SongListActivity extends Activity implements OnGesturePerformedListener {
    private int mSongCount;
    private Cursor mSongCursor;
    public MusicAdapter mAdapter;
    private ListView mSongList;
    private EditText mFilter;
    private LinearLayout mButtons;
    private RelativeLayout mGestureBottom;
    private GestureOverlayView mGestureView;
    private LinearLayout mGestureDisplay;
    private Pattern mPattern;
    private ArrayList<Song> mAllSongs;
    private ArrayList<Song> mSongs;
    private ArrayList<Gesture> mGestures;
    private ArrayList<View> mGestureImages;
    private GestureLibrary mLibrary;
    private GestureAdapter mGestureAdapter;
    public static final int GET_T9 = 1;
    public static final String T9_RESULT = "t9 regex";
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        mFilter = (EditText) findViewById(R.id.filter);
        mFilter.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdapter.getFilter().filter(s);
            }
        });
        
        initCursors();
        mPattern = Pattern.compile("");
        
        mSongList = (ListView) findViewById(R.id.list);
        mSongList.setTextFilterEnabled(true);
        mAdapter = new MusicAdapter(getApplicationContext());
        mSongList.setAdapter(mAdapter);
        mSongList.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = getSongPlayerIntent(position);
                startActivity(intent);
                return true;
            }
        });
        
        mButtons = (LinearLayout) findViewById(R.id.buttons);
        mGestureBottom = (RelativeLayout) findViewById(R.id.gesturebottom);
        mGestureView = (GestureOverlayView) findViewById(R.id.gestureoverlay);
        mGestureView.addOnGesturePerformedListener(this);
        mGestureView.setEnabled(false);
        mGestureDisplay = (LinearLayout) findViewById(R.id.gesturedisplay);
        mGestureAdapter = new GestureAdapter(getApplicationContext());
        mGestures = new ArrayList<Gesture>();
        mGestureImages = new ArrayList<View>();
        
        ((Button) findViewById(R.id.gesturebutton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mGestureView.setEnabled(true);
                mFilter.setVisibility(View.GONE);
                mButtons.setVisibility(View.GONE);
                mGestureBottom.setVisibility(View.VISIBLE);
                mAdapter.getFilter().filter("");
            }
        });
        
        ((Button) findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mGestureView.setEnabled(false);
                mFilter.setVisibility(View.VISIBLE);
                mButtons.setVisibility(View.VISIBLE);
                mGestureBottom.setVisibility(View.GONE);
                mGestures.clear();
                mGestureImages.clear();
                mGestureDisplay.removeAllViews();
                mAdapter.getFilter().filter("");
            }
        });
        
        ((Button) findViewById(R.id.t9button)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), T9Overlay.class);
                startActivityForResult(intent, GET_T9);
            }
        });
        
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() != 0) {
            ((Button) findViewById(R.id.speakbutton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    startVoiceRecognitionActivity();
                }
            });
        } else {
            ((Button) findViewById(R.id.speakbutton)).setEnabled(false);
        }
        
        mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
        if (!mLibrary.load()) {
            finish();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_T9) {
            mFilter.setText("");
            if (resultCode == RESULT_OK) {
                if (data.hasExtra(T9_RESULT)) {
                    mAdapter.getFilter().filter(data.getStringExtra(T9_RESULT));
                    return;
                }
            }
            mAdapter.getFilter().filter("");
        }
        else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String regex = "";
            if (matches.size() > 0) {
                for (int i = 0; i < matches.size(); i++) {
                    if (matches.get(i).length() > 1) regex += matches.get(i) + '|';
                }
                if (!regex.equals("")) regex = regex.substring(0, regex.length() - 1);
            }
            mPattern = Pattern.compile(regex);
            mAdapter.getFilter().filter(regex);
        }
    }
    
    private void initCursors() {
        mAllSongs = new ArrayList<Song>();
        String[] proj = { MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, 
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION };
        mSongCursor = (Cursor) managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        mSongCount = mSongCursor.getCount();
        if (mSongCursor.moveToFirst()) {
            do {
                String data = mSongCursor.getString(0);
                String title = mSongCursor.getString(1);
                String artist = mSongCursor.getString(2);
                String album = mSongCursor.getString(3);
                int duration = mSongCursor.getInt(4);
                mAllSongs.add(new Song(data, title, artist, album, duration));
            } while (mSongCursor.moveToNext());
        }
        mSongs = mAllSongs;
    }
    
    private Intent getSongPlayerIntent(int position) {
        if (mSongs == null || mSongs.size() == 0) return null;
        Intent intent = new Intent(getApplicationContext(), SongPlayer.class);
        int size = mSongs.size();
        String[] titles = new String[size];
        String[] artists = new String[size];
        String[] albums = new String[size];
        String[] data = new String[size];
        int[] durations = new int[size];
        for (int i = position, j = 0; i < mSongs.size(); i++, j++) {
            Song song = mSongs.get(i);
            titles[j] = song.title;
            artists[j] = song.artist;
            albums[j] = song.album;
            data[j] = song.data;
            durations[j] = song.duration;
        }
        for (int i = 0, j = mSongs.size() - position; i < position; i++, j++) {
            Song song = mSongs.get(i);
            titles[j] = song.title;
            artists[j] = song.artist;
            albums[j] = song.album;
            data[j] = song.data;
            durations[j] = song.duration;
        }
        intent.putExtra(SongPlayer.ARTIST_KEY, artists);
        intent.putExtra(SongPlayer.SONG_KEY, titles);
        intent.putExtra(SongPlayer.ALBUM_KEY, albums);
        intent.putExtra(SongPlayer.DATA_KEY, data);
        intent.putExtra(SongPlayer.DURATION_KEY, durations);
        return intent;
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
    
    public class MusicAdapter extends BaseAdapter implements Filterable {
        private Context mContext;
        
        public MusicAdapter(Context c) {
            mContext = c;
        }
        
        @Override
        public int getCount() {
            return mSongCount;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ContentView cv;
            if (convertView == null) cv = new ContentView(mContext.getApplicationContext());
            else cv = (ContentView) convertView;
            Song song = mSongs.get(position);
            cv.setName(song.title);
            cv.setArtist(song.artist);
            cv.highlightMatch(mPattern);
            return cv;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.equals("")) {
                        mPattern = Pattern.compile("");
                        results.values = mAllSongs;
                        results.count = mAllSongs.size();
                        return results;
                    }
                    String c = constraint.toString().toLowerCase();
                    ArrayList<Song> values = new ArrayList<Song>();
                    ArrayList<Song> temp1 = new ArrayList<Song>();
                    ArrayList<Song> temp2 = new ArrayList<Song>();
                    if (c.contains("|")) {
                        String[] matches = c.split("\\|");
                        for (Song song : mAllSongs) {
                            String title = song.toString().toLowerCase();
                            for (String match: matches) {
                                if (title.contains(match)) {
                                    values.add(song);
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        for (Song song : mAllSongs) {
                            String title = song.toString().toLowerCase();
                            if (title.matches(c + ".*")) values.add(song);
                            else if (title.matches(".* " + c + ".*")) temp1.add(song);
                            else if (title.matches("[^ ]+" + c + ".*")) temp2.add(song);
                        }
                    }
                    values.addAll(temp1);
                    values.addAll(temp2);
                    results.values = values;
                    results.count = values.size();
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mPattern = Pattern.compile(constraint.toString().toLowerCase());
                    mSongCount = results.count;
                    mSongs = (ArrayList<Song>) results.values;
                    mAdapter.notifyDataSetChanged();
                }
            };
        }
    }


    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
        if (predictions.size() > 0) {
            if (gesture.getBoundingBox().centerY() > mSongList.getBottom()) {
                int i = 0;
                while (predictions.get(i).name.charAt(0) != '.' && i < 3) i++;
                if (i < 3) {
                    if (gesture.getBoundingBox().centerY() > mGestureDisplay.getTop()) {
                        if (predictions.get(i).name.contains(".clear")) {
                            mGestures.clear();
                            mGestureAdapter.notifyDataSetChanged();
                            mPattern = Pattern.compile("");
                            mAdapter.getFilter().filter("");
                        }
                        else {
                            String regex = mPattern.pattern();
                            if (!regex.equals("")) {
                                regex = regex.substring(0, regex.lastIndexOf('['));
                                mPattern = Pattern.compile(regex);
                                mAdapter.getFilter().filter(regex);
                                mGestures.remove(mGestures.size() - 1);
                                mGestureAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }
            else {
                int limit = predictions.size() < 10 ? predictions.size() : 10;
                String newChars = "";
                double maxScore = predictions.get(0).score;
                String regex = mPattern.pattern();
                for (int i = 0; i < limit; i++) {
                    double score = predictions.get(i).score;
                    if (score > 1 && score > maxScore / 2) {
                        char newChar = predictions.get(i).name.toLowerCase().charAt(0);
                        if (!newChars.contains(newChar + "")) {
                            newChars += newChar;
                        }
                    }
                }
                if (newChars.length() > 0) {
                    regex += '[' + newChars + ']';
                    mPattern = Pattern.compile(regex);
                    mAdapter.getFilter().filter(regex);
                    mGestures.add(gesture);
                    mGestureAdapter.notifyDataSetChanged();
                }
            }
        }
    }
    
    public class GestureAdapter {
        private Context mContext;

        public GestureAdapter(Context c) {
            mContext = c;
        }
        
        public int getCount() {
            return mGestures.size();
        }
        
        public void notifyDataSetChanged() {
            while (mGestureImages.size() > mGestures.size()) {
                mGestureDisplay.removeView(mGestureImages.remove(mGestureImages.size() - 1));
            }
            for (int i = 0; i < mGestureImages.size(); i++) {
                getView(i, mGestureImages.get(i), mGestureDisplay);
            }
            for (int i = mGestureImages.size(); i < mGestures.size(); i++) {
                View v = getView(i, null, mGestureDisplay);
                mGestureDisplay.addView(v);
                mGestureImages.add(v);
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv;
            if (convertView == null) iv = new ImageView(mContext.getApplicationContext());
            else iv = (ImageView) convertView;
            int size = (int) getResources().getDimension(R.dimen.gesture_thumbnail_size);
            if (mGestures.size() > 6) size = size / 2;
            int inset = (int) getResources().getDimension(R.dimen.gesture_thumbnail_inset);
            Bitmap bm = mGestures.get(position).toBitmap(size, size, inset, Color.YELLOW);
            iv.setImageBitmap(bm);
            return iv;
        }
    }
    
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (SongPlayer.mMediaPlayer != null && SongPlayer.mMediaPlayer.isPlaying()) {
            SongPlayer.mMediaPlayer.stop();
        }
    }
}