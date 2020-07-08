package xjunz.tool.wechat.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import xjunz.tool.wechat.R;
import xjunz.tool.wechat.databinding.ActivityDetailBinding;
import xjunz.tool.wechat.impl.model.account.Contact;
import xjunz.tool.wechat.impl.model.account.Talker;
import xjunz.tool.wechat.ui.BaseActivity;

public class DetailActivity extends BaseActivity {
    public static final String EXTRA_DATA = "DetailActivity.extra.data";
    private ActivityDetailBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
        if (intent != null) {
            Contact contact = (Contact) intent.getSerializableExtra(EXTRA_DATA);
            if (contact != null) {
                mBinding.setContact(contact);
                if (contact instanceof Talker) {
                    mBinding.setTalker((Talker) contact);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            Contact contact = intent.getParcelableExtra(EXTRA_DATA);
            if (contact != null) {
                mBinding.setContact(contact);
                if (contact instanceof Talker) {
                    mBinding.setTalker((Talker) contact);
                }
            }
        }
    }
}
