package edu.calpoly.android.amclark;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContentView extends LinearLayout {
    private TextView mC1;
    private TextView mC2;

    public ContentView(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.content, this, true);
        mC1 = (TextView) findViewById(R.id.c1);
        mC2 = (TextView) findViewById(R.id.c2);
    }
    
    public void setName(String name) {
        mC1.setText(name, TextView.BufferType.SPANNABLE);
    }
    
    public CharSequence getName() {
        return mC1.getText();
    }
    
    public void setNumber(String number) {
        mC2.setText(number);
    }
    
    public void setArtist(String artist) {
        mC2.setText(artist, TextView.BufferType.SPANNABLE);
    }

    public void highlightMatch(Pattern pattern) {
        if (pattern != null) {
            Spannable text = (Spannable) mC1.getText();
            Matcher m = pattern.matcher(text.toString().toLowerCase());
            if (m.find(0)) {
                int start = m.start();
                int end = start + m.group().length();
                text.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);
            }
        }
    }
}
