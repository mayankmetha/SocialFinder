package com.mayank.socialfinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.collection.LruCache;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.Request;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ArrayList<SocialModel> keys;
    public static int distro = 0;
    SocialAdapter adapter;
    ArrayList<String> agents;
    SecureRandom sr;
    JSONArray socialPlatforms;
    public static NotificationManager updateNotificationManager;
    public NotificationCompat.Builder updateNotify;
    public static Config config;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 0;
    static double currentVersion;
    static double newVersion;

    String username;

    @SuppressLint("PackageManagerGetSignatures")
    private void getReleaseSigningHash() {
        String GitHubRelease = "0Xv/I6xP6Q1wKbIqCgXi4CafhKZtOZLOR575TiqN93s=";
        String debug = "6E9Beocz6JC8xJXYO7Wu3nZQwZW1bKb1M004n/xR2BE=";
        ArrayList<String> hashList = new ArrayList<>();
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.PackageInfoFlags.of(PackageManager.	GET_SIGNING_CERTIFICATES));
            } else {
                info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            }
            if (info.signingInfo.hasMultipleSigners()) {
                for (Signature signature: info.signingInfo.getApkContentsSigners()) {
                    MessageDigest md = MessageDigest.getInstance("SHA256");
                    md.update(signature.toByteArray());
                    hashList.add(new String(Base64.encode(md.digest(), 0)));
                }
            } else {
                for (Signature signature: info.signingInfo.getSigningCertificateHistory()) {
                    MessageDigest md = MessageDigest.getInstance("SHA256");
                    md.update(signature.toByteArray());
                    hashList.add(new String(Base64.encode(md.digest(), 0)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < hashList.size(); i++) {
            if(hashList.get(i).trim().equals(GitHubRelease)) {
                distro = R.string.releaseGitHub;
            } else if(hashList.get(i).trim().equals(debug)) {
                distro = R.string.releaseTest;
            } else {
                distro = R.string.releaseOthers;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        config = new Config(this);
        switch (config.getDarkMode()) {
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.INSTALL_PACKAGES}, PERMISSION_REQUEST_NOTIFICATION);
            }
        }

        getReleaseSigningHash();
        updateInit();
        openSettings();
        exportList();

        sr = new SecureRandom();

        AppCompatEditText usernameEditText = findViewById(R.id.username_input);
        username = "";
        AppCompatButton button = findViewById(R.id.run_btn);
        ListView list = findViewById(R.id.social_list);
        keys = new ArrayList<>();
        adapter = new SocialAdapter(keys, this);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this::viewDialog);
        String userAgentURL = "https://raw.githubusercontent.com/mayankmetha/SocialFinder/main/user_agent.json";
        String socialPlatformURL = "https://raw.githubusercontent.com/mayankmetha/SocialFinder/main/social_platform.json";

        JsonObjectRequest agentRequest = new JsonObjectRequest(Request.Method.GET, userAgentURL, null,
                response -> {
                    agents = new ArrayList<>();
                    try {
                        JSONArray resArray = response.getJSONArray("agents");
                        for (int i=0; i<resArray.length();i++) {
                            agents.add(resArray.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {});

        JsonObjectRequest socialRequest = new JsonObjectRequest(Request.Method.GET, socialPlatformURL, null,
                response -> {
                    try {
                        socialPlatforms = response.getJSONArray("social_platforms");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {});

        CURL.getInstance(this).addToRequestQueue(agentRequest);
        CURL.getInstance(this).addToRequestQueue(socialRequest);

        button.setOnClickListener(view -> {
            username = String.valueOf(usernameEditText.getText());
            list.removeAllViewsInLayout();
            keys.clear();
            for(int i=0;i< socialPlatforms.length();i++) {
                try {
                    runRequest(socialPlatforms.getJSONObject(i), username);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void openSettings() {
        Button settingBtn = findViewById(R.id.setting_button);
        settingBtn.setFilterTouchesWhenObscured(true);
        settingBtn.setOnClickListener(view -> {
            Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingIntent);
        });
    }

    void runRequest(JSONObject jsonObject, String username) {
        try{
            String url = jsonObject.getString("url");
            String platform = jsonObject.getString("platform");
            int method;
            String processing = jsonObject.getString("processing");
            JSONArray metadata = jsonObject.getJSONArray("metadata");
            StringBuilder finalMetadata = new StringBuilder();
            if(jsonObject.getString("method").equalsIgnoreCase("get"))
                method = Request.Method.GET;
            else
                method = Request.Method.POST;
            CURLRequest stringRequest = new CURLRequest(method, url.replace("{username}",username),
                    response -> {
                        if(metadata.length() == 0)
                            finalMetadata.append("");
                        else {
                            if (processing.equalsIgnoreCase("html")) {
                                try {
                                    Document doc = Jsoup.parse(CURLRequest.parseToString(response),"utf-8");
                                    for(int i=0;i<metadata.length();i++) {
                                        JSONObject obj = metadata.getJSONObject(i);
                                        String key = obj.getString("key");
                                        String prefix = obj.getString("prefix");
                                        String search = obj.getString("search");
                                        String output = obj.getString("output");

                                        Element searchOutput = doc.select(search).first();
                                        if(output.isEmpty()) {
                                            finalMetadata.append(key).append(":").append(prefix).append(searchOutput != null ? searchOutput.text() : "").append("\n");
                                        } else {
                                            finalMetadata.append(key).append(":").append(prefix).append(searchOutput != null ? searchOutput.attr(output) : "").append("\n");
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else if (processing.equalsIgnoreCase("json")) {
                                try {
                                    JSONObject doc = new JSONObject(CURLRequest.parseToString(response));
                                    for(int i=0;i<metadata.length();i++) {
                                        JSONObject obj = metadata.getJSONObject(i);
                                        String key = obj.getString("key");
                                        String prefix = obj.getString("prefix");
                                        String search = obj.getString("search");
                                        JSONObject tmp = doc;
                                        Log.e("JSON Prefix",prefix);
                                        Log.e("JSON",tmp.toString());
                                        if (!prefix.isEmpty() && prefix.split(",").length > 0) {
                                            String[] path = prefix.split(",");
                                            for (String str: path) {
                                                tmp = tmp.getJSONObject(str);
                                                Log.e("JSON",tmp.toString());
                                            }
                                        }
                                        Log.e("JSON",tmp.get(search).toString());
                                        finalMetadata.append(key).append(":").append(tmp.has(search)?tmp.get(search):"").append("\n");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                        String metadataString = finalMetadata.toString().strip();
                        SocialModel model = new SocialModel(url,response.statusCode,platform,metadataString);
                        keys.add(model);
                        adapter.notifyDataSetChanged();
                    }, error -> {
                        finalMetadata.append("");
                        SocialModel model = new SocialModel(url,error.networkResponse.statusCode,platform,finalMetadata.toString());
                        keys.add(model);
                        adapter.notifyDataSetChanged();
                    }) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String>  params = new HashMap<>();
                            params.put("User-Agent", agents.get(sr.nextInt(agents.size()-1)));
                            return params;
                        }
            };
            CURL.getInstance(this).addToRequestQueue(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void viewDialog(AdapterView<?> parent, View view, int position, long id) {
        SocialModel model = Objects.requireNonNull(adapter.getItem(position));
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

        LayoutInflater keyLI = LayoutInflater.from(this);
        final View dialog_view = keyLI.inflate(R.layout.account_details, null);
        builder.setView(dialog_view);
        builder.setCancelable(false);

        MaterialTextView name = dialog_view.findViewById(R.id.platformDetails);
        name.setText(model.getPlatform());

        MaterialTextView details = dialog_view.findViewById(R.id.textDetails);
        NetworkImageView profilePic = dialog_view.findViewById(R.id.imageView);
        String metadata = model.getDetails();
        StringBuilder detailText = new StringBuilder();
        ImageLoader imageLoader = new ImageLoader(CURL.getInstance(this).getRequestQueue(), new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>((int) Runtime.getRuntime().maxMemory() / 8192);
            @Nullable
            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
        for (String str: metadata.split("\n")) {
            if (str.startsWith("Profile Picture")) {
                profilePic.setImageUrl(str.replace("Profile Picture:",""), imageLoader);
            } else {
                detailText.append("\n").append(str);
            }
        }
        details.setText(detailText.toString());

        builder.setNeutralButton(R.string.view_page, (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, BrowserActivity.class);
            intent.putExtra(Constants.activityTitle, model.getPlatform());
            if(model.getUrl().endsWith(".json")) {
                intent.putExtra(Constants.webViewID, model.getUrl().substring(0,model.getUrl().lastIndexOf("}")+1).replace("{username}", username));
            } else {
                intent.putExtra(Constants.webViewID, model.getUrl().replace("{username}", username));
            }
            startActivity(intent);
        });

        builder.setPositiveButton(R.string.close_button, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    void exportList() {
        FloatingActionButton export_btn = findViewById(R.id.exportBtn);
        export_btn.setOnClickListener(v -> {
            try {
                if (username.isEmpty())
                    return;
                JSONObject exportObject = new JSONObject();
                exportObject.put("Username",username);
                for (int i = 0; i < keys.size(); i++) {
                    SocialModel m = keys.get(i);
                    JSONObject tmp = new JSONObject();
                    tmp.put("URL",m.getUrl());
                    tmp.put("HTTP_Response_Code",m.getStatus());
                    for (String str: m.getDetails().split("\n")) {
                        String[] strSplit = str.split(":",2);
                        if (strSplit.length == 2)
                            tmp.put(strSplit[0],strSplit[1]);
                    }
                    exportObject.put(m.getPlatform(),tmp);
                }
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),exportObject.getString("Username")+".json");
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                bufferedWriter.write(exportObject.toString(4));
                bufferedWriter.close();
                Snackbar.make(findViewById(android.R.id.content), file.getName() +" "+getResources().getString(R.string.file_saved), Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void preCheckAppUpdate() {
        getCurrentAppVersion();
        getNewAppVersion();
    }

    private void getCurrentAppVersion() {
        try {
            PackageInfo pInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.PackageInfoFlags.of(PackageManager.GET_ATTRIBUTIONS));
            } else {
                pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            }
            currentVersion = Double.parseDouble(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void getNewAppVersion() {
       Runnable runnable = () -> {
            try {
                URL url = new URL(Constants.GITHUB_RELEASE_LATEST);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.getInputStream();
                String str = conn.getHeaderField( "Location" );
                if (str.isEmpty()) {
                    newVersion = 0;
                } else {
                    newVersion = Double.parseDouble(str.substring(str.lastIndexOf('/') + 1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (newVersion > currentVersion) {
                runOnUiThread(this::updateScreen);
            }
        };
        new Thread(runnable).start();
    }

    private void updateScreen() {
        Button updateBtn = findViewById(R.id.update_button);
        updateBtn.setFilterTouchesWhenObscured(true);
        if(newVersion > currentVersion) {
            Intent updateIntent = new Intent(MainActivity.this, UpdateActivity.class);
            updateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, updateIntent, PendingIntent.FLAG_IMMUTABLE);
            updateNotify = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                    .setContentTitle(String.format("%s%s",getString(R.string.update_new),newVersion))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(notifyPendingIntent)
                    .setGroup(Constants.PACKAGE_NAME)
                    .setGroupSummary(true)
                    .setAutoCancel(false);
            updateNotificationManager.notify(0, updateNotify.build());
            updateBtn.setEnabled(true);
        } else {
            updateBtn.setEnabled(false);
        }


        updateBtn.setOnClickListener(view -> {
            Intent updateIntent = new Intent(MainActivity.this, UpdateActivity.class);
            startActivity(updateIntent);
        });
    }

    private void updateInit() {
        NotificationChannel notificationChannel;
        notificationChannel = new NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(false);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableVibration(false);
        notificationChannel.canBypassDnd();
        notificationChannel.setSound(null,null);
        notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        updateNotificationManager = getSystemService(NotificationManager.class);
        assert updateNotificationManager != null;
        updateNotificationManager.createNotificationChannel(notificationChannel);

        preCheckAppUpdate();
    }

}