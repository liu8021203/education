package com.li.education.main.study.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.li.education.R;
import com.li.education.base.bean.vo.ChapterListVO;
import com.li.education.base.bean.vo.ChapterVO;
import com.li.education.base.bean.vo.StudyVO;
import com.li.education.main.study.PlayActivity;
import com.li.education.main.study.PlayListActivity;
import com.li.education.util.UtilIntent;

import java.util.List;

/**
 * Created by liu on 2017/6/8.
 */

public class ChapterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private List<ChapterVO> data;
    private PlayListActivity mActivity;
    private LayoutInflater mInflater;
    private ItemOnClickListener mListener;
    private ChapterListVO mListVO;

    public ChapterAdapter(PlayListActivity activity) {
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mListener = new ItemOnClickListener();
    }

    public void setData(List<ChapterVO> data) {
        this.data = data;
    }


    public void setListVO(ChapterListVO listVO) {
        mListVO = listVO;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        View view = mInflater.inflate(R.layout.recycle_chapter_list, parent, false);
        holder = new ItemViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChapterVO vo = data.get(position);
        ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
        itemViewHolder.mTvName.setText((position + 1) + "、" + vo.getTitle2());
        if(vo.getFinished().equals("Y")){
            itemViewHolder.mIvState.setImageResource(R.mipmap.play_complete);
        }else{
            itemViewHolder.mIvState.setImageResource(R.mipmap.play_uncomplete);
        }
        itemViewHolder.itemView.setTag(position);
        itemViewHolder.itemView.setOnClickListener(mListener);
    }


    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }



    private class ItemViewHolder extends RecyclerView.ViewHolder{
        private ImageView mIvState;
        private TextView mTvName;
        public ItemViewHolder(View itemView) {
            super(itemView);
            mIvState = (ImageView) itemView.findViewById(R.id.iv_state);
            mTvName = (TextView) itemView.findViewById(R.id.tv_name);
        }
    }

    private class ItemOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            bundle.putSerializable("data", mListVO);
            UtilIntent.intentDIYLeftToRight(mActivity, PlayActivity.class, bundle);
        }
    }
}
