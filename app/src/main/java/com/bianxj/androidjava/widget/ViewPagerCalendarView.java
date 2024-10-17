package com.bianxj.androidjava.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bianxj.androidjava.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewPagerCalendarView extends LinearLayout {

    private final static long ONE_DAY = 24 * 60 * 60 * 1000;
    private int layout;
    private ViewPager vp;
    private CalendarAdapter mCalendarAdapter;

    public ViewPagerCalendarView(Context context) {
        this(context, null);
    }

    public ViewPagerCalendarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerCalendarView);
        layout = array.getResourceId(R.styleable.ViewPagerCalendarView_layout, R.layout.default_calendar);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);
        LayoutInflater.from(context).inflate(layout, this);
        vp = findViewById(R.id.pager);
        vp.addOnPageChangeListener(onPageChangeListener);
    }

    public void resetPosition() {
        vp.setCurrentItem(defaultPosition, true);
    }

    public void fresh() {
        getPageMonth(vp.getCurrentItem()).fresh();
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            PageMonth month = getPageMonth(position);
            LayoutParams vpLayoutParams = (LayoutParams) vp.getLayoutParams();
            vpLayoutParams.height = month.getHeight();
            vp.setLayoutParams(vpLayoutParams);

            callback.onPageSelected(month.mCalendar.getTimeInMillis());
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    //原点
    private long timestamp;
    private Map<Integer, PageMonth> monthMap = new HashMap<>();
    private int defaultPosition = 5000;
    private PageMonthAdapterCallback callback;
    private boolean isElasticity;

    public void setDefaultMonth(long timestamp, PageMonthAdapterCallback callback, boolean isElasticity) {
        this.timestamp = timestamp;
        this.callback = callback;
        this.isElasticity = isElasticity;

        mCalendarAdapter = new CalendarAdapter(this);
        vp.setAdapter(mCalendarAdapter);
        vp.setCurrentItem(defaultPosition);
    }

    public PageMonth getPageMonth(int position) {
        if (monthMap.containsKey(position)) {
            return monthMap.get(position);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            calendar.add(Calendar.MONTH, position - defaultPosition);
            PageMonth pageMonth = new PageMonth(getContext(), calendar.getTimeInMillis(), this.callback.buildPageMonthAdapter(), isElasticity);
            monthMap.put(position, pageMonth);
            return pageMonth;
        }
    }

    private class CalendarAdapter extends PagerAdapter {

        private ViewPagerCalendarView calendarView;

        public CalendarAdapter(ViewPagerCalendarView calendarView) {
            this.calendarView = calendarView;
        }

        @Override
        public int getCount() {
            return 10000;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            PageMonth month = calendarView.getPageMonth(position);
            container.addView(month.getRv());
            return month.getRv();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    private static class PageMonth {
        private Calendar mCalendar = Calendar.getInstance();
        private int lines;
        private List<Day> days = new ArrayList<>();
        private RecyclerView rv;
        //是否收缩
        private boolean isElasticity;
        private PageMonthAdapter mAdapter;
        private int lineHeight;

        public PageMonth(Context context, long timestamp, PageMonthAdapter adapter, boolean isElasticity) {
            this.isElasticity = isElasticity;
            rv = new RecyclerView(context);
            rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rv.setNestedScrollingEnabled(false);
            rv.setLayoutManager(new GridLayoutManager(context, 7));

            RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(rv, 0);
            holder.itemView.measure(0, 0);
            lineHeight = holder.itemView.getMeasuredHeight();

            mAdapter = adapter;
            rv.setAdapter(mAdapter);
            initDays(timestamp);
            mAdapter.setDays(days);
            mAdapter.notifyDataSetChanged();
        }

        private void initDays(long timestamp) {
            mCalendar.setTimeInMillis(timestamp);
            mCalendar.set(Calendar.DAY_OF_MONTH, 1);
            mCalendar.set(Calendar.HOUR, 0);
            mCalendar.set(Calendar.MINUTE, 0);
            mCalendar.set(Calendar.SECOND, 0);
            int diff = mCalendar.get(Calendar.DAY_OF_WEEK) - 1;
            long firstDay = mCalendar.getTimeInMillis() - (diff * ONE_DAY);
            days.clear();

            this.lines = 7;
            Calendar calendar = Calendar.getInstance();
            if (isElasticity) {
                calendar.setTimeInMillis(mCalendar.getTimeInMillis());
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                int dayCount = calendar.get(Calendar.DAY_OF_MONTH);
                int calcDayCount = dayCount + (dayOfWeek - 1);
                this.lines = calcDayCount / 7;
                if (calcDayCount % 7 > 0) {
                    this.lines++;
                }
            }

            for (int i = 0; i < this.lines * 7; i++) {
                calendar.setTimeInMillis(firstDay);
                calendar.add(Calendar.DAY_OF_YEAR, i);

                Day data = new Day();
                data.setCurrentMonth(calendar.get(Calendar.MONTH) == mCalendar.get(Calendar.MONTH));
                data.setTimestamp(calendar.getTimeInMillis());
                days.add(data);
            }
        }

        public void fresh() {
            mAdapter.notifyDataSetChanged();
        }

        public RecyclerView getRv() {
            return rv;
        }

        public int getHeight() {
            return lineHeight * lines;
        }
    }

    public abstract static class PageMonthAdapter<T extends PageMonthViewHolder> extends RecyclerView.Adapter<T> {

        private List<Day> days = new ArrayList<>();

        public void setDays(List<Day> days) {
            this.days.addAll(days);
        }

        @Override
        public void onBindViewHolder(@NonNull T holder, int position) {
            holder.bind(days.get(position));
        }

        @Override
        public int getItemCount() {
            return days.size();
        }
    }

    public abstract static class PageMonthViewHolder extends RecyclerView.ViewHolder {

        public PageMonthViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(Day day);
    }

    public interface PageMonthAdapterCallback {
        PageMonthAdapter buildPageMonthAdapter();

        void onPageSelected(long timestamp);
    }


    public static class Day {
        private long timestamp;
        private boolean isCurrentMonth;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isCurrentMonth() {
            return isCurrentMonth;
        }

        public void setCurrentMonth(boolean currentMonth) {
            isCurrentMonth = currentMonth;
        }
    }

}
