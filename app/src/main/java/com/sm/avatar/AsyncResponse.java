package com.sm.avatar;

public interface AsyncResponse {
    void taskFinished(String result);
    void taskFailed();
}
