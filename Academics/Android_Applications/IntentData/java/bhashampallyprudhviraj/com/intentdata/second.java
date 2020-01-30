package bhashampallyprudhviraj.com.intentdata;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class second extends AppCompatActivity {
    String name;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        textView = findViewById(R.id.textView);
        name = getIntent().getStringExtra("User");
        textView.setText("Hi "+name+"\n Welcome to MAD Lab");
    }
}
