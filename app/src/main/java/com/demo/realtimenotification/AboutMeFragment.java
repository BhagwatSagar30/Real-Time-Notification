package com.demo.realtimenotification;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class AboutMeFragment extends Fragment {

    public AboutMeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_about_me, container, false);

    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();

        if (activity instanceof RealTimeNotificationActivity) {

            ((RealTimeNotificationActivity) activity).changeToolbar(true);
            ((RealTimeNotificationActivity) activity).updateToolbarText(activity.getResources().getString(R.string.action_about_me));

        }
    }
}
