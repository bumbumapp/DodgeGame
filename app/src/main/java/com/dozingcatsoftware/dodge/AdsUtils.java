package com.dozingcatsoftware.dodge;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdsUtils {

    public static RewardedAd rewardedAd;
    public static boolean ISREWARDED = false;
    private static boolean rewardedLoading;

    /** Anchored adaptive banner (only after consent allows ad requests). */
    public static void showGoogleBannerAd(Activity activity, ViewGroup container) {
        container.post(() -> BannerHelper.load(activity, container));
    }

    public static void loadRewardAds(Context context) {
        if (rewardedAd != null || rewardedLoading || !AdsConsent.canRequestAds(context)) return;
        rewardedLoading = true;
        RewardedAd.load(context.getApplicationContext(), context.getString(R.string.admob_rewarded_id),
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedLoading = false;
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                    }
                });
    }
}
