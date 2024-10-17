package com.bianxj.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class PageCalendarView extends ViewGroup {

    private VelocityTracker mVelocityTracker;
    private OverScroller mOverScroler;
    private int mMaxVelocity = 0;
    private int mMinVelocity = 0;
    private int mSlop = 0;

    private Date start;
    private Date end;

    private Calendar limitStart;
    private Calendar limitEnd;

    public PageCalendarView(Context context) {
        this(context,null);
    }

    public PageCalendarView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public PageCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mOverScroler = new OverScroller(context,new LinearInterpolator());
        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int count = getChildCount();
        if ( count > 0 ){
            for (int i=0;i<count;i++){
                View view = getChildAt(i);
                measureChild(view,widthMeasureSpec,heightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        if ( count > 0 ){
            for (int i=0;i<count;i++){
                View view = getChildAt(i);
                view.layout(view.getLeft(),view.getTop(),view.getRight(),view.getBottom());
            }
        }
    }

    public void setLimitStart(Date limitStart){
        if ( limitStart != null ){
            this.limitStart = Calendar.getInstance();
            this.limitStart.setTimeInMillis(limitStart.getTime());
        } else {
            this.limitStart = null;
        }
    }

    public void setLimitEnd(Date limitEnd){
        if ( limitEnd != null ){
            this.limitEnd = Calendar.getInstance();
            this.limitEnd.setTimeInMillis(limitEnd.getTime());
        } else {
            this.limitEnd = null;
        }
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public void notifityChangeDate(){
        for (Month month:months){
            month.fresh();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                if ( !mOverScroler.computeScrollOffset() ){
                    isScrolling = false;
                }
                mOverScroler.abortAnimation();
                preY = ev.getRawY();
            case MotionEvent.ACTION_MOVE:
                int dy = (int) (ev.getRawY() - preY);
                if ( !isScrolling ){
                    if ( Math.abs(dy) >= mSlop ){
                        isScrolling = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return isScrolling;
    }

    private float positionY = 0;
    private boolean isScrolling = false;

    private float preY = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ( mVelocityTracker == null ){
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                int dy = (int) (event.getRawY() - preY);
                freshCalendar(dy);
                preY = event.getRawY();
                requestLayout();
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000);
                float yVelocity = mVelocityTracker.getYVelocity();
                if ( Math.abs(yVelocity)  > 1000 ) {
                    preY = 0;
                    if (yVelocity > 0) {
                        mOverScroler.fling(0, 0, 0, (int) yVelocity, mMinVelocity, mMaxVelocity, mMinVelocity, mMaxVelocity);
                    } else {
                        mOverScroler.fling(0, 0, 0, (int) yVelocity, mMinVelocity, mMaxVelocity, -mMaxVelocity, -mMinVelocity);
                    }
                    mVelocityTracker.clear();
                    invalidate();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if ( mOverScroler.computeScrollOffset() ){
            int currentY = mOverScroler.getCurrY();
            int dy = (int) (currentY - preY);
            preY = currentY;
            freshCalendar(dy);
            requestLayout();
            invalidate();
        }
    }

    private float topLine = 0;
    private float bottomLine = 0;
    private void freshCalendar(float offset){
        topLine += offset;
        bottomLine += offset;

        if ( limitStart != null ) {
            if (calendar.get(Calendar.YEAR) == limitStart.get(Calendar.YEAR)
                    && calendar.get(Calendar.MONTH) == limitStart.get(Calendar.MONTH)
                    && topLine > 0
            ){
                topLine = 0;
                bottomLine = 0;
                for (Month month:months){
                    bottomLine+=month.getHeight();
                }
            }
        }

        if ( limitEnd != null ){
            long temp = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH,months.size()-1);

            if (calendar.get(Calendar.YEAR) == limitEnd.get(Calendar.YEAR)
                    && calendar.get(Calendar.MONTH) == limitEnd.get(Calendar.MONTH)
                    && bottomLine < mParentHeight
            ){
                bottomLine = mParentHeight;
                topLine = mParentHeight;
                for (Month month:months){
                    topLine-=month.getHeight();
                }
            }
            calendar.setTimeInMillis(temp);
        }

        int baseLine = (int) topLine;
        for (Month month:months){
            System.out.println(month.getMonth());
            month.setTop(baseLine);
            if ( month.getTop() > mParentHeight ){
                bottomLine -= month.getHeight();
                recoveryMonth(month);
            } else if ( month.getBottom() < 0 ){
                topLine += month.getHeight();
                calendar.add(Calendar.MONTH,1);
                recoveryMonth(month);
            }
            baseLine+= month.getHeight();
        }
        months.removeAll(recoveryMonth);


        while (topLine > 0){
            calendar.add(Calendar.MONTH,-1);
            Month month = createMonth(calendar.getTimeInMillis());
            topLine -= month.getHeight();
            month.setTop((int) topLine);
            months.addFirst(month);
        }

        long temp = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH,months.size()-1);

        while (bottomLine < mParentHeight){
            calendar.add(Calendar.MONTH,1);
            Month month = createMonth(calendar.getTimeInMillis());

            month.setTop((int) bottomLine);
            bottomLine += month.getHeight();
            months.addLast(month);
        }
        calendar.setTimeInMillis(temp);
        requestLayout();
    }

    private final static int columnCount = 7;
    public final static long ONE_DAY = 24 * 60 * 60 * 1000;
    private List<DayHolder> recoveryDayHolder = new ArrayList<>();
    private List<Month> recoveryMonth = new ArrayList<>();
    private LinkedList<Month> months = new LinkedList<>();
    private Adapter adapter;

    private int mParentHeight;
    private int mParentWidth;
//    private int mItemMonthHeight;
    private int mItemHeight;
    private int mItemWidth;
    private Calendar calendar;

    public void setAdapter(Adapter adapter){
        this.adapter = adapter;
    }

    public void setDefaultMonth(long time){
        this.calendar = Calendar.getInstance();
        this.calendar.setTimeInMillis(time);

        mParentHeight = getMeasuredHeight();
        mParentWidth = getMeasuredWidth();
        mItemWidth = mParentWidth/columnCount;
        freshCalendar(0);
    }

    private DayHolder createDayHolder(){
        if ( recoveryDayHolder.size() > 0 ){
            DayHolder holder = recoveryDayHolder.get(0);
            recoveryDayHolder.remove(holder);
            return holder;
        }
        DayHolder holder = adapter.onCreateViewHolder(this);
        View view = holder.itemView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setZ(0);
        }
        LayoutParams params = holder.itemView.getLayoutParams();
        params.width = mItemWidth;
        if ( mItemHeight == 0 ){
            view.measure(0,0);
            mItemHeight = view.getMeasuredHeight();
        }
        addView(view);
        return holder;
    }

    private void bindViewHolder(DayHolder holder,long time){
        adapter.bindViewHolder(holder,time);
    }

    public void recoveryMonth(Month month){
        recoveryMonth.add(month);
        recoveryDayHolder.addAll(month.getDays());
    }


    public Month createMonth(long time){
        Month month = null;
        if ( recoveryMonth.size() > 0 ){
            month = recoveryMonth.get(0);
            recoveryMonth.remove(month);
        } else {
            month = new Month(this);
        }
        month.setCalendar(time);
        return month;
    }

//    public TextView createMonthTitle(){
//        TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.item_month,this,false);
//
//        tv.setZ(1);
//        addView(tv);
//        if ( mItemMonthHeight == 0 ){
//            mItemMonthHeight = tv.getLayoutParams().height;
//        }
//        return tv;
//    }

    public static class Month{
        private Calendar calendar;
        private View monthTitle;
        private MonthHolder monthHolder;
        private int mItemMonthHeight;
        private List<DayHolder> days;
        private List<DayHolder> realDays;
        private PageCalendarView view;
        private int top;
        private int bottom;
        private String month;

        public Month(PageCalendarView view) {
            this.view = view;
            calendar = Calendar.getInstance();
            days = new ArrayList<>();
            realDays = new ArrayList<>();
            monthHolder = view.adapter.onCreateMonthHolder(view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                monthHolder.itemView.setZ(1);
            }
            view.addView(monthHolder.itemView);
            this.monthTitle = monthHolder.itemView;
            this.mItemMonthHeight = this.monthTitle.getLayoutParams().height;
        }

        public String getMonth() {
            return month;
        }

        public void fresh(){
            long startOfMonth = calendar.getTimeInMillis();
            for (DayHolder holder:realDays){
                view.bindViewHolder(holder,startOfMonth);
                startOfMonth += ONE_DAY;
            }
        }

        public void setCalendar(long time){
            calendar.setTimeInMillis(time);
            month = calendar.get(Calendar.YEAR)+"年"+(calendar.get(Calendar.MONTH)+1)+"月";
            days.clear();
            realDays.clear();

            view.adapter.bindMonthHolder(monthHolder,time);
//            monthTitle.setText(month);
            calendar.add(Calendar.MONTH,1);
            calendar.set(Calendar.DAY_OF_MONTH,1);
            calendar.add(Calendar.DAY_OF_MONTH,-1);
            long endOfMonth = calendar.getTimeInMillis();
            int endDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            calendar.set(Calendar.DAY_OF_MONTH,1);
            long startOfMonth = calendar.getTimeInMillis();
            int startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            if ( startDayOfWeek > 1 ){
                for (int i=1;i<startDayOfWeek;i++){
                    DayHolder holder = view.createDayHolder();
                    view.bindViewHolder(holder,0);
                    days.add(holder);
                }
            }

            while(startOfMonth <= endOfMonth){
                DayHolder holder = view.createDayHolder();
                view.bindViewHolder(holder,startOfMonth);
                days.add(holder);
                realDays.add(holder);
                startOfMonth += ONE_DAY;
            }

            if ( endDayOfWeek < 7 ){
                for (int i=7;i>endDayOfWeek;i--){
                    DayHolder holder = view.createDayHolder();
                    view.bindViewHolder(holder,0);
                    days.add(holder);
                }
            }
        }

        public void setBottom(int bottom){
            this.top = bottom;
            this.bottom = bottom;
            for (int line = days.size()/7-1;line>=0;line--){
                for (int column=0;column<7;column++){
                    DayHolder holder = days.get(line*7+column);
                    View itemView = holder.itemView;
                    itemView.setTop(bottom+(line-1)*view.mItemHeight);
                    itemView.setBottom(bottom+line*view.mItemHeight);
                    itemView.setLeft(column*view.mItemWidth);
                    itemView.setRight((column+1)*view.mItemWidth);
                }
                this.top-=view.mItemHeight;
            }

            this.top -= mItemMonthHeight;
            monthTitle.setTop(top);
            monthTitle.setBottom(top+mItemMonthHeight);
            monthTitle.setLeft(0);
            monthTitle.setRight(view.mParentWidth);
        }

        public void setTop(int top){
            this.top = top;
            this.bottom = top;

            this.bottom += mItemMonthHeight;
            if ( top < 0 ){
                monthTitle.setTop(0);
                monthTitle.setBottom(mItemMonthHeight);
            } else {
                monthTitle.setTop(top);
                monthTitle.setBottom(top+mItemMonthHeight);
            }

            monthTitle.setLeft(0);
            monthTitle.setRight(view.mParentWidth);

            for (int line = 0;line<days.size()/7;line++){
                for (int column=0;column<7;column++){
                    DayHolder holder = days.get(line*7+column);
                    View itemView = holder.itemView;

                    itemView.setTop(bottom);
                    itemView.setBottom(bottom+view.mItemHeight);
                    itemView.setLeft(column*view.mItemWidth);
                    itemView.setRight((column+1)*view.mItemWidth);
                }
                this.bottom+=view.mItemHeight;
            }

            if ( bottom - mItemMonthHeight < 0 ){
                monthTitle.setTop(bottom - mItemMonthHeight);
                monthTitle.setBottom(bottom);
            }
        }

        public float getTop(){
            return top;
        }

        public float getBottom(){
            return bottom;
        }

        public List<DayHolder> getDays(){
            return  days;
        }

        public int getLines(){
            return days.size()/7;
        }

        public int getHeight(){
            return (days.size()/7 * view.mItemHeight) + mItemMonthHeight;
        }
    }

    public static class DayHolder extends RecyclerView.ViewHolder {

        public DayHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class MonthHolder extends RecyclerView.ViewHolder {

        public MonthHolder(@NonNull View itemView) {super(itemView);}
    }

    public interface Adapter{
        DayHolder onCreateViewHolder(ViewGroup parent);
        void bindViewHolder(DayHolder holder, long time);
        MonthHolder onCreateMonthHolder(ViewGroup parent);
        void bindMonthHolder(MonthHolder holder,long time);
    }

}
