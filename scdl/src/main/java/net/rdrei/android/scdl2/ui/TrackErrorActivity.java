package net.rdrei.android.scdl2.ui;

import net.rdrei.android.scdl2.R;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import android.os.Bundle;
import android.widget.TextView;

public class TrackErrorActivity extends RoboActivity {

	public static enum ErrorCode {
		UNSUPPORTED_URL, NO_WRITE_PERMISSION, UNKNOWN_ERROR, NO_MARKET, NOT_FOUND, NETWORK_ERROR
	}

	public static final String EXTRA_ERROR_CODE = "error_code";

	@InjectView(R.id.error_message)
	TextView mErrorTextView;

	@InjectExtra(EXTRA_ERROR_CODE)
	private ErrorCode mErrorCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.track_error);

		setErrorText();
	}

	private void setErrorText() {
		final int errorMessage;

		switch (mErrorCode) {
		case UNSUPPORTED_URL:
			errorMessage = R.string.track_error_unsupported_url;
			break;
		case NO_WRITE_PERMISSION:
			errorMessage = R.string.track_error_no_write_permission;
			break;
		case NO_MARKET:
			errorMessage = R.string.track_error_no_market;
			break;
		case NOT_FOUND:
			errorMessage = R.string.track_error_not_found;
			break;
		case NETWORK_ERROR:
			errorMessage = R.string.track_error_network;
			break;
		default:
			errorMessage = R.string.track_error_unknown;
		}

		mErrorTextView.setText(errorMessage);
	}
}