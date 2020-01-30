package bhashampallyprudhviraj.com.imagebuttons;

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

    public void image1(View view) {
        imageView.setImageResource(R.drawable.images);
    }

    public void image2(View view) {
        imageView.setImageResource(R.drawable.images1);
    }

    public void image3(View view) {
        imageView.setImageResource(R.drawable.download1);
    }

    public void image4(View view) {
        imageView.setImageResource(R.drawable.download);
    }

    public void image5(View view) {
        imageView.setImageResource(R.drawable.download2);
    }
}
