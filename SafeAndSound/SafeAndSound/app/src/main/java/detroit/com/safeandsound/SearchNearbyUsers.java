package detroit.com.safeandsound;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-02-03.
 */
public final class SearchNearbyUsers extends AsyncTask<String, Integer, List<ArcGISFeature>> {
    private Context mContext;
    private Point usrLocation;
    private ServiceFeatureTable mServiceFeatureTable;


    public SearchNearbyUsers(Context context, Point usrLocation, ServiceFeatureTable mServiceFeatureTable) {
        mContext = context;
        this.usrLocation = usrLocation;
        this.mServiceFeatureTable = mServiceFeatureTable;
    }

    @Override
    protected List<ArcGISFeature> doInBackground(String... string) {
        publishProgress(0);
        List<ArcGISFeature> listFeatures = new ArrayList<>();
        try {

            QueryParameters query = new QueryParameters();
            query.setReturnGeometry(true);
            query.setWhereClause("Name <>" + "'" + DisplayMap.getSharedPreferences().getString(Settings.USER_NAME, "") +"'");
            Polygon ply = GeometryEngine.buffer(usrLocation, 600);
            query.setGeometry(ply);
            query.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);
            query.setMaxFeatures(30);
            ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);
            FeatureQueryResult result = future.get();

            Iterator<Feature> iter = result.iterator();
            while(iter.hasNext()){
                ArcGISFeature f = (ArcGISFeature) iter.next();
                f.loadAsync();
                if(!f.getAttributes().get("Name").equals(DisplayMap.getSharedPreferences().getString(Settings.USER_NAME, ""))){
                    listFeatures.add(f);
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            return listFeatures;
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        Toast.makeText(mContext, mContext.getResources().getString(R.string.searching_nearby_users), Toast.LENGTH_LONG).show();
    }


    protected void onPostExecute(final List<ArcGISFeature> listUsers) {
        if (listUsers !=null) {
            final String[] userNames = new String[listUsers.size()];
            int i = 0;
            for (ArcGISFeature f : listUsers) {
                userNames[i] = String.valueOf(f.getAttributes().get("Name"));
                i++;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.MyDialogTheme);
            builder.setTitle(mContext.getResources().getString(R.string.select_user_add))
                    .setItems(userNames, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            new UpdateAttributes(mContext, listUsers.get(which), DisplayMap.getUsersFeaTable()).execute();
                        }
                    });

            builder.create();
            builder.show();
        }
    }
}
