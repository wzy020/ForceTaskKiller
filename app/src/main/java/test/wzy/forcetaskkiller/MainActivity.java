package test.wzy.forcetaskkiller;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

    private HashSet<String> mAppsSet = new HashSet<>();
    private ListView mAppsListView;
    private ArrayAdapter<String> mAdapter;
    private ActivityManager am;
    private WorkThread mWorkThread;

    private final int MSG_KILL = 0;
    private final int MSG_FRESH = 1;


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
        mAppsListView = findViewById(R.id.app_list);
        mAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
        mAppsListView.setAdapter(mAdapter);

        mWorkThread = new WorkThread();
        mWorkThread.start();

        mAppsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String pkgName =(String) ((TextView)view).getText();
                Message msg = new Message();
                msg.what = MSG_KILL;
                msg.obj = pkgName;
                mWorkThread.workHandler.sendMessage(msg);
            }
        });

        freshList();
    }

    @Override
    protected void onDestroy() {
        if(mWorkThread!=null) {
            mWorkThread.workHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    private Handler uiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_FRESH){
                freshList();
            }
        }
    };

    private void freshList(){
        //String[] mPkgNames = execRootCmd("dumpsys activity p | grep trm:'.*[a-z]*\\..*' | grep -o '[a-z]*\\..*\\.[a-z]*'").split("\r\n");
        //String[] mFeatures = execRootCmd("dumpsys activity p | grep trm:'.*[a-z]*\\..*' | grep -o '(.*)'").replace("(","").replace(")","").split("\r\n");
        mAppsSet.clear();
        String[] lines = execRootCmd("dumpsys activity p | grep -o '[a-z]*\\..*(.*)'").replaceAll("\\/[a-z0-9].* \\("," ").replace(")","").split("\r\n");
        for(int i=0; i<lines.length; i++){
            String str=lines[i];
            if(str.contains("cch") || str.endsWith("services")){
                mAppsSet.add(str.replaceAll(" .*","").replaceAll(":.*",""));
            }
        }
        mAdapter.clear();
        mAdapter.addAll(mAppsSet);
        mAdapter.notifyDataSetChanged();
    }

    private class WorkThread extends Thread{
        public Handler workHandler;

        @Override
        public void run() {
            Looper.prepare();
            workHandler = new Handler(){
                public void handleMessage (Message msg) {
                    if(msg.what == MSG_KILL){
                        String pkg = (String)msg.obj;
                        am.killBackgroundProcesses(pkg);
                        execRootCmd("am force-stop "+pkg);
                        Message msg2UI = new Message();
                        msg2UI.what = MSG_FRESH;
                        uiHandler.sendMessage(msg2UI);
                    }
                }
            };
            Looper.loop();
        }
    }



}
