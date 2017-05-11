package edu.vandy.gcdtesttask.view;

import android.support.v4.app.Fragment;

import edu.vandy.fwklib.view.AbstractMainFragmentActivity;

/**
 * Main (Fragment) Activity of the application.

 * Activity that is listed in Manifest and launched by Android.
 */
public class GCDMainActivity
       extends AbstractMainFragmentActivity {
    /**
     * This factory method sets what Fragment to use in this app.
     *
     * @return Fragment to display in the Activity.
     */
    @Override
    public Fragment makeTesterFragment() {
        return new TestTaskFragment();
    }
}
