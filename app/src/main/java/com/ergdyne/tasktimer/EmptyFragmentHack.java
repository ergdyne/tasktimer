package com.ergdyne.tasktimer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by j on 3/29/17.
 */

public class EmptyFragmentHack extends Fragment{
    /**********************/
    //This is what it says it is
    //Built to make the swapping fragment to navigate stuff work.
    /**********************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.empty_hack, container,false);
    }


}
