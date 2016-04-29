package com.firebase.ui.auth.choreographer;

public interface Controller {
    public static final int DEFAULT_INIT_FLOW_ID = -1;
    public static final int BLOCK_AT_CURRENT_ACTIVITY_ID = -2;
    public static final int FINISH_FLOW_ID = -3;
    public static final int START_NEW_FLOW_ID = -4;

    Action next(Result result);
}
