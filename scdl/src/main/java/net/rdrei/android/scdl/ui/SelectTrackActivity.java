package net.rdrei.android.scdl.ui;

import java.io.File;
import java.net.URL;

import net.rdrei.android.scdl.R;
import net.rdrei.android.scdl.ShareIntentResolver;
import net.rdrei.android.scdl.ShareIntentResolver.UnsupportedUrlException;
import net.rdrei.android.scdl.api.ServiceManager;
import net.rdrei.android.scdl.api.entity.TrackEntity;
import net.rdrei.android.scdl.api.service.DownloadService;
import net.rdrei.android.scdl.api.service.TrackService;
import net.rdrei.android.scdl.ui.TrackErrorActivity.ErrorCode;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import roboguice.util.Ln;
import roboguice.util.RoboAsyncTask;
import roboguice.util.SafeAsyncTask;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;

public class SelectTrackActivity extends RoboActivity {

	@InjectView(R.id.track_title)
	private TextView mTitleView;

	@InjectView(R.id.track_description)
	private TextView mDescriptionView;

	@InjectView(R.id.detail_container)
	private View mDetailContainerView;

	@InjectView(R.id.track_unavailable)
	private View mTrackUnavailableView;

	@InjectView(R.id.progress_bar)
	private View mProgressBarView;

	@InjectView(R.id.btn_download)
	private Button mDownloadButton;

	@InjectView(R.id.btn_cancel)
	private Button mCancelButton;

	@InjectView(R.id.img_artwork)
	private ImageView mArtworkImageView;

	private TrackEntity mTrack;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.select_track);

		final TrackResolverTask task = new TrackResolverTask(this);
		task.execute();

		bindButtons();
	}

	private void bindButtons() {
		mDownloadButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				DownloadTask task = new DownloadTask(SelectTrackActivity.this,
						String.valueOf(mTrack.getId()));
				task.execute();
				mDownloadButton.setEnabled(false);
			}

		});

		mCancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Ln.d("Canceling download. Bye, bye!");
				finish();
			}
		});
	}

	/**
	 * Show error activity with the given error code and exit the current
	 * activity.
	 * 
	 * @param errorCode
	 */
	protected void startErrorActivity(TrackErrorActivity.ErrorCode errorCode) {
		Intent intent = new Intent(this, TrackErrorActivity.class);
		intent.putExtra(TrackErrorActivity.EXTRA_ERROR_CODE, errorCode);
		startActivity(intent);
		finish();
	}

	/**
	 * Creates a new download manager request based on the given uri.
	 * 
	 * @param uri
	 * @return
	 */
	private DownloadManager.Request createDownloadRequest(final Uri uri) {
		final Request request = new Request(uri);
		// Path based on the public Music directory and a - currently -
		// hard-coded value.
		final String typePath = new File(Environment.DIRECTORY_MUSIC,
				"Soundcloud").toString();

		request.setTitle(mTrack.getTitle());
		request.setDescription(getString(R.string.download_description));
		request.setDestinationInExternalPublicDir(typePath,
				mTrack.getDownloadFilename());

		// We have an audio file, please scan it!
		request.allowScanningByMediaScanner();

		return request;
	}

	protected void downloadTrack(final Uri uri) throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
				Ln.d("Starting download of %s.", uri.toString());
				Request request = createDownloadRequest(uri);

				downloadManager.enqueue(request);
			}
		}).start();
		Toast.makeText(SelectTrackActivity.this, "Download started.",
				Toast.LENGTH_SHORT).show();
	}

	protected void updateTrackDisplay() {
		if (mTrack == null) {
			return;
		}

		mTitleView.setText(mTrack.getTitle());
		mDescriptionView.setText(mTrack.getDescription());
		mProgressBarView.setVisibility(View.GONE);
		mDetailContainerView.setVisibility(View.VISIBLE);
		if (!mTrack.isDownloadable()) {
			mTrackUnavailableView.setVisibility(View.VISIBLE);

		}

		ArtworkLoaderTask artworkLoaderTask = new ArtworkLoaderTask(
				mTrack.getArtworkUrl());
		artworkLoaderTask.execute();
		mDownloadButton.setEnabled(mTrack.isDownloadable());
	}

	/**
	 * Resolves a track to its id.
	 * 
	 * TODO: Errors in here must be tracked and should end the current activity
	 * (either error activity or just popup dialog).
	 * 
	 * @author pascal
	 * 
	 */
	public class TrackResolverTask extends RoboAsyncTask<String> {

		protected TrackResolverTask(Context context) {
			super(context);
		}

		@Inject
		private ShareIntentResolver mShareIntentResolver;

		@Override
		public String call() throws Exception {
			return mShareIntentResolver.resolveId();
		}

		@Override
		protected void onException(Exception e) throws RuntimeException {
			super.onException(e);

			if (e instanceof UnsupportedUrlException) {
				startErrorActivity(ErrorCode.UNSUPPORTED_URL);
			} else {
				startErrorActivity(ErrorCode.UNKNOWN_ERROR);
			}
		}

		@Override
		protected void onSuccess(String id) throws Exception {
			super.onSuccess(id);

			Ln.d("Resolved track to id %s. Starting further API calls.", id);
			final TrackLoaderTask trackLoaderTask = new TrackLoaderTask(
					context, id);
			trackLoaderTask.execute();
		}
	}

	public class TrackLoaderTask extends RoboAsyncTask<TrackEntity> {
		@Inject
		private ServiceManager mServiceManager;

		private String mId;

		protected TrackLoaderTask(Context context, String id) {
			super(context);
			mId = id;
		}

		@Override
		protected void onException(Exception e) throws RuntimeException {
			super.onException(e);
			Ln.e("Error during resolving track: %s", e.toString());

			Toast.makeText(getContext(), "ERROR: " + e.toString(),
					Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onSuccess(TrackEntity t) throws Exception {
			super.onSuccess(t);
			mTrack = t;
			updateTrackDisplay();
		}

		@Override
		public TrackEntity call() throws Exception {
			TrackService trackService = mServiceManager.trackService();
			return trackService.getTrack(mId);
		}
	}

	private class ArtworkLoaderTask extends SafeAsyncTask<Drawable> {

		private String mUrlStr;

		public ArtworkLoaderTask(String url) {
			super();

			mUrlStr = url;
		}

		@Override
		public Drawable call() throws Exception {
			final URL artworkURL = new URL(mUrlStr);
			return Drawable.createFromStream(artworkURL.openStream(), null);
		}

		@Override
		protected void onSuccess(Drawable t) throws Exception {
			super.onSuccess(t);

			mArtworkImageView.setImageDrawable(t);
		}
	}

	private class DownloadTask extends RoboAsyncTask<Uri> {
		@Inject
		private ServiceManager mServiceManager;

		private String mId;

		protected DownloadTask(Context context, String id) {
			super(context);
			mId = id;
		}

		@Override
		public Uri call() throws Exception {
			final DownloadService service = mServiceManager.downloadService();
			return service.resolveUri(mId);
		}

		@Override
		protected void onSuccess(Uri t) throws Exception {
			super.onSuccess(t);

			Ln.d("Resolved download URL: %s", t);
			downloadTrack(t);
		}
	}
}
