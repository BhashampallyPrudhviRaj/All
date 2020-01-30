package bhashampallyprudhviraj.com.imagenext;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    Button next;
    Button previous;
    int i=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        next = findViewById(R.id.button);
        previous = findViewById(R.id.button2);
        imageView = findViewById(R.id.imageView);
    }

    int[] image = new int[]{R.drawable.engineer,R.drawable.engineer1,R.drawable.engineer2,R.drawable.engineer3,R.drawable.engineer4,R.drawable.engineer5};

    public void previous(View view) {
        if(i==0){
            i = image.length-1;
            imageView.setImageResource(image[i]);
            Toast.makeText(this,"This is the 1st image",Toast.LENGTH_LONG).show();
        }
        else{
            imageView.setImageResource(image[i]);
            i--;
        }
    }

    public void next(View view) {
        if(i>=image.length) {
            i = 0;
            imageView.setImageResource(image[1]);
            i++;
            Toast.makeText(this,"This is the last image",Toast.LENGTH_LONG).show();
        }
        else{
            imageView.setImageResource(image[i]);
            i++;
        }
    }
}
