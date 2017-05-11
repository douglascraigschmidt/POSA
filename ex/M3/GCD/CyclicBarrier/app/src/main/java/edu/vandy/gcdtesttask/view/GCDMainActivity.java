package edu.vandy.gcdtesttask.view;

import android.support.v4.app.Fragment;

import edu.vandy.tasktesterframeworklib.view.MainFragmentActivity;

/**
 * Main (Fragment) Activity of the application.

 * Activity that is listed in Manifest and launched by Android.
 */
public class GCDMainActivity
       extends MainFragmentActivity {
    /**
     * This sets what Fragment to use in this App.
     *
     * @return Fragment to display in the Activity.
     */
    @Override
    public Fragment setTesterFragment() {
        return new TestTaskFragment();
    }
}
