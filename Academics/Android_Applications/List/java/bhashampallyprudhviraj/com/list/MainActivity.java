package bhashampallyprudhviraj.com.list;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    String name[] = {"prudhvi","raj","bhashampally"};
    String phones[] = {"9999999999","8888888888","7777777777"};
    int imgs[] = {R.drawable.engineer,R.drawable.download1,R.drawable.images};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.list);
        MyAdapter adapter = new MyAdapter(this,names,phones,imgs);
        listView.setAdapter(adapter);
    }
}
