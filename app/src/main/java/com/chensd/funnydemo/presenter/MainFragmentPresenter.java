package com.chensd.funnydemo.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.chensd.funnydemo.adapter.RecyclerAdapter;
import com.chensd.funnydemo.model.ImageInfo;
import com.chensd.funnydemo.retrofit.ApiCallBack;
import com.chensd.funnydemo.retrofit.ImageApi;
import com.chensd.funnydemo.retrofit.RetrofitApi;
import com.chensd.funnydemo.ui.BaseActivity;
import com.chensd.funnydemo.utils.FileUtil;
import com.chensd.funnydemo.utils.OkHttpClientManager;
import com.chensd.funnydemo.utils.WxShareUtil;
import com.chensd.funnydemo.view.BaseView;

import java.io.File;
import java.util.List;

import io.realm.Realm;
import okhttp3.Call;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by chen on 2017/1/17.
 */
public class MainFragmentPresenter extends BasePresenter<BaseView> {

    protected ImageApi imageApi;

    public MainFragmentPresenter(BaseView view){
        attachView(view);
        imageApi = RetrofitApi.getImageApi();
    }

    public void loadImage(String key){
        mvpView.showLoading();
        Subscription subscription = imageApi.search(key)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new ApiCallBack<List<ImageInfo>>() {
                    @Override
                    public void onSuccess(List<ImageInfo> model) {
                        mvpView.hideLoading();
                        mvpView.getDataSuccess(model);
                    }

                    @Override
                    public void onFailure(String msg) {
                        mvpView.hideLoading();
                        mvpView.getDataFailure(msg);
                    }

                    @Override
                    public void onFinish() {
                        mvpView.hideLoading();
                    }
                });
    }

    public void onItemViewClick(final BaseActivity mActivity, View v, final int position, final RecyclerAdapter mAdapter){
        final String image_url = ((ImageInfo) mAdapter.getItem(position)).getImage_url();
        WxShareUtil.showShareDialog(mActivity, "分享", image_url, new WxShareUtil.OnShareBtnClick() {
            @Override
            public void shareBtnClick(View v, Drawable drawable) {

                if (drawable instanceof GlideBitmapDrawable) {
                    Bitmap bmp = ((GlideBitmapDrawable) drawable).getBitmap();

                    if (bmp != null) {
                        final Bitmap finalBmp = bmp;
                        Observable.create(new Observable.OnSubscribe<String>() {
                            @Override
                            public void call(Subscriber<? super String> subscriber) {
                                WxShareUtil.shareBitmap(mActivity.wxapi, finalBmp);
                                subscriber.onNext("success");
                            }
                        }).subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<String>() {
                                    @Override
                                    public void call(String s) {
                                        if (!TextUtils.isEmpty(s) && s.equals("success")){
//                                                    showtToast("添加到记录");
                                            //更新到数据库
                                            final ImageInfo item = (ImageInfo) mAdapter.getItem(position);
                                            item.setTime(System.currentTimeMillis());
                                            Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    realm.copyToRealmOrUpdate(item);
                                                }
                                            });
                                        }
                                    }
                                });
                        Log.e("weixin", "width=" + bmp.getWidth() + ",height=" + bmp.getHeight() + "大小：" + bmp.getByteCount() / 1024 + "kb");
                    }
                } else if (drawable instanceof GifDrawable) {
                    File file = new File(FileUtil.getImageDir() + "/" + OkHttpClientManager.getInstance().getFileName(image_url));
                    if (file.exists()){
                        shareGif(file.getAbsolutePath(), mActivity, mAdapter, position);
                    }else {
                        OkHttpClientManager.downLoadAsyn(image_url, FileUtil.getImageDir(), new OkHttpClientManager.ResultCallback() {
                            @Override
                            public void onError(Call request, Exception e) {
//                                showtToast(e.getMessage());
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(Object response) {
//                                showtToast("下载成功");
                                final String path = (String) response;
                                shareGif(path, mActivity, mAdapter, position);
                            }
                        });
                    }
                }

            }
        });
    }

    private void shareGif(final String path, final BaseActivity mActivity, final RecyclerAdapter mAdapter, final int position) {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                WxShareUtil.shareGif(mActivity.wxapi, path);
                subscriber.onNext("success");
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (!TextUtils.isEmpty(s) && s.equals("success")){
                            final ImageInfo item = (ImageInfo) mAdapter.getItem(position);
                            item.setTime(System.currentTimeMillis());
                            Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.copyToRealmOrUpdate(item);
                                }
                            });
                        }
                    }
                });
    }

}
