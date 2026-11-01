package com.dozingcatsoftware.dodge;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdLoadListener;
import com.huawei.hms.ads.reward.RewardAdStatusListener;

/** Huawei Ads twin of the Play build's AdsUtils (banner + rewarded "continue" video). */
public class AdsUtils {

    private static RewardAd rewardedAd;
    private static boolean earnedReward;
    private static boolean rewardedLoading;

    /** Banner (only after Huawei Ads is initialized). */
    public static void showGoogleBannerAd(Activity activity, ViewGroup container) {
        container.post(() -> BannerHelper.load(activity, container));
    }

    public static void loadRewardAds(Context context) {
        if (rewardedAd != null || rewardedLoading || !AdsConsent.canRequestAds(context)) return;
        rewardedLoading = true;
        Context app = context.getApplicationContext();
        RewardAd ad = new RewardAd(app, AdsConsent.slot(app, R.string.hms_rewarded_id, R.string.hms_test_rewarded_id));
        ad.loadAd(new AdParam.Builder().build(), new RewardAdLoadListener() {
            @Override
            public void onRewardAdFailedToLoad(int errorCode) {
                rewardedLoading = false;
                rewardedAd = null;
            }

            @Override
            public void onRewardedLoaded() {
                rewardedLoading = false;
                rewardedAd = ad;
            }
        });
    }

    /** Result of a rewarded ad: {@code earned} is true when the player watched it to the reward. */
    public interface RewardCallback {
        void onClosed(boolean earned);
    }

    public static boolean isRewardedReady() {
        return rewardedAd != null && rewardedAd.isLoaded();
    }

    /**
     * Shows the preloaded rewarded ad. Returns false (and never calls {@code callback}) if none is ready;
     * otherwise {@code callback} runs once when the ad is closed or fails to show.
     */
    public static boolean showRewarded(Activity activity, RewardCallback callback) {
        final RewardAd ad = rewardedAd;
        if (ad == null || !ad.isLoaded()) {
            return false;
        }
        rewardedAd = null;
        earnedReward = false;
        // The game waits for callback: if show() never opens the ad, finish as "not earned" after 3 s.
        final boolean[] opened = {false};
        final boolean[] done = {false};
        ad.show(activity, new RewardAdStatusListener() {
            @Override
            public void onRewardAdOpened() {
                opened[0] = true;
            }

            @Override
            public void onRewarded(Reward reward) {
                earnedReward = true;
            }

            @Override
            public void onRewardAdClosed() {
                boolean earned = earnedReward;
                earnedReward = false;
                finish(earned);
            }

            @Override
            public void onRewardAdFailedToShow(int errorCode) {
                Log.e("TAG", "Reward ad failed to show: " + errorCode);
                finish(false);
            }

            private void finish(boolean earned) {
                if (done[0]) {
                    return;
                }
                done[0] = true;
                callback.onClosed(earned);
                loadRewardAds(activity);
            }
        });
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!opened[0] && !done[0]) {
                done[0] = true;
                callback.onClosed(false);
                loadRewardAds(activity);
            }
        }, 3000L);
        return true;
    }
}
