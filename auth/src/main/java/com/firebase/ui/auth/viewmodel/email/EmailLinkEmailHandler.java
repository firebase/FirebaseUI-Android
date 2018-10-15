package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.data.client.EmailLinkPersistenceManager;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;

public class EmailLinkEmailHandler extends AuthViewModelBase<String> {
    public EmailLinkEmailHandler(Application application) {
        super(application);
    }

    public void sendSignInLinkToEmail(@NonNull final String email, @NonNull final
    ActionCodeSettings actionCodeSettings) {
        if (getAuth() == null) {
            return;
        }
        getAuth().sendSignInLinkToEmail(email, actionCodeSettings)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            EmailLinkPersistenceManager.getInstance().saveEmailForLink(getApplication(),
                                    email);
                            setResult(Resource.forSuccess(email));
                        } else {
                            setResult(Resource.<String>forFailure(task.getException()));
                        }
                    }
                });
    }

}
