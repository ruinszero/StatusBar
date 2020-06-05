package com.ruinszero.statusbar;

import android.app.Dialog;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Request manager retriever.
 *
 * @author geyifeng
 * @date 2019 /4/12 4:21 PM
 */
class RequestManagerRetriever implements Handler.Callback {

    private String mTag = ImmersionBar.class.getName();

    private Handler mHandler;

    private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

    private static class Holder {
        private static final RequestManagerRetriever INSTANCE = new RequestManagerRetriever();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static RequestManagerRetriever getInstance() {
        return Holder.INSTANCE;
    }

    private RequestManagerRetriever() {
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    private final Map<FragmentManager, RequestManagerFragment> mPendingFragments = new HashMap<>();
    private final Map<FragmentManager, SupportRequestManagerFragment> mPendingSupportFragments = new HashMap<>();

    /**
     * Get immersion bar.
     *
     * @param activity the activity
     * @return the immersion bar
     */
    public ImmersionBar get(AppCompatActivity activity) {
        checkNotNull(activity, "activity is null");
        String tag = mTag + System.identityHashCode(activity);
        return getSupportFragment(activity.getSupportFragmentManager(), tag).get(activity);
    }

    /**
     * Get immersion bar.
     *
     * @param fragment the fragment
     * @param isOnly   the is only
     * @return the immersion bar
     */
    public ImmersionBar get(Fragment fragment, boolean isOnly) {
        checkNotNull(fragment, "fragment is null");
        checkNotNull(fragment.getActivity(), "fragment.getActivity() is null");
        if (fragment instanceof DialogFragment) {
            checkNotNull(((DialogFragment) fragment).getDialog(), "fragment.getDialog() is null");
        }
        String tag = mTag;
        if (isOnly) {
            tag += fragment.getClass().getName();
        } else {
            tag += System.identityHashCode(fragment);
        }
        return getSupportFragment(fragment.getChildFragmentManager(), tag).get(fragment);
    }


    /**
     * Get immersion bar.
     *
     * @param activity the activity
     * @param dialog   the dialog
     * @return the immersion bar
     */
    public ImmersionBar get(AppCompatActivity activity, Dialog dialog) {
        checkNotNull(activity, "activity is null");
        checkNotNull(dialog, "dialog is null");
        String tag = mTag + System.identityHashCode(dialog);
        return getSupportFragment(activity.getSupportFragmentManager(), tag).get(activity, dialog);
    }

    /**
     * Destroy.
     *
     * @param fragment the fragment
     */
    public void destroy(Fragment fragment, boolean isOnly) {
        if (fragment == null) {
            return;
        }
        String tag = mTag;
        if (isOnly) {
            tag += fragment.getClass().getName();
        } else {
            tag += System.identityHashCode(fragment);
        }
        getSupportFragment(fragment.getChildFragmentManager(), tag, true);
    }

    /**
     * Destroy.
     *
     * @param activity the activity
     * @param dialog   the dialog
     */
    public void destroy(AppCompatActivity activity, Dialog dialog) {
        if (activity == null || dialog == null) {
            return;
        }
        String tag = mTag + System.identityHashCode(dialog);
        {
            SupportRequestManagerFragment fragment = getSupportFragment(activity.getSupportFragmentManager(), tag, true);
            if (fragment != null) {
                fragment.get(activity, dialog).onDestroy();
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean handled = true;
        switch (msg.what) {
            case ID_REMOVE_FRAGMENT_MANAGER:
                FragmentManager fm = (FragmentManager) msg.obj;
                mPendingFragments.remove(fm);
                break;
            case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
                FragmentManager supportFm = (FragmentManager) msg.obj;
                mPendingSupportFragments.remove(supportFm);
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }

    private RequestManagerFragment getFragment(FragmentManager fm, String tag) {
        return getFragment(fm, tag, false);
    }

    private RequestManagerFragment getFragment(FragmentManager fm, String tag, boolean destroy) {
        RequestManagerFragment fragment = (RequestManagerFragment) fm.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = mPendingFragments.get(fm);
            if (fragment == null) {
                if (destroy) {
                    return null;
                }
                fragment = new RequestManagerFragment();
                mPendingFragments.put(fm, fragment);
                fm.beginTransaction().add(fragment, tag).commitAllowingStateLoss();
                mHandler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        if (destroy) {
            fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
            return null;
        }
        return fragment;
    }

    private SupportRequestManagerFragment getSupportFragment(FragmentManager fm, String tag) {
        return getSupportFragment(fm, tag, false);
    }

    private SupportRequestManagerFragment getSupportFragment(FragmentManager fm, String tag, boolean destroy) {
        SupportRequestManagerFragment fragment = (SupportRequestManagerFragment) fm.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = mPendingSupportFragments.get(fm);
            if (fragment == null) {
                if (destroy) {
                    return null;
                }
                fragment = new SupportRequestManagerFragment();
                mPendingSupportFragments.put(fm, fragment);
                fm.beginTransaction().add(fragment, tag).commitAllowingStateLoss();
                mHandler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        if (destroy) {
            fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
            return null;
        }
        return fragment;
    }

    private static <T> void checkNotNull(@Nullable T arg, @NonNull String message) {
        if (arg == null) {
            throw new NullPointerException(message);
        }
    }
}
