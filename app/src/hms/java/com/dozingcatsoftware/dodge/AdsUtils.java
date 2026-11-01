package com.dozingcatsoftware.dodge;

import android.app.Activity;
import android.content.Context;
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
        ad.show(activity, new RewardAdStatusListener() {
            @Override
            public void onRewarded(Reward reward) {
                earnedReward = true;
            }

            @Override
            public void onRewardAdClosed() {
                boolean earned = earnedReward;
                earnedReward = false;
                callback.onClosed(earned);
                loadRewardAds(activity);
            }

            @Override
            public void onRewardAdFailedToShow(int errorCode) {
                Log.e("TAG", "Reward ad failed to show: " + errorCode);
                callback.onClosed(false);
                loadRewardAds(activity);
            }
        });
        return true;
    }
}
