package com.li.education.main.home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.li.education.MainActivity;
import com.li.education.R;
import com.li.education.base.BaseFragment;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.bean.QuestionResult;
import com.li.education.base.bean.vo.HomeFocusVO;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.util.UtilGlide;
import com.li.education.util.UtilIntent;
import com.li.education.view.LfLayoutManager;
import com.liu.learning.library.LoopViewPager;

import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/3.
 */

public class HomeFragment extends BaseFragment implements OnRefreshListener, OnLoadMoreListener{
    private View mView;
    private RecyclerView mRecyclerView;
    private HomeAdapter mHomeAdapter;
    private LoopViewPager mViewPager;
    private SwipeToLoadLayout mSwipeToLoadLayout;
    private ScrollView mScrollView;

    private int page = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_home, container, false);
        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        getData();
    }

    private void getData() {
        RetrofitUtil.getInstance().create(HttpService.class).homePagePre().subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<HomeResult>() {

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onCompleted() {
                mSwipeToLoadLayout.setRefreshing(false);
            }

            @Override
            public void onError(Throwable e) {
                mSwipeToLoadLayout.setRefreshing(false);
                e.printStackTrace();
            }

            @Override
            public void onNext(HomeResult result) {
                if(result.isStatus()){
                    initData(result);
                }
            }
        });
    }

    private void initView() {
        mSwipeToLoadLayout = (SwipeToLoadLayout) mView.findViewById(R.id.swipe_to_load_layout);
        mSwipeToLoadLayout.setOnRefreshListener(this);
        mSwipeToLoadLayout.setOnLoadMoreListener(this);
        mScrollView = (ScrollView) mView.findViewById(R.id.swipe_target);
        mRecyclerView = (RecyclerView) mView.findViewById(R.id.recycle_view);
        mRecyclerView.setFocusableInTouchMode(false);
        mRecyclerView.setFocusable(false);
        mRecyclerView.setHasFixedSize(true);
        LfLayoutManager manager = new LfLayoutManager(getActivity());
        manager.setScrollEnabled(false);
        mRecyclerView.setLayoutManager(manager);

        mRecyclerView.addItemDecoration(new CustomItemDecoration(1));
        mViewPager = (LoopViewPager) mView.findViewById(R.id.loop_pager);
        mViewPager.setCycle(true);
        mViewPager.setWheel(true);
        mViewPager.setTime(4000);
        mViewPager.setIndicatorRes(R.mipmap.banner_unselect, R.mipmap.banner_select);
        mScrollView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);
    }

    ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {

        @Override
        public void onScrollChanged() {
            if (mScrollView.getChildAt(0).getHeight() < mScrollView.getScrollY() + mScrollView.getHeight() && !ViewCompat.canScrollVertically(mScrollView, 1)) {
                mSwipeToLoadLayout.setLoadingMore(true);
            }
        }
    };

    private void initData(HomeResult result){
        if(result != null && result.getData() != null && result.getData().getNewsList() != null){
            mViewPager.setData(result.getData().getFocusList(), new LoopViewPager.ImageListener<HomeFocusVO>() {

                @Override
                public void onImageClick(HomeFocusVO data) {
                    Bundle bundle = new Bundle();
                    bundle.putString("url", data.getCfurl());
                    bundle.putString("title", data.getCfname());
                    UtilIntent.intentDIYLeftToRight(getActivity(), NewDetailsActivity.class, bundle);
                }

                @Override
                public void initData(ImageView view, HomeFocusVO vo) {
                    UtilGlide.loadImg(HomeFragment.this, view, vo.getImgurl());
                }
            });
            if(mHomeAdapter == null){
                mHomeAdapter = new HomeAdapter((MainActivity) getActivity());
                mHomeAdapter.setData(result.getData().getNewsList());
                mRecyclerView.setAdapter(mHomeAdapter);
            }else{
                mHomeAdapter.setData(result.getData().getNewsList());
                mHomeAdapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void onLoadMore() {

    }

    @Override
    public void onRefresh() {
        page = 1;
        getData();
    }
}
