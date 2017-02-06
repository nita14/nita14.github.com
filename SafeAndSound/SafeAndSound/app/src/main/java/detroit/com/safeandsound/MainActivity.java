package detroit.com.safeandsound;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean passLoginActivity = getSharedPreferences(Settings.SHARED_PREFERENCES, MODE_PRIVATE).getBoolean(Settings.PASS_LOGIN_ACTIVITY, false);
        if (passLoginActivity) {
            startActivity(new Intent(getApplicationContext(), DisplayMap.class));
            finish();
        }

        //hide toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

    }

    public void loginToApp(View view) {
        EditText editText = (EditText) findViewById(R.id.login);
        final String userName = editText.getText().toString();
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.logging_in), Toast.LENGTH_LONG).show();
        final ServiceFeatureTable mServiceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.login_url));
        QueryParameters query = new QueryParameters();
        query.setWhereClause("Name = '" + userName + "'");

        final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);

        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    FeatureQueryResult result = future.get();
                    if (result.iterator().hasNext()) {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.login_exist), Toast.LENGTH_SHORT).show();

                    } else {
                        java.util.Map<String, Object> attributes = new HashMap<String, Object>();
                        attributes.put("Name", userName);
                        Feature feature = mServiceFeatureTable.createFeature(attributes, new Point(0, 0));
                        final ListenableFuture<Void> resultAdded = mServiceFeatureTable.addFeatureAsync(feature);
                        resultAdded.addDoneListener(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    resultAdded.get();
                                    if (resultAdded.isDone()) {
                                        final ListenableFuture<List<FeatureEditResult>> edits = mServiceFeatureTable.applyEditsAsync();
                                        edits.addDoneListener(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    List<FeatureEditResult> featureEditResultsList = edits.get();
                                                    if (!featureEditResultsList.get(0).hasCompletedWithErrors()) {
                                                        getSharedPreferences(Settings.SHARED_PREFERENCES, MODE_PRIVATE).edit().putBoolean(Settings.PASS_LOGIN_ACTIVITY, true).commit();
                                                        getSharedPreferences(Settings.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(Settings.USER_NAME, userName).commit();
                                                        getSharedPreferences(Settings.SHARED_PREFERENCES, MODE_PRIVATE).edit().putString(Settings.USER_NAME_ID, String.valueOf(featureEditResultsList.get(0).getObjectId())).commit();
                                                        startActivity(new Intent(getApplicationContext(), DisplayMap.class));
                                                    }

                                                } catch (InterruptedException | ExecutionException e) {
                                                    Log.e("APPLY TO Server", e.getCause().getMessage());
                                                }

                                            }
                                        });
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.login_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.login_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(getResources().getString(R.string.app_name), e.getMessage());
                }
            }
        });
    }
}
