package detroit.com.safeandsound;

import android.graphics.Color;
import android.util.Log;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-01-15.
 */
public class RenderLayers {

    private static RenderLayers instance;
    private static String serviceUrl;
    private static String serviceUrlReferenceData;
    private static String serviceUrlIncidentData;
    private static SimpleFillSymbol incSymbolPoly = new SimpleFillSymbol(
            SimpleFillSymbol.Style.SOLID, Color.RED,
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1));
    private static SimpleMarkerSymbol incSymbolPoint;
    private static TextSymbol incSymbolText = new TextSymbol(10.0f, "I", Color.BLACK, TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
    private static Polygon bBox;
    static{
        incSymbolPoint = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 14.0f);
        incSymbolPoint.setOutline(
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f));

        PointCollection points = new PointCollection(SpatialReference.create(102100));
        points.add(new Point(-9272380.718, 5198595.825));
        points.add(new Point(-9274497.388, 5230557.555));
        points.add(new Point(-9227295.627, 5230134.22));
        points.add(new Point(-9227507.294, 5197325.822));

        bBox = new Polygon(points);

    }


    public static RenderLayers getInstance(String serviceUrlInputDensity, String serviceUrlRefData,
                                           String serviceUrlIncData) {
        if (instance == null) {
            instance = new RenderLayers();
            serviceUrl = serviceUrlInputDensity;
            serviceUrlReferenceData = serviceUrlRefData;
            serviceUrlIncidentData = serviceUrlIncData;
        }
        return instance;
    }

    public static String getServiceUrlIncidentData() {
        return serviceUrlIncidentData;
    }

    public SimpleMarkerSymbol getIncSymbolPoint() {
        return incSymbolPoint;
    }

    public List<FeatureLayer> addReferenceData() {
        List<FeatureLayer> fList = new ArrayList<>();
        fList.add(createFeatureLayer(serviceUrlReferenceData + "0", "Boundary", 1.0f));
        fList.add(createFeatureLayer(serviceUrlReferenceData + "1", "Buffer", 0.7f));
        return fList;
    }


    public static FeatureLayer visualizeSafety() {
        Calendar calendar = Calendar.getInstance();
        int layerId = mapHourToLayerId(calendar.get(Calendar.HOUR));
        return createFeatureLayer(serviceUrl + String.valueOf(layerId), "IncidentDensity", 0.7f);
    }

    private static FeatureLayer createFeatureLayer(String url, String id, Float opacity) {
        ServiceFeatureTable gft = new ServiceFeatureTable(url);
        FeatureLayer fl = new FeatureLayer(gft);
        fl.setId(id);
        fl.setMaxScale(1000);
        fl.setOpacity(opacity);
        return fl;
    }


    public static int mapHourToLayerId(int hour) {
        int layerId = 0;
        switch (hour) {
            case 1:
                layerId = 1;
                break;
            case 2:
                layerId = 2;
                break;
            case 3:
                layerId = 3;
                break;
            case 4:
                layerId = 4;
                break;
            case 5:
                layerId = 5;
                break;
            case 6:
                layerId = 6;
                break;
            case 7:
                layerId = 7;
                break;
            case 8:
                layerId = 8;
                break;
            case 9:
                layerId = 9;
                break;
            case 10:
                layerId = 10;
                break;
            case 11:
                layerId = 11;
                break;
            case 12:
                layerId = 0;
                break;
            case 0:
                layerId = 11;
                break;
        }
        return layerId;
    }

    public static UserCredential getUserCredentails() {
        return new UserCredential("x", "y");
    }

    public static ArcGISMapImageLayer visualizeTraffic(String url) {
        ArcGISMapImageLayer trafficLayer = new ArcGISMapImageLayer(url);
        trafficLayer.setId("TrafficLayer");
        trafficLayer.setCredential(getUserCredentails());
        return trafficLayer;
    }

    public static void addIncindentsGraphics() {
        ServiceFeatureTable sft = new ServiceFeatureTable(getServiceUrlIncidentData());
        sft.setCredential(getUserCredentails());

        QueryParameters query = new QueryParameters();
        query.setMaxFeatures(1000);
        query.setReturnGeometry(true);
        query.setGeometry(bBox);
        query.setOutSpatialReference(SpatialReference.create(102100));
        List<Graphic> list = new ArrayList<>();
        try {
            ListenableFuture<FeatureQueryResult> tableQueryResult = sft.queryFeaturesAsync(query);
            FeatureQueryResult result = tableQueryResult.get();
            for (Iterator<Feature> iter = result.iterator(); iter.hasNext(); ) {
                Feature feature = iter.next();
                Polygon poly = GeometryEngine.buffer(feature.getGeometry(), 10);
                list.add(new Graphic(poly, incSymbolPoly));
                list.add(new Graphic(feature.getGeometry(), incSymbolPoint));
                list.add(new Graphic(feature.getGeometry(), incSymbolText));
            }

            DisplayMap.getTrafficIncOverlay().getGraphics().addAll(list);

        } catch (InterruptedException | ExecutionException e) {
            Log.e("ERROR", e.getCause().getMessage());
        }
    }

    public Geometry getBbox() {
        return bBox;
    }
}
