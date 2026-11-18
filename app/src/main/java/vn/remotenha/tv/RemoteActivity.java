package vn.remotenha.tv;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.concurrent.*;

public final class RemoteActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout panel;
    private EditText address;
    private TextView status;
    private boolean busy;
    private int dp(int n) {return Math.round(getResources().getDisplayMetrics().density*n);}
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20),dp(32),dp(20),dp(24));
        panel.setBackgroundColor(0xff15232a);
        scroll.addView(panel); setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("Remote Nhà Mình"); title.setTextSize(30); panel.addView(title);
        TextView label = new TextView(this);
        label.setText("Địa chỉ IP của TV"); panel.addView(label);
        address = new EditText(this);
        address.setSingleLine(); address.setInputType(17);
        address.setText(getPreferences(0).getString("ip","192.168.1.4"));
        panel.addView(address);
        status = new TextView(this); status.setText("Chọn một phím để điều khiển TV.");
        panel.addView(status);
        row(new String[]{"▲"},new int[]{19});
        row(new String[]{"◀","OK","▶"},new int[]{21,23,22});
        row(new String[]{"▼"},new int[]{20});
        row(new String[]{"Quay lại","Trang chủ"},new int[]{4,3});
        row(new String[]{"Âm lượng −","Âm lượng +"},new int[]{25,24});
        row(new String[]{"Tắt tiếng","Menu"},new int[]{164,82});
    }
    private void row(String[] names,int[] keys) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER); panel.addView(line);
        for (int i=0;i<keys.length;i++) {
            Button button = new Button(this);
            String name=names[i]; int key=keys[i];
            button.setText(name); button.setAllCaps(false);
            line.addView(button,new LinearLayout.LayoutParams(
                names.length==1?dp(100):0,dp(60),names.length==1?0:1));
            button.setOnClickListener(v -> press(name,key));
        }
    }
    private void press(String name,int key) {
        String host=address.getText().toString().trim();
        if (!android.util.Patterns.IP_ADDRESS.matcher(host).matches() || host.contains(":")) {
            address.setError("Nhập địa chỉ IPv4 của TV"); return;
        }
        if (busy) return;
        busy=true;
        getPreferences(0).edit().putString("ip",host).apply();
        status.setText("Đang gửi phím "+name+"…");
        worker.execute(() -> {
            String result;
            try {MiLink.press(host,key); result="TV đã xác nhận phím "+name;}
            catch (Exception e) {result="Chưa nhận xác nhận từ "+host+":6091 ("+e.getClass().getSimpleName()+")";}
            final String message=result;
            runOnUiThread(() -> {busy=false; if (!isDestroyed()) status.setText(message);});
        });
    }
    @Override protected void onDestroy() {worker.shutdownNow(); super.onDestroy();}
}
