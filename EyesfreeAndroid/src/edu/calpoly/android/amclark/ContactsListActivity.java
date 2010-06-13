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
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
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

public class ContactsListActivity extends Activity implements OnGesturePerformedListener {
    private int mContactsCount;
    private Cursor mContactsCursor;
    private ContactsAdapter mAdapter;
    private ListView mContactsList;
    private EditText mFilter;
    private LinearLayout mButtons;
    private RelativeLayout mGestureBottom;
    private GestureOverlayView mGestureView;
    private LinearLayout mGestureDisplay;
    private Pattern mPattern;
    private ArrayList<Person> mAllContacts;
    private ArrayList<Person> mContacts;
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
        
        mContactsList = (ListView) findViewById(R.id.list);
        mContactsList.setTextFilterEnabled(true);
        mAdapter = new ContactsAdapter(getApplicationContext());
        mContactsList.setAdapter(mAdapter);
        mContactsList.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + mContacts.get(position).number));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        mAllContacts = new ArrayList<Person>();
        String[] proj = new String[] { People.NAME, People.NUMBER };
        String where = People.NUMBER + " <> ''";
        mContactsCursor = (Cursor) managedQuery(People.CONTENT_URI, proj, where, null, People.NAME + " ASC");
        mContactsCount = mContactsCursor.getCount();
        if (mContactsCursor.moveToFirst()) {
            do {
                String name = mContactsCursor.getString(0);
                String number = mContactsCursor.getString(1);
                mAllContacts.add(new Person(name, number));
            } while (mContactsCursor.moveToNext());
        }
        mContacts = mAllContacts;
    }
    
    private class Person {
        public String name;
        public String number;
        
        public Person(String n, String m) {
            name = n;
            number = m;
        }
        
        @Override
        public String toString() {
            return name + ": " + number;
        }
    }
    
    public class ContactsAdapter extends BaseAdapter implements Filterable {
        private Context mContext;
        
        public ContactsAdapter(Context c) {
            mContext = c;
        }
        
        @Override
        public int getCount() {
            return mContactsCount;
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
            Person person = mContacts.get(position);
            cv.setName(person.name);
            cv.setNumber(person.number);
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
                        results.values = mAllContacts;
                        results.count = mAllContacts.size();
                        return results;
                    }
                    String c = constraint.toString().toLowerCase();
                    ArrayList<Person> values = new ArrayList<Person>();
                    ArrayList<Person> temp1 = new ArrayList<Person>();
                    ArrayList<Person> temp2 = new ArrayList<Person>();
                    if (c.contains("|")) {
                        String[] matches = c.split("\\|");
                        for (Person contact : mAllContacts) {
                            String name = contact.name.toLowerCase();
                            for (String match: matches) {
                                if (name.contains(match)) {
                                    values.add(contact);
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        for (Person contact : mAllContacts) {
                            String name = contact.name.toLowerCase();
                            if (name.matches(c + ".*")) values.add(contact);
                            else if (name.matches(".* " + c + ".*")) temp1.add(contact);
                            else if (name.matches("[^ ]+" + c + ".*")) temp2.add(contact);
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
                    mContactsCount = results.count;
                    mContacts = (ArrayList<Person>) results.values;
                    mAdapter.notifyDataSetChanged();
                }
            };
        }
    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
        if (predictions.size() > 0) {
            if (gesture.getBoundingBox().centerY() > mContactsList.getBottom()) {
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
}