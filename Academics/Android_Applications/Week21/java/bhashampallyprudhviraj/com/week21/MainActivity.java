package bhashampallyprudhviraj.com.week21;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    EditText editText;
    int a;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
    }

    public void increase(View view) {
        a = Integer.parseInt(editText.getText().toString());
        if(a>50)
            Toast.makeText(this,"\tSorry\nLimited to 50",Toast.LENGTH_SHORT).show();
        else
            editText.setText(" "+(a+1));
    }

    public void decrease(View view) {
        a = Integer.parseInt(editText.getText().toString());
        if(a<0)
            Toast.makeText(this,"\tSorry\nLimited to 0",Toast.LENGTH_SHORT).show();
        else
            editText.setText(" "+(a-1));
    }
}
