package com.example.brokerfi.token;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.brokerfi.R;

/** wBKC 椤甸潰缁熶竴鐨?SwipeRefreshLayout 閰嶇疆銆?*/
public final class TokenSwipeRefreshHelper {

    private static final int PROGRESS_START_DP = 20;
    private static final int PROGRESS_END_DP = 68;

    private TokenSwipeRefreshHelper() {
    }

    public static void bind(SwipeRefreshLayout refresh, Runnable onRefresh) {
        if (refresh == null) {
            return;
        }
        refresh.setColorSchemeResources(R.color.black);
        int start = dp(refresh, PROGRESS_START_DP);
        int end = dp(refresh, PROGRESS_END_DP);
        refresh.setProgressViewOffset(false, start, end);
        refresh.setOnRefreshListener(onRefresh::run);
    }

    /**
     * 涓嬫媺鍒锋柊锛氬己鍒舵媺鍙栭摼涓婁綑棰濓紝鍦ㄤ富绾跨▼鏇存柊 UI 骞跺仠姝㈠埛鏂板姩鐢汇€?
     */
    public static void refreshBalances(AppCompatActivity activity, SwipeRefreshLayout refresh,
                                       Runnable updateUiOnMainThread) {
        refreshBalances(activity, refresh, updateUiOnMainThread, null);
    }

    public static void refreshBalances(AppCompatActivity activity, SwipeRefreshLayout refresh,
                                       Runnable updateUiOnMainThread,
                                       Runnable onFetchFailed) {
        TokenBalanceCache.prefetchForce(activity, () -> activity.runOnUiThread(() -> {
            if (activity.isFinishing()) {
                stop(refresh);
                return;
            }
            try {
                if (updateUiOnMainThread != null) {
                    updateUiOnMainThread.run();
                }
            } finally {
                stop(refresh);
            }
        }), onFetchFailed);
    }

    public static void stop(SwipeRefreshLayout refresh) {
        if (refresh != null) {
            refresh.setRefreshing(false);
        }
    }

    private static int dp(SwipeRefreshLayout refresh, int valueDp) {
        return Math.round(valueDp * refresh.getResources().getDisplayMetrics().density);
    }
}


