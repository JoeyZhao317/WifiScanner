package com.cm.wifiscanner;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cm.wifiscanner.jabber.JabberActivity;
import com.cm.wifiscanner.util.ThemeManager;
import com.cm.wifiscanner.wifi.WifiListActivity;
import com.cm.wifiscanner.wifi.FavouriteActivity;

import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;

import java.util.ArrayList;
import java.util.List;

public class WifiScannerActivity extends SherlockFragmentActivity {

    private static final String WIFI_LIST_TAG = "tab-wifi-list";
    private static final String FAV_LIST_TAG = "tab-favorite-list";
    private static final String JABBER_TAG = "tab-jabber-list";
    private static final String SETTING_TAG = "tab-setting-list";

    private LocalActivityManager mActivityMgr;
    private ViewPager mViewPager;
    private MyPagerAdapter mPagerAdapter;

    private TabHost mTabHost;

    public WifiScannerActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getTheme());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mActivityMgr = new LocalActivityManager(this, true);
        mActivityMgr.dispatchResume();

        mPagerAdapter = new MyPagerAdapter(this, mTabHost, mViewPager);
        mPagerAdapter.addTabAndView(mTabHost, getView("list", new Intent(this, WifiListActivity.class)), "list");
        mPagerAdapter.addTabAndView(mTabHost, getView("fav", new Intent(this, FavouriteActivity.class)), "fav");
        mPagerAdapter.addTabAndView(mTabHost, getView("jab", new Intent(this, JabberActivity.class)), "jab");
        mPagerAdapter.addTabAndView(mTabHost, getView("jab", new Intent(this, SettingActivity.class)), "setting");

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(0);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isLight = ThemeManager.isLightTheme();

        menu.add("Save")
            .setIcon(isLight ? R.drawable.ic_compose_inverse : R.drawable.ic_compose)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add("Refresh")
            .setIcon(isLight ? R.drawable.ic_refresh_inverse : R.drawable.ic_refresh)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    private View getView(String tag, Intent intent) {
        return mActivityMgr.startActivity(tag, intent).getDecorView();
    }

    private static class MyPagerAdapter extends PagerAdapter implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener{
        static class _DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public _DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        private final List<View> mViewList;
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mPager;

        public MyPagerAdapter(Context context, TabHost tabHost, ViewPager pager) {
            mContext = context;
            mViewList = new ArrayList<View>();
            mTabHost = tabHost;
            mPager = pager;

            mTabHost.setOnTabChangedListener(this);
            mPager.setOnPageChangeListener(this);
        }

        public void addTabAndView(TabHost tabHost, View view, String name) {
            TabHost.TabSpec tabSpec = tabHost.newTabSpec(name).setIndicator(name);
            tabSpec.setContent(new _DummyTabFactory(mContext));
            tabHost.addTab(tabSpec);
            mViewList.add(view);
            notifyDataSetChanged();
        }

        @Override
        public void startUpdate(View arg0) {
        }

        @Override
        public Object instantiateItem(View view, int position) {
            ViewPager viewPager = (ViewPager) view;
            viewPager.addView(mViewList.get(position));
            return mViewList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ViewPager pViewPager = ((ViewPager) container);
            pViewPager.removeView(mViewList.get(position));
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mPager.setCurrentItem(position);
        }
    }
}
