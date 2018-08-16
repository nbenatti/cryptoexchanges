package com.benatti.nicolas.cryptoexchanges;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {

    // elementi grafici (e affini)
    private Toolbar toolbar;
    private EditText from, to;
    private Spinner fromSpinner, toSpinner;
    private List<String> fromSpinnerData, toSpinnerData;
    private ArrayAdapter<String> fromSpinnerAdpt, toSpinnerAdpt;
    private Button refreshButton;

    // elenco dei cambi valuta (modificato durante aggiornamento dati)
    private Map<Pair, Double> exchanges;

    // richieste di conversione da mandare al server
    private  ArrayList<String> requests;

    // task che prende i dati dei cambi dal server (una volta all'avvio e ogni volta che lo sceglie l'utente)
    private RetrieveCurrencyData retrieveDataTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        from = findViewById(R.id.from_input);
        to = findViewById(R.id.to_input);
        fromSpinner = findViewById(R.id.from_spinner);
        toSpinner = findViewById(R.id.to_spinner);
        refreshButton = findViewById(R.id.refresh_button);
        exchanges = new ConcurrentHashMap<>();

        // riempi gli spinner

        fromSpinnerData = new ArrayList<>();
        toSpinnerData = new ArrayList<>();

        fillSpinnersData(fromSpinnerData, toSpinnerData);
        fromSpinner.setSelection(getIndexOf(fromSpinner, "Bitcoin\t\t[" + Utils.currencyCodes.get("Bitcoin") + "]"));
        toSpinner.setSelection(getIndexOf(toSpinner, "Euro\t\t[" + Utils.currencyCodes.get("Euro") + "]"));

        fromSpinnerAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fromSpinnerData);
        toSpinnerAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, toSpinnerData);

        fromSpinnerAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinnerAdpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromSpinner.setAdapter(fromSpinnerAdpt);
        toSpinner.setAdapter(toSpinnerAdpt);

        // prendi i dati dei cambi-valuta

        requests = new ArrayList<>();

        // non vengono eseguite richieste "simmetriche" (es. BTC-EUR, ma non EUR-BTC)
        for (int i = 0; i < Utils.currencyCodes.size(); ++i) {
            for(int j = i+1; j < Utils.currencyCodes.size(); ++j) {

                //Log.d("DUMP", Utils.currencyCodes.get(Utils.currencyNames.get(i)) + ", " + Utils.currencyCodes.get(Utils.currencyNames.get(j)));

                requests.add("https://forex.1forge.com/1.0.3/convert?from=" +
                        Utils.currencyCodes.get(Utils.currencyNames.get(i)) + "&to=" +
                        Utils.currencyCodes.get(Utils.currencyNames.get(j)) + "&quantity=1&api_key=" + Utils.apiKey);
            }
        }

        // delega il fetching dei dati a thread secondario

        updateExchanges(requests);

        Log.d("CAMBI_VALUTA_FINALI", exchanges.toString());

        from.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d("LISTENER_DATA", s.toString());

                Log.d("SPINNER_VALUE", fromSpinner.getSelectedItem() + ", " + toSpinner.getSelectedItem());

                String[] fromCoin = ((String)fromSpinner.getSelectedItem()).split("\t\t");
                String[] toCoin = ((String)toSpinner.getSelectedItem()).split("\t\t");

                if(fromCoin.equals(toCoin)) {

                    //Toast.makeText(null, "you cannot select the same currency", Toast.LENGTH_SHORT);
                }
                else {
                    if (!s.toString().isEmpty()) {
                        double conversionResult = convert(Double.parseDouble(s.toString()),
                                fromCoin[1].substring(1, fromCoin[1].length() - 1),
                                toCoin[1].substring(1, fromCoin[1].length() - 1));

                        to.setText(String.format(Locale.US, "%.2f", conversionResult));
                        //from.setText(String.format(Locale.US, "%.2f", Double.parseDouble(from.getText().toString())));
                    } else
                        to.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /* === gestione del menu opzioni === */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater minf = getMenuInflater();
        minf.inflate(R.menu.items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // per ora c'è solo un'opzione
        switch (item.getItemId()) {

            case R.id.stats:

                Intent openStatsPage = new Intent(this, StatsViewer.class);
                startActivity(openStatsPage);

                break;

            default:
                break;
        }

        return true;
    }

    public void fillSpinnersData(List<String> fsd, List<String> tsd) {

        for(String key : Utils.currencyCodes.keySet()) {

            fsd.add(key + "\t\t[" + Utils.currencyCodes.get(key) + "]");
            tsd.add(key + "\t\t[" + Utils.currencyCodes.get(key) + "]");
        }
    }

    public double convert(double fromImport, String fromCoin, String toCoin) {

        double res = exchanges.get(new Pair<>(fromCoin, toCoin));

        return res * fromImport;
    }

    private int getIndexOf(Spinner sp, String val) {

        for(int i = 0; i < sp.getCount(); ++i) {
            if(sp.getItemAtPosition(i).equals(val))
                return i;
        }

        return -1;
    }

    // eseguita quando si preme il pulsante di refresh, aggiorna i dati sui cambi valuta
    private void updateExchanges(ArrayList<String> requests) {

        ArrayList<String> responses = null;

        try {
            responses = new RetrieveCurrencyData().execute(requests.toArray(new String[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Log.d("CAMBI-VALUTA-JSON", responses.toString());

        // riempi la mappa dei cambi

        JSONObject parser = null;

        for(String response : responses) {

            try {
                parser = new JSONObject(response);

                // recupera i nomi delle monete
                String[] splitted = parser.getString("text").split(" ");
                String fromCoin = splitted[1], toCoin = splitted[splitted.length-1];
                double quantity = parser.getDouble("value");

                exchanges.put(new Pair<String, String>(fromCoin, toCoin), quantity);
            }
            catch(JSONException e) {

                Log.d("ERROR", "error in parsing JSON response");
            }
        }

        // ricava i dati dei cambi simmetrici

        for (Pair it : exchanges.keySet()) {

            exchanges.put(new Pair(it.second, it.first), 1 / exchanges.get(it));
        }
    }

    // wrapper del metodo eseguito dal refreshButton
    public void grabData(View v) {

        updateExchanges(requests);
    }
}
