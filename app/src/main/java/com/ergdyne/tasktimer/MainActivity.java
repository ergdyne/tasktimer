package com.ergdyne.tasktimer;



import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.DBMap;

//Alternatively this could be done with multiple activities; however, using fragments is faster...
//There is an abandoned branch where I attempted to do this, but it isn't shared with the public repo
//because the original repo contained data that should have been saved in an uncommited configuration file.
public class MainActivity extends AppCompatActivity
        implements
        EventsListFragment.OnEventSelectedListener,
        EventsListFragment.OnEventLongClicked,
        SwitchTaskFragment.OnSwitchTaskListener,
        SwitchTaskFragment.OnEditCurrentListener,
        TotalRangeFragment.OnSwitchRangeListener,
        TaskTotalsFragment.OnTaskLongClickListener,
        TaskTotalsFragment.OnTaskClickListener,
        TagTotalsFragment.OnTagClickListener,
        TagTotalsFragment.OnTagLongClickListener,
        TagTotalsFragment.OnNoTags,
        NavigationView.OnNavigationItemSelectedListener

{

    /**********************/
    //Variable definitions
    /**********************/
    //Fragments
    private EventsListFragment eventsListFragment;
    private SwitchTaskFragment switchTaskFragment;
    private TotalRangeFragment taskRangeFragment;
    private TaskTotalsFragment taskTotalsFragment;
    private TagTotalsFragment tagTotalsFragment;
    private TotalRangeFragment tagRangeFragment;
    private ExportFragment exportFragment;

    //layout locations
    private FrameLayout topFrame;
    private FrameLayout bottomFrame;
    private FrameLayout wholeFrame;

    TextView tempText;

    //Weird navigation system items. Probably not optimal way to do this.
    private NavigationView navigationView;
    private int navigationID;
    private String NAVIGATION_ID_KEY = "navigationID";
    private int NAV_HOME = 0;
    private int NAV_TASK_TOTALS = 1;
    private int NAV_TAG_TOTALS = 2;
    private int NAV_EXPORT = 4;
    private int NAV_CHARTS = 3;



    /**********************/
    //Activity lifecycle
    /**********************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //Setup Navigation
        {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_drawer);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_events_log);
            navigationView.setNavigationItemSelectedListener(this);
        }


        //Link to frames.
        {
            topFrame = (FrameLayout) findViewById(R.id.fragment_top);
            bottomFrame = (FrameLayout) findViewById(R.id.fragment_bottom);
            wholeFrame = (FrameLayout) findViewById(R.id.fragment_whole);

            //Temporary method of adding text and information to basically blank fragments,
            //which became long term.
            tempText = new TextView(this);
            tempText.setAutoLinkMask(Linkify.WEB_URLS);
            tempText.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            wholeFrame.addView(tempText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        //Run the tutorial functionality.
        TutorialToast.init(this);

        //Initiate the frames if new ones are required or set the navigationID to match the saved state.
        //The navigationID is then called in onResume to fill the content.
        if(savedInstanceState == null) {
            navigationID = NAV_HOME;

            eventsListFragment = new EventsListFragment();
            switchTaskFragment = new SwitchTaskFragment();

            getFragmentManager().beginTransaction().add(R.id.fragment_top, switchTaskFragment).commit();
            getFragmentManager().beginTransaction().add(R.id.fragment_bottom, eventsListFragment).commit();
            getFragmentManager().beginTransaction().add(R.id.fragment_whole, new EmptyFragmentHack())
                    .addToBackStack(null).commit();
        }else{

            navigationID = savedInstanceState.getInt(NAVIGATION_ID_KEY,R.id.nav_events_log);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main,menu);

        return super.onCreateOptionsMenu(menu);
    }

    //Builds the menu under the (...) in the upper right.
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_settings:
                 Toast.makeText(this,"No Settings Yet",Toast.LENGTH_SHORT).show();
                 return true;
            case R.id.action_about:
                try{
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.ABOUT_PAGE));
                    startActivity(intent);
                }catch(ActivityNotFoundException e){

                    Toast.makeText(this,"No application can handle this request." + " Please install a webbrowser", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        navigationRouting(navigationID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putInt(NAVIGATION_ID_KEY,navigationID);
    }


    /**********************/
    //All the listeners, connections to Fragments.
    /**********************/
    public void onTaskSwitch(){
        eventsListFragment.onTaskSwitch();
    }

    public void onEditCurrent(){
        openEventEdit(DBMap.SettingsTable.defaultID,true);
    }

    public void onEventSelected(long id){
        //remove the tutorial
        TutorialToast.remove(this, TutorialToast.TUT_REPEAT_TASK);
        switchTaskFragment.onListClicked(id);

    }

    public void onEventLongClicked(long id){
        TutorialToast.remove(this, TutorialToast.TUT_EDIT_EVENT);
        openEventEdit(id,false);
    }

    public void onTotalRangeChange(long from, long to){
        if(navigationID == NAV_TASK_TOTALS){
            taskTotalsFragment.onTaskRangeChange(from, to);
        }else{
            tagTotalsFragment.onTagRangeChange(from, to);
        }
    }

    public void onTaskClick(long id,long from, long to){
        TutorialToast.remove(this, TutorialToast.TUT_VIEW_TASK);
        openSubTotals(id,SubTotalsActivity.TASK_TABLE,from,to);
    }

    public void onTaskLongClick(long id){
        TutorialToast.remove(this, TutorialToast.TUT_EDIT_TASK);
        openTaskEdit(id);}

    public void onNoTags(){
        wholeVisible();
        tempText.setText(getResources().getString(R.string.txt_no_tags));
    }

    public void onTagClick(long id, long from, long to){
        TutorialToast.remove(this, TutorialToast.TUT_VIEW_TAG);
        openSubTotals(id,SubTotalsActivity.TAG_TABLE,from,to);
    }
    public void onTagLongClick(long id){
        TutorialToast.remove(this, TutorialToast.TUT_EDIT_TAG);
        openTagEdit(id);}


    /**********************/
    //navigation
    /**********************/
    @Override
    public void onBackPressed() {
        //Back either moves to home screen or exits.
        if (getFragmentManager().findFragmentById(R.id.fragment_bottom) == eventsListFragment ||
                getFragmentManager().findFragmentById(R.id.fragment_top) == switchTaskFragment) {

            finish();
        } else {
            goToHome();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        navigationRouting(id);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_drawer);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //I don't like how many items are here in the if chain, but I could be wrong. Maybe this is ok.
    private void navigationRouting(int id){
        if (id == R.id.nav_events_log || id == NAV_HOME) {
            goToHome();

        } else if (id == R.id.nav_task_totals || id == NAV_TASK_TOTALS) {
            goToTaskTotals();

        } else if (id == R.id.nav_tag_totals || id == NAV_TAG_TOTALS) {
            goToTagTotals();

        }else if (id == R.id.nav_export || id == NAV_EXPORT){
            goToExport();

        }else if (id == R.id.nav_charts || id == NAV_CHARTS){
            goToCharts();

        }
    }

    /**********************/
    //Main activity fragment Swaps for navigationRouting
    /**********************/
    private void goToHome(){
        splitVisible();

        //For back press case.
        navigationView.setCheckedItem(R.id.nav_events_log);
        navigationID = NAV_HOME;

        switchTaskFragment = new SwitchTaskFragment();
        eventsListFragment = new EventsListFragment();

        getFragmentManager().beginTransaction().replace(R.id.fragment_top,switchTaskFragment).commit();
        getFragmentManager().beginTransaction().replace(R.id.fragment_bottom,eventsListFragment).commit();

        //Tutorial, TODO further centralize tutorial stuff. i.e. make it so there aren't crazy indents.
        if(!TutorialToast.make(this, TutorialToast.TUT_FIRST_EVENT,
                getResources().getString(R.string.tut_first_task),TutorialToast.FIRST_EVENT_LENGTH)){
            if(!TutorialToast.make(this, TutorialToast.TUT_REPEAT_TASK,
                    getResources().getString(R.string.tut_repeat_task),TutorialToast.REPEAT_TASK_LENGTH)){
                if(!TutorialToast.make(this, TutorialToast.TUT_EDIT_EVENT,
                        getResources().getString(R.string.tut_edit_event),TutorialToast.EDIT_EVENT_LENGTH)){
                    if(!TutorialToast.make(this, TutorialToast.TUT_TASK_TOTALS,
                            getResources().getString(R.string.tut_task_totals),TutorialToast.TASK_TOTALS_LENGTH)){
                        if(!TutorialToast.make(this, TutorialToast.TUT_STOP_TRACKING,
                                getResources().getString(R.string.tut_stop_tracking),TutorialToast.STOP_TRACKING_LENGTH)){

                        }
                    }
                }
            }
        }
    }

    private void goToTaskTotals() {
        taskTotalsFragment = new TaskTotalsFragment();
        taskRangeFragment = new TotalRangeFragment();

        splitVisible();

        navigationID = NAV_TASK_TOTALS;
        getFragmentManager().beginTransaction().replace(R.id.fragment_top, taskRangeFragment).commit();
        getFragmentManager().beginTransaction().replace(R.id.fragment_bottom, taskTotalsFragment).commit();

        //Activate the tutorial messages.
        TutorialToast.remove(this, TutorialToast.TUT_TASK_TOTALS);

        if (!TutorialToast.make(this, TutorialToast.TUT_EDIT_TASK,
                getResources().getString(R.string.tut_edit_task), TutorialToast.EDIT_TASK_LENGTH)) {
            if (!TutorialToast.make(this, TutorialToast.TUT_VIEW_TASK,
                    getResources().getString(R.string.tut_view_task), TutorialToast.VIEW_TASK_LENGTH)) {

            }
        }

    }

    private void goToTagTotals(){
        tagTotalsFragment = new TagTotalsFragment();
        tagRangeFragment = new TotalRangeFragment();

        splitVisible();

        navigationID = NAV_TAG_TOTALS;
        getFragmentManager().beginTransaction().replace(R.id.fragment_top,tagRangeFragment).commit();
        getFragmentManager().beginTransaction().replace(R.id.fragment_bottom,tagTotalsFragment).commit();


    }

    private void goToCharts(){
        wholeVisible();
        navigationID = NAV_CHARTS;

        //Uh, well charts did not come soon, and are not coming.
        tempText.setText(getResources().getString(R.string.txt_charts_coming));
    }

    private void goToExport(){
        exportFragment = new ExportFragment();

        wholeVisible();
        navigationID = NAV_EXPORT;
        getFragmentManager().beginTransaction().replace(R.id.fragment_whole,exportFragment).commit();
    }


    /**********************/
    //Navigation support functions.
    //Interacting with content_main.xml to swap frame layouts
    /**********************/
    private void wholeVisible(){
        topFrame.setVisibility(View.GONE);
        bottomFrame.setVisibility(View.GONE);
        wholeFrame.setVisibility(View.VISIBLE);

        tempText.setText("");

        //Hack for back button to work "correctly."
        //i.e. the way I implement the back button is not best practice.
        getFragmentManager().beginTransaction().replace(R.id.fragment_bottom,new EmptyFragmentHack())
                .addToBackStack(null).commit();
        getFragmentManager().beginTransaction().replace(R.id.fragment_top,new EmptyFragmentHack())
                .addToBackStack(null).commit();
        getFragmentManager().beginTransaction().replace(R.id.fragment_whole,new EmptyFragmentHack())
                .addToBackStack(null).commit();

    }
    private void splitVisible(){
        wholeFrame.setVisibility(View.GONE);
        topFrame.setVisibility(View.VISIBLE);
        bottomFrame.setVisibility(View.VISIBLE);

        tempText.setText("");

        getFragmentManager().beginTransaction().replace(R.id.fragment_whole,new EmptyFragmentHack())
                .addToBackStack(null).commit();

    }

    /**********************/
    //activity calls
    /**********************/
    private void openEventEdit(long id, boolean current){
        Intent editEvent = new Intent(this,EditEventActivity.class)
                .putExtra(EditEventActivity.IS_CURRENT,current)
                .putExtra(EditEventActivity.EVENT_ID,id);
        startActivity(editEvent);
    }

    private void openTaskEdit(long id){
        Intent editTask = new Intent(this, EditTaskActivity.class)
                .putExtra(EditTaskActivity.TASK_ID,id);
        startActivity(editTask);

    }

    private void openTagEdit(long id){
        Intent editTag = new Intent(this, EditTagActivity.class)
                .putExtra(EditTagActivity.TAG_ID,id);
        startActivity(editTag);
    }

    private void openSubTotals(long id, int table,long from, long to){
        Intent subTotal = new Intent(this,SubTotalsActivity.class)
                .putExtra(SubTotalsActivity.ITEM_ID, id)
                .putExtra(SubTotalsActivity.ITEM_TABLE,table)
                .putExtra(SubTotalsActivity.FROM,from)
                .putExtra(SubTotalsActivity.TO,to);
        startActivity(subTotal);
    }

}
