package bpr.com.hobbies;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private  String[] l = {"Playing Cricket", "Reading Magazines", "Watching TV","Listening Songs","Painting","Eating","Poetry"};
    ListView listView;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView)findViewById(R.id.hobby);
        textView = (TextView)findViewById(R.id.textView);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,l);
        listView.setAdapter(a);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                textView.setText("Your Hobby is "+adapterView.getAdapter().getItem(i));
            }
        });
    }
}
