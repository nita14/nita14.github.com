package detroit.com.safeandsound;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-02-03.
 */
public final class MakeGeocoding extends AsyncTask<String, Integer, String> {
    private Context mContext;
    private GeocodeParameters geocodeParameters;
    private LocatorTask locatorTask;
    private String query;

    public MakeGeocoding(Context context, String query, GeocodeParameters geocodeParameters) {
        mContext = context;
        this.query = query;
        this.locatorTask = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
        this.geocodeParameters = geocodeParameters;


    }

    @Override
    protected String doInBackground(String... string) {
        publishProgress(0);
        String matchAddr = null;
        try {
            ListenableFuture<List<GeocodeResult>> results = locatorTask.geocodeAsync(query, geocodeParameters);
            List<GeocodeResult> geocodes = results.get();
            if (!geocodes.isEmpty()) {
                GeocodeResult geocode = geocodes.get(0);
                matchAddr = geocode.getAttributes().get("Match_addr").toString() + ";" +
                        String.valueOf(geocode.getDisplayLocation().getX() +";" +
                                String.valueOf(geocode.getDisplayLocation().getY()));
                HashMap<String, Object> atts = new HashMap<>();
                atts.put("Address", matchAddr);
                Graphic marker = new Graphic(geocode.getDisplayLocation(), atts, DisplayMap.getStopSymbolPoint());
                DisplayMap.getStopsOverlay().getGraphics().add(marker);

            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            return matchAddr;
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        Toast.makeText(mContext, mContext.getResources().getString(R.string.searching_for_address), Toast.LENGTH_LONG).show();
    }

    protected void onPostExecute(String result) {
        if (result != null) {
            Toast.makeText(mContext, result.split(";")[0], Toast.LENGTH_LONG).show();
            DisplayMap.getMapView().setViewpointCenterAsync(new Point(Double.valueOf(result.split(";")[1]),
                    Double.valueOf(result.split(";")[2]),
                    DisplayMap.getMapView().getSpatialReference()));
        } else {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.no_address_found), Toast.LENGTH_LONG).show();
        }
    }
}
