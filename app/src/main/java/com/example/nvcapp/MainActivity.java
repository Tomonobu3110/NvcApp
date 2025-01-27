package com.example.nvcapp;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_CODE_NEW_ITEM = 1;
    public final static int REQUEST_CODE_EDIT_ITEM = 2;
    public final static int REQUEST_CODE_SAVE_JSON = 1001; // For Google Drive (Save)
    public final static int REQUEST_CODE_LOAD_JSON = 1002; // For Google Drive (Load)
    private List<NvcItem> itemList = new ArrayList<>();
    private NvcAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        Button addButton = findViewById(R.id.addButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button loadButton = findViewById(R.id.loadButton);

        // RecyclerView設定
        adapter = new NvcAdapter(itemList, this::onItemClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // データ読み込み
        itemList.addAll(JsonUtil.loadData(this));
        adapter.notifyDataSetChanged();

        // 「+できごとの追加」ボタン
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditActivity.class);
            startActivityForResult(intent, REQUEST_CODE_NEW_ITEM);
        });

        // save and load
        saveButton.setOnClickListener(v -> signIn(REQUEST_CODE_SAVE_JSON));
        loadButton.setOnClickListener(v -> signIn(REQUEST_CODE_LOAD_JSON));
    }

    private void onItemClick(int position) {
        Intent intent = new Intent(MainActivity.this, EditActivity.class);
        intent.putExtra("position", position);
        intent.putExtra("item", itemList.get(position).toJson());
        startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            NvcItem newItem = NvcItem.fromJson(data.getStringExtra("item"));
            int position = data.getIntExtra("position", -1);

            if (requestCode == REQUEST_CODE_NEW_ITEM) { // 新規追加
                itemList.add(newItem);
            } else if (requestCode == REQUEST_CODE_EDIT_ITEM && position != -1) { // 編集
                itemList.set(position, newItem);
            }

            adapter.notifyDataSetChanged();
            JsonUtil.saveData(this, itemList);
        }

        // for Google Drive (Save & Load)
        else if (requestCode == REQUEST_CODE_SAVE_JSON || requestCode == REQUEST_CODE_LOAD_JSON)  {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.i("MainActivity", "login : " + account.getDisplayName());
                    Log.i("MainActivity", "email : " + account.getEmail());
                    switch (requestCode) {
                        case REQUEST_CODE_SAVE_JSON:
                            saveJsonToGoogleDrive(account);
                            break;
                        case REQUEST_CODE_LOAD_JSON:
                            loadLatestJsonFromGoogleDrive(account);
                            break;
                    }
                } else {
                    Toast.makeText(this, "error : account is null", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "error : Google Sign In", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", e.toString());
                Log.e("MainActivity", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void signIn(int requestCode) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        startActivityForResult(googleSignInClient.getSignInIntent(), requestCode);
    }

    public void saveJsonToGoogleDrive(GoogleSignInAccount account) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccount(account.getAccount());
                com.google.api.services.drive.Drive driveService =
                        new com.google.api.services.drive.Drive.Builder(
                                new NetHttpTransport(),
                                new JacksonFactory(),
                                credential)
                                .setApplicationName("My Goal Application")
                                .build();

                Gson gson = new Gson();
                String json = gson.toJson(itemList);

                try {
                    // フォルダが存在するかチェック
                    String folderId = null;
                    FileList result = driveService.files().list()
                            .setQ("name = 'MyGoalApplicationData' and mimeType = 'application/vnd.google-apps.folder'")
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute();

                    if (!result.getFiles().isEmpty()) {
                        folderId = result.getFiles().get(0).getId();
                    } else {
                        // フォルダを作成
                        File folderMetadata = new File();
                        folderMetadata.setName("MyGoalApplicationData");
                        folderMetadata.setMimeType("application/vnd.google-apps.folder");

                        File folder = driveService.files().create(folderMetadata)
                                .setFields("id")
                                .execute();
                        folderId = folder.getId();
                    }

                    // 日時付きファイル名の生成
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String timestamp = sdf.format(new Date());
                    String fileName = "my_goal_application_data_" + timestamp + ".json";

                    // ファイルをフォルダ内に作成
                    File fileMetadata = new File();
                    fileMetadata.setName(fileName);
                    fileMetadata.setParents(Collections.singletonList(folderId));

                    ByteArrayContent content = new ByteArrayContent("application/json", json.getBytes());

                    driveService.files().create(fileMetadata, content)
                            .setFields("id")
                            .execute();

                    // メインスレッドでUIの更新
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), "JSON saved to Google Drive", Toast.LENGTH_SHORT).show()
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    // メインスレッドでエラー表示
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), "Error: JSON save", Toast.LENGTH_SHORT).show()
                    ); // end of runOnUiThread()
                }
            } // end of run()
        }); // end of executor.execute()
    } // end of function of saveJsonToGoogleDrive()

    public void loadLatestJsonFromGoogleDrive(GoogleSignInAccount account) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccount(account.getAccount());
                com.google.api.services.drive.Drive driveService =
                        new com.google.api.services.drive.Drive.Builder(
                                new NetHttpTransport(),
                                new JacksonFactory(),
                                credential)
                                .setApplicationName("NVC Application")
                                .build();

                try {
                    // フォルダIDの取得
                    String folderId = null;
                    FileList folderResult = driveService.files().list()
                            .setQ("name = 'NvcAppData' and mimeType = 'application/vnd.google-apps.folder'")
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute();

                    if (!folderResult.getFiles().isEmpty()) {
                        folderId = folderResult.getFiles().get(0).getId();
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(getApplicationContext(), "Folder not found", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // フォルダ内のファイル一覧取得 (my_goal_application_data_*.json)
                    FileList fileResult = driveService.files().list()
                            .setQ("'" + folderId + "' in parents and name contains 'my_goal_application_data_' and name contains '.json'")
                            .setSpaces("drive")
                            .setFields("files(id, name, createdTime)")
                            .setOrderBy("createdTime desc") // 最新順にソート
                            .execute();

                    if (fileResult.getFiles().isEmpty()) {
                        runOnUiThread(() ->
                                Toast.makeText(getApplicationContext(), "No JSON file found", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // 最新ファイルの取得
                    File latestFile = fileResult.getFiles().get(0);
                    String fileId = latestFile.getId();

                    // ファイルのコンテンツ取得
                    InputStream inputStream = driveService.files().get(fileId).executeMediaAsInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();

                    String json = jsonBuilder.toString();

                    // JSONパースとデータの読み込み
                    Gson gson = new Gson();
                    List<NvcItem> loadedNvcList = gson.fromJson(json, new TypeToken<List<NvcItem>>() {}.getType());

                    // メインスレッドでUIを更新
                    runOnUiThread(() -> {
                        itemList.clear();
                        itemList.addAll(loadedNvcList);
                        adapter.notifyDataSetChanged();
                        JsonUtil.saveData(getApplicationContext(), itemList);
                        Toast.makeText(getApplicationContext(), "Latest JSON loaded", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), "Error loading JSON", Toast.LENGTH_SHORT).show()
                    ); // end of runOnUiThread()
                }
            } // end of run()
        }); // end of executor.execute()
    } // end of function of loadLatestJsonFromGoogleDrive()
}
