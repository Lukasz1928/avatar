package com.sm.avatar.chatbot.iaml;

import com.sm.avatar.AsyncResponse;
import com.sm.avatar.chatbot.ChatbotHandler;

public class IamlChatbotHandler extends ChatbotHandler {

    private AsyncResponse asyncResponse;

    public IamlChatbotHandler(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
        //        AssetManager assets = getResources().getAssets();
//        File jayDir = new File(Environment.getExternalStorageDirectory().toString() + "/rosie/bots/Rosie");
//        boolean b = jayDir.mkdirs();
//        if (jayDir.exists()) {
//            //Reading the file
//            try {
//                for (String dir : assets.list("Rosie")) {
//                    File subdir = new File(jayDir.getPath() + "/" + dir);
//                    boolean subdir_check = subdir.mkdirs();
//                    for (String file : assets.list("Rosie/" + dir)) {
//                        File f = new File(jayDir.getPath() + "/" + dir + "/" + file);
//                        if (f.exists()) {
//                            continue;
//                        }
//                        InputStream in = null;
//                        OutputStream out = null;
//                        in = assets.open("Rosie/" + dir + "/" + file);
//                        out = new FileOutputStream(jayDir.getPath() + "/" + dir + "/" + file);
//                        //copy file from assets to the mobile's SD card or any secondary memory
//                        copyFile(in, out);
//                        in.close();
//                        out.flush();
//                        out.close();
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
////get the working directory
//        MagicStrings.root_path = Environment.getExternalStorageDirectory().toString() + "/rosie";
//        System.out.println("Working Directory = " + MagicStrings.root_path);
//        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
////Assign the AIML files to bot for processing
//        bot = new Bot("Rosie", MagicStrings.root_path, "chat");
//        chat = new Chat(bot);
    }

    @Override
    public void requestResponse(String message) {
        asyncResponse.taskFinished("I don't understand");
    }
}
