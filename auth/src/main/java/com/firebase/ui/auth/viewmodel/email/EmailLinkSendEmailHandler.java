package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.util.data.Utils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;

public class EmailLinkSendEmailHandler extends AuthViewModelBase<String> {
    private static final int SESSION_ID_LENGTH = 10;

    public EmailLinkSendEmailHandler(Application application) {
        super(application);
    }

    public void sendSignInLinkToEmail(@NonNull final String email, @NonNull final
    ActionCodeSettings actionCodeSettings) {
        if (getAuth() == null) {
            return;
        }
        setResult(Resource.<String>forLoading());

        final String anonymousUserId = AuthOperationManager.getInstance().canUpgradeAnonymous
                (getAuth(), getArguments()) ? getAuth().getCurrentUser().getUid() : null;
        final String sessionId =
                Utils.generateRandomAlphaNumericString(SESSION_ID_LENGTH);
        getAuth().sendSignInLinkToEmail(email, actionCodeSettings)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            EmailLinkPersistenceManager.getInstance().saveEmail(getApplication(),
                                    email, sessionId, anonymousUserId);
                            setResult(Resource.forSuccess(email));
                        } else {
                            setResult(Resource.<String>forFailure(task.getException()));
                        }
                    }
                });
    }
}
