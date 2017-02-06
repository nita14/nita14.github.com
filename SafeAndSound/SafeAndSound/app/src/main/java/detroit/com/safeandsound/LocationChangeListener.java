package detroit.com.safeandsound;

import android.util.Log;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeodeticDistanceResult;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-01-14.
 */
public class LocationChangeListener {
    public static SpatialReference sp = SpatialReference.create(102100);
    private static Point lastPosition = new Point(0, 0, sp);
    public static LinearUnit linearUnit = new LinearUnit(LinearUnitId.METERS);
    public static AngularUnit angularUnit = new AngularUnit(AngularUnitId.DEGREES);
    private static ServiceFeatureTable mServiceFeatureTable;

    public LocationChangeListener (ServiceFeatureTable mServiceFeatureTable) {
        this.mServiceFeatureTable = mServiceFeatureTable;
    }

    public static Point getLastPosition() {
        return lastPosition;
    }

    public Point checkForUpdates(Point newPoint) {

        Point pkt = (Point) GeometryEngine.project(newPoint, sp);
        GeodeticDistanceResult geod = GeometryEngine.distanceGeodetic(getLastPosition(), pkt, linearUnit, angularUnit, GeodeticCurveType.GEODESIC);
        Double distanceLastAndNew = geod.getDistance();
        if (distanceLastAndNew > 10) {
            lastPosition = pkt;
            updateMyLocation(pkt);
            return newPoint;
        } else {
            return null;
        }


    }

    private void updateMyLocation(Point pkt) {
        ArcGISFeature curPoint =  DisplayMap.getUserPos();
        curPoint.loadAsync();
        curPoint.setGeometry(pkt);
        ListenableFuture<Void> tableUpdated = mServiceFeatureTable.updateFeatureAsync(curPoint);
        tableUpdated.addDoneListener(new Runnable() {
            @Override
            public void run() {
                ListenableFuture<List<FeatureEditResult>> fCommited = mServiceFeatureTable.applyEditsAsync();
                try {
                    List<FeatureEditResult> resultsFs = fCommited.get();
                    if (resultsFs != null && resultsFs.size() > 0 && !resultsFs.get(0).hasCompletedWithErrors()) {
                        String  fOutID = String.valueOf(resultsFs.get(0).getObjectId());
                        if (fOutID.equalsIgnoreCase(DisplayMap.getSharedPreferences().getString(Settings.USER_NAME_ID, ""))){
                            Log.e("Update Location", "Success");
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
