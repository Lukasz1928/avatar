package com.sm.avatar.chatbot.dialogflow;

import android.os.AsyncTask;

import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.sm.avatar.AsyncResponse;

public class DialogflowRequestTask extends AsyncTask<Void, Void, DetectIntentResponse> {

    private AsyncResponse asyncResponse;
    private SessionName sessionName;
    private SessionsClient sessionsClient;
    private QueryInput queryInput;

    public DialogflowRequestTask(AsyncResponse asyncResponse, SessionName sessionName, SessionsClient sessionsClient, QueryInput queryInput) {
        super();
        this.asyncResponse = asyncResponse;
        this.sessionName = sessionName;
        this.sessionsClient = sessionsClient;
        this.queryInput = queryInput;
    }

    @Override
    protected DetectIntentResponse doInBackground(Void... voids) {
        try {
            DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
                            .setSession(sessionName.toString())
                            .setQueryInput(queryInput)
                            .build();
            return sessionsClient.detectIntent(detectIntentRequest);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(DetectIntentResponse response) {
        if(response != null) {
            String msg = response.getQueryResult().getFulfillmentText();
            if(msg.equals("")) {
                String noTextResponseText = "I could not understand your query.";
                this.asyncResponse.taskFinished(noTextResponseText);
            }
            else {
                this.asyncResponse.taskFinished(msg);
            }
        }
        else {
            this.asyncResponse.taskFailed();
        }
    }
}
