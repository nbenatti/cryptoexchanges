package com.benatti.nicolas.cryptoexchanges;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class StatsViewer extends Activity {

    private TextView marketStatus, quotaLimit, quotaUsed, quotaRemaining, hoursUntilReset;

    private RetrieveCurrencyData downloader;

    private ArrayList<String> requests, responses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_viewer);

        requests = new ArrayList<>();
        responses = new ArrayList<>();

        downloader = new RetrieveCurrencyData();

        marketStatus = (TextView)findViewById(R.id.market_open);
        quotaLimit = (TextView)findViewById(R.id.quota_limit);
        quotaUsed = (TextView)findViewById(R.id.quota_used);
        quotaRemaining = (TextView)findViewById(R.id.quota_rem);
        hoursUntilReset = (TextView)findViewById(R.id.hours_until_reset);

        requests.add("https://forex.1forge.com/1.0.3/market_status?api_key="+Utils.apiKey);
        requests.add("https://forex.1forge.com/1.0.3/quota?api_key="+Utils.apiKey);

        // prendi i dati
        try {
            responses = downloader.execute(requests.toArray(new String[0])).get();
        }
        catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // mostrali a video
        try {
            JSONObject parser = new JSONObject(responses.get(0));

            boolean isOpen = parser.getBoolean("market_is_open");

            String msString = (isOpen) ? "OPEN" : "CLOSED";

            // formatta la voce dell'elenco
            SpannableStringBuilder ssb = new SpannableStringBuilder(getResources().getString(R.string.market_status_in_menu) + "\t" + msString);
            ForegroundColorSpan color = new ForegroundColorSpan((isOpen) ? Color.rgb(74, 153, 18) : Color.rgb(186, 18, 18));
            // indici del frammento di stringa da colorare
            Pair<Integer, Integer> indexes = new Pair<>(ssb.length() - msString.length(), ssb.length());

            ssb.setSpan(color, indexes.first, indexes.second, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            marketStatus.setText(ssb);
            //marketStatus.setTextColor((isOpen) ? Color.GREEN : Color.RED);

            parser = new JSONObject(responses.get(1));

            quotaLimit.setText(getResources().getString(R.string.quota_limit_in_menu) + "\t" + parser.getInt("quota_limit"));
            quotaUsed.setText(getResources().getString(R.string.quota_used_in_menu) + "\t" + parser.getInt("quota_used"));
            quotaRemaining.setText(getResources().getString(R.string.quota_rem_in_menu) + "\t" + parser.getInt("quota_remaining"));
            hoursUntilReset.setText(getResources().getString(R.string.hours_until_reset_in_menu) + "\t" + parser.getInt("hours_until_reset"));
        }
        catch(JSONException e) {
            System.out.println("ERROR: something went wrong in parsing response data");
        }
    }

    /*public void createStatItem() {


    }*/
}
