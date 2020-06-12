package com.sm.avatar.chatbot.dialogflow;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.sm.avatar.AsyncResponse;
import com.sm.avatar.chatbot.ChatbotHandler;

import java.io.IOException;
import java.util.UUID;

public class DialogflowChatbotHandler extends ChatbotHandler {

    private AsyncResponse asyncResponse;
    private SessionName sessionName;
    private SessionsClient sessionsClient;
    private String uuid;

    public DialogflowChatbotHandler(AsyncResponse asyncResponse, GoogleCredentials credentials) throws IOException {
        this.asyncResponse = asyncResponse;

        SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
        SessionsSettings settings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

        String projectId = ((ServiceAccountCredentials)credentials).getProjectId();
        uuid = UUID.randomUUID().toString();
        this.sessionsClient = SessionsClient.create(settings);
        this.sessionName = SessionName.of(projectId, uuid);
    }

    @Override
    public void requestResponse(String message) {
        QueryInput queryInput = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).build()).build();
        new DialogflowRequestTask(this.asyncResponse, this.sessionName, this.sessionsClient, queryInput).execute();
    }
}
