package bhashampallyprudhviraj.com.intentresult;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class second extends AppCompatActivity {

    EditText editText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        editText = findViewById(R.id.editText);
    }

    public void submit(View view) {
        String a = editText.getText().toString();
        if(a.equals(" "))
            editText.setError("Please Enter Your Name");
        else{
            Intent intent = new Intent();
            intent.putExtra("result",a);
            setResult(RESULT_OK,intent);
            finish();
        }
    }
}
