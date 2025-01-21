package com.example.nvcapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class EditActivity extends AppCompatActivity {

    private EditText eventInput, observationInput, feelingsInput, needsInput, requestInput;
    private int position = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        eventInput = findViewById(R.id.eventInput);
        observationInput = findViewById(R.id.observationInput);
        feelingsInput = findViewById(R.id.feelingsInput);
        needsInput = findViewById(R.id.needsInput);
        requestInput = findViewById(R.id.requestInput);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button saveButton = findViewById(R.id.saveButton);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("item")) {
            NvcItem item = NvcItem.fromJson(intent.getStringExtra("item"));
            position = intent.getIntExtra("position", -1);
            if (item != null) {
                eventInput.setText(item.getEvent());
                observationInput.setText(item.getObservation());
                feelingsInput.setText(item.getFeelings());
                needsInput.setText(item.getNeeds());
                requestInput.setText(item.getRequest());
            }
        }

        // キャンセルボタン
        cancelButton.setOnClickListener(v -> finish());

        // 保存ボタン
        saveButton.setOnClickListener(v -> {
            String event = eventInput.getText().toString();
            String observation = observationInput.getText().toString();
            String feelings = feelingsInput.getText().toString();
            String needs = needsInput.getText().toString();
            String request = requestInput.getText().toString();

            NvcItem newItem = new NvcItem(event, observation, feelings, needs, request);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("item", newItem.toJson());
            resultIntent.putExtra("position", position);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
