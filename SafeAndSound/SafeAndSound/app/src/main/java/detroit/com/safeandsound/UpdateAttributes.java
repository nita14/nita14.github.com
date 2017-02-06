package detroit.com.safeandsound;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-02-03.
 */
public final class UpdateAttributes extends AsyncTask<String, Integer, String> {
    private ServiceFeatureTable mServiceFeatureTable;
    private ArcGISFeature agf;
    private Context mContext;

    public UpdateAttributes(Context mContext, ArcGISFeature agf, ServiceFeatureTable mServiceFeatureTable) {
        this.agf = agf;
        this.mServiceFeatureTable = mServiceFeatureTable;
        this.mContext = mContext;
    }

    @Override
    protected String doInBackground(String... string) {
        String str = null;

        String sharedVal = String.valueOf(agf.getAttributes().get("SharedTo"));
        if (sharedVal.equals("null")){
            agf.getAttributes().put("SharedTo", DisplayMap.getSharedPreferences().getString(Settings.USER_NAME, ""));
        } else {
            String sharedValNew = sharedVal + ";" + DisplayMap.getSharedPreferences().getString(Settings.USER_NAME, "");
            agf.getAttributes().put("SharedTo", sharedValNew);
        }

        ListenableFuture<Void> tableUpdated = mServiceFeatureTable.updateFeatureAsync(agf);
        tableUpdated.addDoneListener(new Runnable() {
            @Override
            public void run() {
                ListenableFuture<List<FeatureEditResult>> fCommited = mServiceFeatureTable.applyEditsAsync();
                try {
                    List<FeatureEditResult> resultsFs = fCommited.get();
                    if (resultsFs != null && resultsFs.size() > 0 && !resultsFs.get(0).hasCompletedWithErrors()) {
                        String fOutID = String.valueOf(resultsFs.get(0).getObjectId());
                        if (fOutID.equalsIgnoreCase(String.valueOf(agf.getAttributes().get("OBJECTID")))) {
                            Log.e("Update Attributes", "Success");

                            Set<String> hs = DisplayMap.getSharedPreferences().getStringSet(Settings.MY_FRIENDS, new HashSet<String>());
                            hs.add(String.valueOf(agf.getAttributes().get("OBJECTID")));
                            DisplayMap.getSharedPreferences().edit().clear().putStringSet(Settings.MY_FRIENDS, hs).commit();
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        return str;
    }

    protected void onProgressUpdate(Integer... progress) {
        Toast.makeText(mContext, mContext.getResources().getString(R.string.updating_atts), Toast.LENGTH_SHORT).show();

    }

    protected void onPostExecute(String result) {

    }
}
