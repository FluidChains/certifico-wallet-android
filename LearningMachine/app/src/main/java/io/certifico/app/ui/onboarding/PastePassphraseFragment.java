package io.certifico.app.ui.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.certifico.app.R;
import io.certifico.app.data.CertificateManager;
import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.databinding.FragmentPastePassphraseBinding;
import io.certifico.app.ui.LMActivity;
import io.certifico.app.ui.home.HomeActivity;
import io.certifico.app.util.DialogUtils;
import io.certifico.app.util.StringUtils;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

public class PastePassphraseFragment extends OnboardingFragment {

    @Inject
    protected BitcoinManager mBitcoinManager;

    @Inject
    protected CertificateManager mCertificateManager;

    private FragmentPastePassphraseBinding mBinding;

    private boolean isGoogleFlow;

    public static PastePassphraseFragment newInstance(boolean isGoogleFlow) {
        PastePassphraseFragment instance = new PastePassphraseFragment();
        Bundle args = new Bundle();
        args.putBoolean("isGoogleFlow", isGoogleFlow);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(getContext())
                .inject(this);
        Bundle args = getArguments();
        isGoogleFlow = args.getBoolean("isGoogleFlow");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_paste_passphrase, container, false);

        if (isGoogleFlow) {
            ((LMActivity) getActivity()).askRestoreFromGoogleDrive((loading) -> {
                if (loading)
                    displayProgressDialog(R.string.onboarding_passphrase_load_gdrive_progress);
                else hideProgressDialog();
            }, (passphrase) -> {
                Timber.i("Sync.BackupPassphraseFragment PastePassphraseFragment() -> " + passphrase);
                if (passphrase != null) {
                    displayProgressDialog(R.string.onboarding_passphrase_load_gdrive_progress);
                    (getActivity()).runOnUiThread(() -> {
                        mBinding.pastePassphraseEditText.setText(passphrase.toString());
                        onDone();
                    });
                } else {
                    Timber.i("[Drive] backup not found");
                    backupNotFound(getResources().getString(R.string.error_passphrase_backup_not_found_gdrive));
                }
                return Observable.just(null);
            }, (driveFile) -> {
                if (driveFile == null) {
                    return Observable.just(null);
                }
                displayProgressDialog(R.string.onboarding_passphrase_load_gdrive_progress);
                return mCertificateManager.addCertificate(driveFile.stream);
            });
        } else {
            ((LMActivity) getActivity()).askToGetPassphraseFromDevice((passphrase) -> {
                if (passphrase != null) {
                    mBinding.pastePassphraseEditText.setText(passphrase.toString());
                    onDone();
                } else {
                    backupNotFound(getResources().getString(R.string.error_passphrase_backup_not_found_device));
                }
                return null;
            });
        }


        mBinding.pastePassphraseEditText.setFilters(new InputFilter[]{
                new InputFilter.AllCaps() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        String toLowered = String.valueOf(source).toLowerCase();
                        String sanitized = toLowered.replaceAll("[^a-zA-Z ]", "");
                        return sanitized;
                    }
                }
        });

        mBinding.pastePassphraseEditText.addTextChangedListener(new PastePassphraseTextWatcher());
        mBinding.doneButton.setEnabled(false);
        mBinding.doneButton.setOnClickListener(view -> onDone());

        return mBinding.getRoot();
    }

    private void backupNotFound(String message) {
        DialogUtils.showAlertDialog(getContext(), this,
                R.drawable.ic_dialog_failure,
                getResources().getString(R.string.error_passphrase_backup_not_found_title),
                message,
                null,
                getResources().getString(R.string.ok_button),
                (btnIdx) -> {
                    return null;
                });
        mBinding.passphraseLabel.requestFocus();
    }

    private void onDone() {
//        displayProgressDialog(R.string.onboarding_passphrase_loading);
        String passphrase = mBinding.pastePassphraseEditText.getText().toString();
        Activity activity = getActivity();

        mBinding.doneButton.setEnabled(false);
        mBinding.pastePassphraseEditText.setEnabled(false);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mBitcoinManager.setPassphrase(passphrase)
                        .compose(bindToMainThread())
                        .subscribe(wallet -> {

                            if (isVisible()) {

                                Log.d("LM", "PastePassphraseFragment isVisible()");

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (isVisible()) {
                                            // if we return to the app by pasting in our passphrase, we
                                            // must have already backed it up!
                                            mSharedPreferencesManager.setHasSeenBackupPassphraseBefore(true);
                                            mSharedPreferencesManager.setWasReturnUser(true);
                                            mSharedPreferencesManager.setFirstLaunch(false);
                                            if (continueDelayedURLsFromDeepLinking() == false) {
                                                startActivity(new Intent(getActivity(), HomeActivity.class));
                                                getActivity().finish();
                                            }
                                        }
                                    }
                                });
                            }
                            hideProgressDialog();
                        }, e -> {
                            Timber.e(e, "Could not set passphrase.");
                            hideProgressDialog();
                            displayErrorsLocal(e, DialogUtils.ErrorCategory.GENERIC, R.string.error_title_message);
                        });
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    protected void displayErrorsLocal(Throwable throwable, DialogUtils.ErrorCategory errorCategory, @StringRes int errorTitleResId) {
        mBinding.pastePassphraseEditText.setEnabled(true);
        mBinding.pastePassphraseEditText.setText("");

        DialogUtils.showAlertDialog(getContext(), this,
                R.drawable.ic_dialog_failure,
                getResources().getString(R.string.onboarding_passphrase_invalid_title),
                getResources().getString(R.string.onboarding_passphrase_invalid_desc),
                null,
                getResources().getString(R.string.ok_button),
                (btnIdx) -> {
                    return null;
                });
    }


    private class PastePassphraseTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String passphrase = mBinding.pastePassphraseEditText.getText()
                    .toString();
            boolean emptyPassphrase = StringUtils.isEmpty(passphrase);
            mBinding.doneButton.setEnabled(!emptyPassphrase);
        }
    }
}
