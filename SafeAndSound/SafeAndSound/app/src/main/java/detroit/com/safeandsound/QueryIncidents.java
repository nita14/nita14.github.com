package detroit.com.safeandsound;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-02-03.
 */
public final class QueryIncidents extends AsyncTask<String, Integer, String[]> {
    private Context mContext;
    private ServiceFeatureTable mServiceFeatureTable;
    private QueryParameters query;

    public QueryIncidents(Context context, QueryParameters query, String fsUrl) {
        mContext = context;
        this.query = query;
        mServiceFeatureTable = new ServiceFeatureTable(fsUrl);

    }

    @Override
    protected String[] doInBackground(String... string) {
        publishProgress(0);
        String items[] = null;
        try {
            ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);
            FeatureQueryResult result = future.get();
            int i = 0;
            final HashMap<String, Integer> sum = new HashMap<>();
            for (Feature f : result) {
                String atValue = f.getAttributes().get("CATEGORY").toString();
                i++;
                if (!sum.containsKey(atValue)) {
                    sum.put(atValue, 1);
                } else {
                    sum.put(atValue, sum.get(atValue) + 1);
                }
            }

            HashMap<Integer, String> sum1 = new HashMap<>();
            for (Map.Entry<String, Integer> entry : sum.entrySet()) {
                sum1.put(entry.getValue(), entry.getKey());
            }

            TreeMap<Integer, String> tree = new TreeMap<>(Collections.reverseOrder());
            tree.putAll(sum1);

            LinkedHashMap<Integer, String> hashFinal = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> entry : tree.entrySet()) {
                hashFinal.put(entry.getKey(), entry.getValue());
            }

            items = new String[sum1.size()];
            int j = 0;
            for (Integer key : hashFinal.keySet()) {
                items[j] = hashFinal.get(key) + " - " + String.valueOf(key);
                j++;
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            return items;
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        Toast.makeText(mContext, mContext.getResources().getString(R.string.searching_for_incidents), Toast.LENGTH_SHORT).show();
    }

    protected void onPostExecute(String[]  result) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext, R.style.MyDialogTheme);
        builder1.setItems(result, null);
        builder1.create();
        builder1.show();

    }
}
