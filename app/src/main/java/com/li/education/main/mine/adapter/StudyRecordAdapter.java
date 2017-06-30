package com.li.education.main.mine.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.li.education.R;
import com.li.education.main.home.HomeAdapter;
import com.li.education.main.mine.StudyRecordActivity;

/**
 * Created by liu on 2017/6/19.
 */

public class StudyRecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private StudyRecordActivity mActivity;
    private LayoutInflater mInflater;

    public StudyRecordAdapter(StudyRecordActivity activity) {
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        View view = mInflater.inflate(R.layout.recycle_study_record_item, parent, false);
        holder = new ItemViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 10;
    }


    private class ItemViewHolder extends RecyclerView.ViewHolder{
        private TextView mTvName;
        private TextView mTvTime;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mTvName = (TextView) itemView.findViewById(R.id.tv_name);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
        }
    }
}
