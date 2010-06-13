package edu.calpoly.android.amclark;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class T9Overlay extends Activity {
    private View b1, b2, b3, r1, r2, r3, r4;
    private final String[] LETTERS = {
            "[1[^\\w]]", "[abc2]", "[def3]",
            "[ghi4]", "[jkl5]", "[mno6]",
            "[pqrs7]", "[tuv8]", "[wxyz9]",
            null, "[ 0]", null};
    private final long[] PATTERN = {0, 70, 50, 70, 50, 70};
    
    private ArrayList<Integer> entered;
    private String regex;
    private boolean wasOff;
    public static final String STANDARD = "/system/media/audio/ui/KeypressStandard.ogg";
    public static final String SPACEBAR = "/system/media/audio/ui/KeypressSpacebar.ogg";
    public static final String RETURN = "/system/media/audio/ui/KeypressReturn.ogg";
    public static final String DELETE = "/system/media/audio/ui/KeypressDelete.ogg";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t9layout);
        
        b1 = findViewById(R.id.b1); b2 = findViewById(R.id.b2); b3 = findViewById(R.id.b3);
        r1 = findViewById(R.id.r1); r2 = findViewById(R.id.r2);
        r3 = findViewById(R.id.r3); r4 = findViewById(R.id.r4);
        entered = new ArrayList<Integer>();
        regex = "";
        wasOff = true;
        
        findViewById(R.id.parent).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                int row = -1;
                int column = -1;
                if (y < r1.getBottom() - 5) row = 0;
                else if (y > r2.getTop() + 5 && y < r2.getBottom() - 5) row = 1;
                else if (y > r3.getTop() + 5 && y < r3.getBottom() - 5) row = 2;
                else if (y > r4.getTop() + 5) row = 3;
                if (row != -1) {
                    if (x < b1.getRight() - 5) column = 0;
                    else if (x > b2.getLeft() + 5 && x < b2.getRight() - 5) column = 1;
                    else if (x > b3.getLeft() + 5) column = 2;
                    if (column != -1) {
                        int index = (row * 3) + column;
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            wasOff = true;
                            String letters = LETTERS[index];
                            MediaPlayer mp = new MediaPlayer();
                            try {
                                mp.setDataSource(STANDARD);
                                mp.prepare();
                                mp.start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (letters != null) {
                                entered.add(index);
                                regex += LETTERS[index];
                            }
                            else {
                                if (index == 11 && entered.size() > 0) {
                                    entered.remove(entered.size() - 1);
                                    regex = "";
                                    for (int i : entered) {
                                        regex += LETTERS[i];
                                    }
                                }
                                else if (index == 9) {
                                    if (entered.size() > 0) {
                                        Intent intent = new Intent();
                                        intent.putExtra(ContactsListActivity.T9_RESULT, regex);
                                        setResult(RESULT_OK, intent);
                                    }
                                    else setResult(RESULT_CANCELED);
                                    finish();
                                }
                            }
                        }
                        else if (index == 4 && wasOff) {
                            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(PATTERN, -1);
                            wasOff = false;
                        }
                        else if (index != 4) wasOff = true;
                    }
                    else {
                        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(100);
                    }
                }
                else {
                    wasOff = true;
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(100);
                }
                return true;
            }
        });
    }
}
