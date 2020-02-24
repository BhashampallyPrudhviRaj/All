package bhashampallyprudhviraj.com.greenhousemonitoring;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    DatabaseReference mref;
    TextView t1,t2,t3,t4,t5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Green House Monitoring");
        t1 = findViewById(R.id.textView);
        t2 = findViewById(R.id.textView4);
        t3 = findViewById(R.id.textView6);
        t4 = findViewById(R.id.textView7);
        t5 = findViewById(R.id.textView9);

        mref = FirebaseDatabase.getInstance().getReference();
        mref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                t1.setText(dataSnapshot.child("Temperature").getValue().toString());
                t2.setText(dataSnapshot.child("Humidity").getValue().toString());
                t3.setText(dataSnapshot.child("Soil Moisture").getValue().toString());
                t4.setText(dataSnapshot.child("Soil Condition").getValue().toString());
                t5.setText(dataSnapshot.child("Light").getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
