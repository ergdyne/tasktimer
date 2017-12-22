package com.ergdyne.tasktimer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.EpicDate;

/**
 * Created by j on 3/22/17.
 * Intended for selecting the range on all types of total lists
 */

//Provides a range control for TotalsFragments.
    //TODO make range steady when going into subtotal or edit activities.
    //TODO possibly adapat for use in subtotals too.
public class TotalRangeFragment extends Fragment {

    /**********************/
    //variable definitions
    /**********************/
    TotalRangeFragment.OnSwitchRangeListener mainCallback;
    private Button dayButton;
    private Button weekButton;
    private Button monthButton;
    private Button allButton;

    private ImageView firstButton;
    private ImageView leftButton;
    private ImageView rightButton;
    private ImageView lastButton;

    private TextView centerDate;
    private RelativeLayout clickDate;
    private LinearLayout dateView;

    private int buttonRange;
    private EpicDate selectedDate;
    private long rightNow;

    private static final int DAY = 0;
    private static final int WEEK =1;
    private static final int MONTH = 2;
    private static final int ALL = 3;

    private int light;
    private int dark;



    /**********************/
    //Fragment lifecycle
    /**********************/
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mainCallback = (TotalRangeFragment.OnSwitchRangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnSwitchRangeListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            try {
                mainCallback = (TotalRangeFragment.OnSwitchRangeListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnSwitchRangeListener");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_range_totals, container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){

        //Link active components to view.
        dayButton = (Button) view.findViewById(R.id.button_day);
        weekButton = (Button) view.findViewById(R.id.button_week);
        monthButton = (Button) view.findViewById(R.id.button_month);
        allButton = (Button) view.findViewById(R.id.button_all);

        firstButton = (ImageView) view.findViewById(R.id.imageView_first);
        leftButton = (ImageView) view.findViewById(R.id.imageView_left);
        rightButton = (ImageView) view.findViewById(R.id.imageView_right);
        lastButton = (ImageView) view.findViewById(R.id.imageView_last);


        centerDate = (TextView) view.findViewById(R.id.textView_range_date_from);
        clickDate =(RelativeLayout) view.findViewById(R.id.relativeLayout_range_date);
        dateView = (LinearLayout) view.findViewById(R.id.linearLayout_date_range);

        light = ContextCompat.getColor(getActivity(),R.color.colorPrimary);
        dark = ContextCompat.getColor(getActivity(),R.color.colorPrimaryDark);

        buttonRange = DAY;

        selectedDate = new EpicDate();
        rightNow = new EpicDate().sEpoch;

        centerDate.setText(selectedDate.toDateString());

        addButtons();
        addDatePicker();

    }

    /**********************/
    //functions used in lifecycle
    /**********************/

    private void addButtons(){
        dayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonColors();
                dayButton.setBackgroundColor(dark);
                buttonRange = DAY;
                long to = selectedDate.sEpoch;
                long from = EpicDate.getDayStart(to);
                mainCallback.onTotalRangeChange(from, to);

            }
        });

        weekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonColors();
                weekButton.setBackgroundColor(dark);
                buttonRange = WEEK;
                long to = selectedDate.sEpoch;
                long from = EpicDate.getMondayStart(to);
                mainCallback.onTotalRangeChange(from, to);

            }
        });
        monthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonColors();
                monthButton.setBackgroundColor(dark);
                buttonRange = MONTH;
                long to = selectedDate.sEpoch;
                long from = EpicDate.getMonthStart(to);
                mainCallback.onTotalRangeChange(from,to);

            }
        });

        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonColors();
                allButton.setBackgroundColor(dark);
                buttonRange = ALL;
                dateView.setVisibility(View.GONE);
                selectedDate = new EpicDate();
                centerDate.setText(selectedDate.toDateString());
                long to = selectedDate.sEpoch;
                long from = 0;
                mainCallback.onTotalRangeChange(from, to);

            }
        });

        //TODO add grey-out when not usable.
        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBHelper db = new DBHelper(getActivity());
                selectedDate.setSEpoch(db.getLogStart());
                dateChange();
            }
        });

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustDate(-1);
                dateChange();
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustDate(1);
                dateChange();
            }
        });

        lastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDate.setSEpoch(rightNow);
                dateChange();
            }
        });


    }

    private void addDatePicker(){
        clickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog startPicker;
                startPicker = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        //load in the new values
                        selectedDate.setYear(year);
                        selectedDate.setMonth(month);
                        selectedDate.setDayOfMonth(dayOfMonth);

                        //reset to the date
                        selectedDate.setSEpoch();

                        dateChange();
                    }
                },selectedDate.year,selectedDate.month,selectedDate.dayOfMonth);
                startPicker.setTitle(getResources().getString(R.string.hint_set_range_date));
                startPicker.show();
            }
        });
    }


    private void adjustDate(int i){
        switch (buttonRange){
            case DAY:
                selectedDate.setSEpoch(selectedDate.sEpoch+i*AppConstants.DAY_LENGTH);
                break;
            case WEEK:
                selectedDate.setSEpoch(selectedDate.sEpoch+i*AppConstants.WEEK_LENGTH);
                break;
            case MONTH:
                if(selectedDate.month == 0 && i== -1){
                    selectedDate.setMonth(11);
                    selectedDate.setYear(selectedDate.year-1);
                }else if(selectedDate.month == 11 && i == 1){
                    selectedDate.setMonth(0);
                    selectedDate.setYear(selectedDate.year+1);
                }else{
                    selectedDate.setMonth(selectedDate.month+i);
                }
                selectedDate.setSEpoch();
                break;
            default:break;
        }

    }

    private void dateChange(){
        long to;
        long from;
        //Based on the range, choose the to from

        if(selectedDate.sEpoch > rightNow) {
            selectedDate.setSEpoch(rightNow);
        }
        switch (buttonRange) {
            case DAY:
                from = selectedDate.getDayStart();
                to = from + AppConstants.DAY_LENGTH;
                break;
            case WEEK:
                from = selectedDate.getMondayStart();
                to = from + AppConstants.WEEK_LENGTH;
                break;
            case MONTH:
                from = selectedDate.getMonthStart();
                to = selectedDate.getNextMonthStart();
                break;
            default:
                from = 0;
                to = selectedDate.sEpoch;
        }

        centerDate.setText(selectedDate.toDateString());
        mainCallback.onTotalRangeChange(from,to);
    }

    private void clearButtonColors(){
        dayButton.setBackgroundColor(light);
        weekButton.setBackgroundColor(light);
        monthButton.setBackgroundColor(light);
        allButton.setBackgroundColor(light);
        dateView.setVisibility(View.VISIBLE);
    }

    /**********************/
    //connection to mainactivity and other fragments
    /**********************/

    public interface OnSwitchRangeListener {
        public void onTotalRangeChange(long from, long to);
    }

}
