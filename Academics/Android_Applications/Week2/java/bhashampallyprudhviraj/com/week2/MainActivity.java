package bhashampallyprudhviraj.com.week2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    EditText editText;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
        textView = (TextView)findViewById(R.id.textView);
    }

    public void read(View view) {
        String a = editText.getText().toString();
        textView.setVisibility(view.VISIBLE);
        editText.setVisibility(view.INVISIBLE);
        textView.setText("Hello \nMr/Mrs. : "+a);
    }
}
