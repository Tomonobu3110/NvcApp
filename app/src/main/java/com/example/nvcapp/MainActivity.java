package com.example.nvcapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<NvcItem> itemList = new ArrayList<>();
    private NvcAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        Button addButton = findViewById(R.id.addButton);

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
            startActivityForResult(intent, 1);
        });
    }

    private void onItemClick(int position) {
        Intent intent = new Intent(MainActivity.this, EditActivity.class);
        intent.putExtra("position", position);
        intent.putExtra("item", itemList.get(position).toJson());
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            NvcItem newItem = NvcItem.fromJson(data.getStringExtra("item"));
            int position = data.getIntExtra("position", -1);

            if (requestCode == 1) { // 新規追加
                itemList.add(newItem);
            } else if (requestCode == 2 && position != -1) { // 編集
                itemList.set(position, newItem);
            }

            adapter.notifyDataSetChanged();
            JsonUtil.saveData(this, itemList);
        }
    }
}
