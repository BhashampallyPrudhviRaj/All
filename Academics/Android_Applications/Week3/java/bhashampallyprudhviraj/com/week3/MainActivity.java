package bhashampallyprudhviraj.com.week3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
    }

    public void resetimg(View view) {
        imageView.setImageResource(R.drawable.halfsuit);
    }

    public void changeimg(View view) {
        imageView.setImageResource(R.drawable.half);
    }
}
