package com.benatti.nicolas.cryptoexchanges;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

public class RetrieveCurrencyData extends AsyncTask<String, Void, ArrayList<String>> {

    // wrapper delle funzionalità dell'API
    private ApiConnectionManager apiConnector = ApiConnectionManager.getInstance();

    @Override
    protected ArrayList<String> doInBackground(String... strings) {

        /*ArrayList<String> responses = new ArrayList<>();

        for (String url : strings) {
            responses.add(apiConnector.sendGetRequest(url));
        }

        return responses;*/

        apiConnector.clearCache();

        for(String reqUrl : strings) {
            apiConnector.sendGetRequest(reqUrl);
        }

        return apiConnector.getResponses();
    }

    @Override
    protected void onPostExecute(ArrayList<String> strings) {
        //Toast.makeText(MainActivity.class, "executed " + strings.size() + " requests", Toast.LENGTH_SHORT).show();
        Log.d("DATA_RETRIEVAL_RESULT", "executed " + strings.size() + " requests");
    }
}
