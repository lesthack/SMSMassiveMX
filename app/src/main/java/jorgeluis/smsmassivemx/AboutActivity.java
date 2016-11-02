package jorgeluis.smsmassivemx;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    private TextView txt_company_web;
    private TextView txt_colaboration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        txt_company_web = (TextView) findViewById(R.id.txt_company_web);
        txt_company_web.setMovementMethod(LinkMovementMethod.getInstance());

        txt_colaboration = (TextView) findViewById(R.id.txt_colaboration);
        txt_colaboration.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
