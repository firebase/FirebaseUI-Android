package com.firebase.ui.auth.ui;

import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;

/**
 * View (Activity or Fragment, normally) that can respond to progress events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ProgressView {

    void showProgress(@StringRes int message);

    void hideProgress();

}
