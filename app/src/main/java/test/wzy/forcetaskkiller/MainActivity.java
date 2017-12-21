package test.wzy.forcetaskkiller;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashSet;

import static test.wzy.forcetaskkiller.Utils.upgradeRootPermission;
import static test.wzy.forcetaskkiller.Utils.execRootCmd;

public class MainActivity extends Activity {

    private HashSet<String> mApps = new HashSet<>();
    private ListView mAppsList;
    private ArrayAdapter<String> mAdapter;
    private ActivityManager am;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (upgradeRootPermission(getPackageCodePath())){
            init();
        } else {
            Toast.makeText(this, "need root permission!", Toast.LENGTH_SHORT).show();
        }
    }

    private void init(){
        am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        mAppsList = findViewById(R.id.app_list);
        mAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
        mAppsList.setAdapter(mAdapter);

        mAppsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String pkgName =(String) ((TextView)view).getText();
                AsyncTask task = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        String str = (String) objects[0];
                        am.killBackgroundProcesses(str);
                        execRootCmd("am force-stop "+str);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        super.onPostExecute(o);
                        freshList();
                    }
                };
                task.execute(pkgName);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        freshList();


    }


    private void freshList(){
        //String[] mPkgNames = execRootCmd("dumpsys activity p | grep trm:'.*[a-z]*\\..*' | grep -o '[a-z]*\\..*\\.[a-z]*'").split("\r\n");
        //String[] mFeatures = execRootCmd("dumpsys activity p | grep trm:'.*[a-z]*\\..*' | grep -o '(.*)'").replace("(","").replace(")","").split("\r\n");
        mApps.clear();
        String[] lines = execRootCmd("dumpsys activity p | grep -o '[a-z]*\\..*(.*)'").replaceAll("\\/[a-z0-9].* \\("," ").replace(")","").split("\r\n");
        for(int i=0; i<lines.length; i++){
            String str=lines[i];
            if(str.contains("cch") || str.endsWith("services")){
                mApps.add(str.replaceAll(" .*","").replaceAll(":.*",""));
            }
        }
        mAdapter.clear();
        mAdapter.addAll(mApps);
        mAdapter.notifyDataSetChanged();
    }







}
