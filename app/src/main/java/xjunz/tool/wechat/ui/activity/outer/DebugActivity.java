package xjunz.tool.wechat.ui.activity.outer;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.util.Objects;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.impl.Environment;
import xjunz.tool.wechat.ui.activity.BaseActivity;
import xjunz.tool.wechat.util.UiUtils;
import xjunz.tool.wechat.util.UniUtils;

public class DebugActivity extends BaseActivity {
    @Keep
    public static final String EXTRA_ENV_SERIAL = "xjunz.extra.EnvSerial";
    private EditText mEtInput, mEtOutput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        String serial = getIntent().getStringExtra(EXTRA_ENV_SERIAL);
        mEtInput = findViewById(R.id.et_serial);
        if (serial != null) {
            mEtInput.setText(serial);
        }
        mEtOutput = findViewById(R.id.et_output);
    }


    public void parseEnvInfo(View view) {
        try {
            String input = mEtInput.getText().toString();
            if (input.contains("Serial")) {
                input = Objects.requireNonNull(UniUtils.extractFirst(input, "Serial:(.+)$"), "error extract serial").trim();
                UiUtils.toast(String.valueOf(input.length()));
            }
            mEtOutput.setText(Html.fromHtml(Objects.requireNonNull(Environment.deserialize(input), "error parse environment")));
        } catch (Exception e) {
            mEtOutput.setText(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
        }
    }
}
