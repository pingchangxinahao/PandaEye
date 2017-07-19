package com.pandaq.pandaeye.modules.video.videodetail;

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pandaq.pandaeye.R;
import com.pandaq.pandaeye.activities.SwipeBackActivity;
import com.pandaq.pandaeye.config.Constants;
import com.pandaq.pandaeye.modules.video.videodetail.mvp.MovieInfo;
import com.pandaq.pandaeye.modules.video.videodetail.mvp.VideoCommentFrag;
import com.pandaq.pandaeye.modules.video.videodetail.mvp.VideoInfoFragment;
import com.pandaq.pandaeye.rxbus.RxBus;
import com.pandaq.pandaeye.rxbus.RxConstants;
import com.pandaq.pandaeye.utils.BlurImageUtils;
import com.pandaq.pandaeye.utils.PicassoTarget;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by PandaQ on 2017/2/28.
 * 视频详情界面
 */

public class VideoInfoActivity extends SwipeBackActivity implements ViewPager.OnPageChangeListener {

    @BindView(R.id.jc_video_player)
    JCVideoPlayerStandard mJcVideoPlayer;
    @BindView(R.id.toolbar_title)
    TextView mToolbarTitle;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.vp_video_info)
    ViewPager mVpVideoInfo;
    @BindView(R.id.tv_tab_description)
    TextView mTvTabDescription;
    @BindView(R.id.tv_tab_comment)
    TextView mTvTabComment;
    @BindView(R.id.ll_parent)
    LinearLayout mLlParent;
    private Disposable mDisposable;
    private Disposable mPicDisposable;
    private ArgbEvaluator argbEvaluator;
    private FloatEvaluator floatEvaluator;
    private String loadingTag = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vedioinfo);
        ButterKnife.bind(this);
        addViewPager(mVpVideoInfo);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        subscribeData();
        subscribePic();
        initView();
    }

    private void initView() {
        Bundle bundle = getIntent().getExtras();
        String title = bundle.getString(Constants.BUNDLE_KEY_TITLE);
        String idOrUrl = bundle.getString(Constants.BUNDLE_KEY_ID);
        String pic = bundle.getString(Constants.BUNDLE_KEY_IMG_URL);
        mToolbarTitle.setText(title);
        mJcVideoPlayer.backButton.setVisibility(View.GONE);
        mJcVideoPlayer.titleTextView.setVisibility(View.GONE);
        mJcVideoPlayer.tinyBackImageView.setVisibility(View.GONE);
        mJcVideoPlayer.thumbImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (!TextUtils.isEmpty(pic)) {
            Picasso.with(this)
                    .load(pic)
                    .into(new PicassoTarget(this, mJcVideoPlayer.thumbImageView, mToolbar,
                            new PicassoTarget.LoadListener() {
                                @Override
                                public void loaded(Bitmap bitmap) {
                                    Bitmap overlay = BlurImageUtils.blur(bitmap, 8, 8);
                                    mLlParent.setBackground(new BitmapDrawable(getResources(), overlay));
                                }
                            }));
        }
        final ArrayList<Fragment> fragments = new ArrayList<>();
        //将首次需要加载的电影Id传递过去
        VideoInfoFragment videoInfoFragment = new VideoInfoFragment();
        Bundle arg = new Bundle();
        arg.putString(Constants.BUNDLE_KEY_DATAID, idOrUrl);
        videoInfoFragment.setArguments(arg);
        VideoCommentFrag videoCommentFrag = new VideoCommentFrag();
        videoCommentFrag.setArguments(arg);
        fragments.add(videoInfoFragment);
        fragments.add(videoCommentFrag);
        mVpVideoInfo.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }
        });
        mVpVideoInfo.addOnPageChangeListener(this);
        mVpVideoInfo.setCurrentItem(0);
        argbEvaluator = new ArgbEvaluator();
        floatEvaluator = new FloatEvaluator();
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAfterTransition();
            }
        });
    }

    private void subscribeData() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
        // 点击推荐视频跳转观察者
        RxBus.getDefault()
                .toObservableWithCode(RxConstants.LOADED_DATA_CODE, MovieInfo.class)
                .subscribe(new Observer<MovieInfo>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(MovieInfo value) {
                        mToolbarTitle.setText(value.getTitle());
                        mJcVideoPlayer.setUp(value.getVideoUrl(), JCVideoPlayerStandard.SCREEN_LAYOUT_LIST, value.getTitle());
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        subscribeData();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void subscribePic() {
        if (mPicDisposable != null) {
            mPicDisposable.dispose();
        }
        RxBus.getDefault()
                .toObservableWithCode(RxConstants.LOADED_VIDEO_PIC_CODE, String.class)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mPicDisposable = d;
                    }

                    @Override
                    public void onNext(String value) {
                        if (!TextUtils.isEmpty(value)) {
                            // 当新的图片不是正在加载的图片时取消加载请求重新加载新图片
                            if (!value.equals(loadingTag)) {
                                Picasso.with(VideoInfoActivity.this)
                                        .cancelTag(loadingTag);
                            }
                            loadingTag = value;
                            Picasso.with(VideoInfoActivity.this)
                                    .load(value)
                                    .tag(value)
                                    .into(new PicassoTarget(VideoInfoActivity.this, mJcVideoPlayer.thumbImageView,
                                            mToolbar, new PicassoTarget.LoadListener() {
                                        @Override
                                        public void loaded(Bitmap bitmap) {
                                            Bitmap overlay = BlurImageUtils.blur(bitmap, 8, 8);
                                            mLlParent.setBackground(new BitmapDrawable(getResources(), overlay));
                                        }
                                    }));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        subscribePic();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }


    @Override
    public void onBackPressed() {
        if (JCVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        JCVideoPlayer.releaseAllVideos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mPicDisposable.dispose();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        changePage(position, positionOffset);
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void changePage(int position, float positionOffset) {
        if (position == 0) {
            // 字体颜色
            mTvTabDescription.setTextColor(ContextCompat.getColor(this, R.color.white_FFFFFF));
            int stepsColor = (int) argbEvaluator.evaluate(positionOffset, ContextCompat.getColor(this, R.color.white_FFFFFF),
                    ContextCompat.getColor(this, R.color.grey_303335));
            mTvTabDescription.setTextColor(stepsColor);
            mTvTabComment.setTextColor(ContextCompat.getColor(this, R.color.grey_303335));
            int sleepColor = (int) argbEvaluator.evaluate(positionOffset, ContextCompat.getColor(this, R.color.grey_303335),
                    ContextCompat.getColor(this, R.color.white_FFFFFF));
            mTvTabComment.setTextColor(sleepColor);
            // 字体大小
            mTvTabDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            float stepsSize = floatEvaluator.evaluate(positionOffset, 18, 14);
            mTvTabDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, stepsSize);
            mTvTabComment.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            float sleepSize = floatEvaluator.evaluate(positionOffset, 14, 18);
            mTvTabComment.setTextSize(TypedValue.COMPLEX_UNIT_SP, sleepSize);
        } else {
            // 字体颜色
            mTvTabDescription.setTextColor(ContextCompat.getColor(this, R.color.grey_303335));
            int stepsColor = (int) argbEvaluator.evaluate(positionOffset, ContextCompat.getColor(this, R.color.grey_303335),
                    ContextCompat.getColor(this, R.color.white_FFFFFF));
            mTvTabDescription.setTextColor(stepsColor);
            mTvTabComment.setTextColor(ContextCompat.getColor(this, R.color.white_FFFFFF));
            int sleepColor = (int) argbEvaluator.evaluate(positionOffset, ContextCompat.getColor(this, R.color.white_FFFFFF),
                    ContextCompat.getColor(this, R.color.grey_303335));
            mTvTabComment.setTextColor(sleepColor);
            // 字体大小
            mTvTabDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            float stepsSize = floatEvaluator.evaluate(positionOffset, 14, 18);
            mTvTabDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, stepsSize);
            mTvTabComment.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            float sleepSize = floatEvaluator.evaluate(positionOffset, 18, 14);
            mTvTabComment.setTextSize(TypedValue.COMPLEX_UNIT_SP, sleepSize);
        }
    }
}
