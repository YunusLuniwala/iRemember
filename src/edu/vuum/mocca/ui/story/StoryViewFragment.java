/*
The iRemember source code (henceforth referred to as "iRemember") is
copyrighted by Mike Walker, Adam Porter, Doug Schmidt, and Jules White
at Vanderbilt University and the University of Maryland, Copyright (c)
2014, all rights reserved.  Since iRemember is open-source, freely
available software, you are free to use, modify, copy, and
distribute--perpetually and irrevocably--the source code and object code
produced from the source, as well as copy and distribute modified
versions of this software. You must, however, include this copyright
statement along with any code built using iRemember that you release. No
copyright statement needs to be provided if you just ship binary
executables of your software products.

You can use iRemember software in commercial and/or binary software
releases and are under no obligation to redistribute any of your source
code that is built using the software. Note, however, that you may not
misappropriate the iRemember code, such as copyrighting it yourself or
claiming authorship of the iRemember software code, in a way that will
prevent the software from being distributed freely using an open-source
development model. You needn't inform anyone that you're using iRemember
software in your software, though we encourage you to let us know so we
can promote your project in our success stories.

iRemember is provided as is with no warranties of any kind, including
the warranties of design, merchantability, and fitness for a particular
purpose, noninfringement, or arising from a course of dealing, usage or
trade practice.  Vanderbilt University and University of Maryland, their
employees, and students shall have no liability with respect to the
infringement of copyrights, trade secrets or any patents by DOC software
or any part thereof.  Moreover, in no event will Vanderbilt University,
University of Maryland, their employees, or students be liable for any
lost revenue or profits or other special, indirect and consequential
damages.

iRemember is provided with no support and without any obligation on the
part of Vanderbilt University and University of Maryland, their
employees, or students to assist in its use, correction, modification,
or enhancement.

The names Vanderbilt University and University of Maryland may not be
used to endorse or promote products or services derived from this source
without express written permission from Vanderbilt University or
University of Maryland. This license grants no permission to call
products or services derived from the iRemember source, nor does it
grant permission for the name Vanderbilt University or
University of Maryland to appear in their names.
 */

package edu.vuum.mocca.ui.story;

import java.io.IOException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import edu.vanderbilt.mooc.R;
import edu.vuum.mocca.orm.MoocResolver;
import edu.vuum.mocca.orm.StoryData;

public class StoryViewFragment extends Fragment {

  private static final String LOG_TAG = StoryViewFragment.class.getCanonicalName();

  private static final String PLAY_AUDIO = "Play Audio";
  private static final String STOP_AUDIO = "Stop Audio";

  private MoocResolver resolver;
  final static String ROW_IDENTIFIER_TAG = "index";

  private OnOpenWindowInterface mOpener;

  private StoryData storyData;

  private TextView titleTV;
  private TextView bodyTV;
  private Button audioButton;
  private VideoView videoLinkView;
  private TextView imageNameTV;
  private ImageView imageMetaDataView;
  private TextView storyTimeTV;
  private TextView latitudeTV;
  private TextView longitudeTV;

  private Button editButton;
  private Button deleteButton;

  private OnClickListener myOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View view) {

      switch (view.getId()) {
      case R.id.button_story_view_to_delete:
        deleteButtonPressed();
        break;
      case R.id.button_story_view_to_edit:
        editButtonPressed();
        break;
      default:
        break;
      }
    }
  };

  public static StoryViewFragment newInstance(long index) {
    StoryViewFragment f = new StoryViewFragment();

    // Supply index input as an argument.
    Bundle args = new Bundle();
    args.putLong(ROW_IDENTIFIER_TAG, index);
    f.setArguments(args);

    return f;
  }

  // this fragment was attached to an activity

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mOpener = (OnOpenWindowInterface) activity;
      resolver = new MoocResolver(activity);
    }
    catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement OnOpenWindowListener");
    }
  }

  // this fragment is being created.

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);

  }

  // this fragment is creating its view before it can be modified
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.story_view_fragment, container, false);
    container.setBackgroundColor(Color.GRAY);
    return view;
  }

  // this fragment is modifying its view before display
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    titleTV = (TextView) getView().findViewById(R.id.story_view_value_title);
    bodyTV = (TextView) getView().findViewById(R.id.story_view_value_body);
    audioButton = (Button) getView().findViewById(R.id.story_view_value_audio_link);
    videoLinkView = (VideoView) getView().findViewById(R.id.story_view_value_video_link);
    imageNameTV = (TextView) getView().findViewById(R.id.story_view_value_image_name);
    imageMetaDataView = (ImageView) getView().findViewById(R.id.story_view_value_image_meta_data);
    storyTimeTV = (TextView) getView().findViewById(R.id.story_view_value_story_time);
    latitudeTV = (TextView) getView().findViewById(R.id.story_view_value_latitude);
    longitudeTV = (TextView) getView().findViewById(R.id.story_view_value_longitude);

    titleTV.setText("");
    bodyTV.setText("");
    imageNameTV.setText("");
    storyTimeTV.setText("0");
    latitudeTV.setText("0");
    longitudeTV.setText("0");

    editButton = (Button) getView().findViewById(R.id.button_story_view_to_edit);
    deleteButton = (Button) getView().findViewById(R.id.button_story_view_to_delete);

    editButton.setOnClickListener(myOnClickListener);
    deleteButton.setOnClickListener(myOnClickListener);

    try {
      setUiToStoryData(getUniqueKey());
    }
    catch (RemoteException e) {
      Toast.makeText(getActivity(), "Error retrieving information from local data store.", Toast.LENGTH_LONG).show();
      Log.e(LOG_TAG, "Error getting Story data from C.P.");
      // e.printStackTrace();
    }
  }

  private void setUiToStoryData(long getUniqueKey) throws RemoteException {
    storyData = resolver.getStoryDataViaRowID(getUniqueKey);
    if (storyData == null) {
      getView().setVisibility(View.GONE);
      return;
    }

    Log.d(LOG_TAG, "In setUiToStoryData method: " + storyData);
    titleTV.setText(storyData.getTitle());
    bodyTV.setText(storyData.getBody());

    final String audioLinkPath = storyData.getAudioLink();

    if (audioLinkPath.isEmpty()) {
      audioButton.setVisibility(View.GONE);
    }
    else {
      audioButton.setOnClickListener(new OnClickListener() {
        private MediaPlayer mediaPlayer = new MediaPlayer();

        @Override
        public void onClick(View v) {
          audioButton.setText(STOP_AUDIO);
          if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            audioButton.setText(PLAY_AUDIO);
            mediaPlayer = new MediaPlayer();
            return;
          }
          mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
              audioButton.setText(PLAY_AUDIO);
              mediaPlayer = new MediaPlayer();
            }

          });
          try {
            mediaPlayer.setDataSource(audioLinkPath);
            mediaPlayer.prepare();
          }
          catch (IOException ioe) {
            Log.i(LOG_TAG, ioe.getMessage());
            Toast.makeText(getActivity(), "Unable to play audio recording. See log for details.", Toast.LENGTH_LONG)
                .show();
            return;
          }
          mediaPlayer.start();
        }
      });
    }

    // Set up video playback using the MediaController android widget
    // and the video view already set up in the layout file.

    String videoLinkPath = storyData.getVideoLink();
    if (videoLinkPath.isEmpty()) {
      videoLinkView.setVisibility(View.GONE);
    }
    else {
      MediaController controller = new MediaController(getActivity());
      controller.setAnchorView(videoLinkView);
      videoLinkView.setMediaController(controller);
      videoLinkView.setVideoURI(Uri.parse(videoLinkPath));
      videoLinkView.start();
    }

    // Display the image data

    imageNameTV.setText(storyData.getImageName());

    String imageMetaDataPath = storyData.getImageLink();
    imageMetaDataView.setImageURI(Uri.parse(imageMetaDataPath));

    Long time = Long.valueOf(storyData.getStoryTime());
    storyTimeTV.setText(Utils.formatDateTime(time));

    latitudeTV.setText(String.format("%.1f", storyData.getLatitude()));
    longitudeTV.setText(String.format("%.1f", storyData.getLongitude()));
  }

  // action to be performed when the edit button is pressed
  private void editButtonPressed() {
    mOpener.openEditStoryFragment(storyData.getKeyId());
  }

  // action to be performed when the delete button is pressed
  private void deleteButtonPressed() {
    String message;

    message = getResources().getString(R.string.story_view_deletion_dialog_message);

    new AlertDialog.Builder(getActivity()).setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.story_view_deletion_dialog_title).setMessage(message)
        .setPositiveButton(R.string.story_view_deletion_dialog_yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            try {
              resolver.deleteAllStoryWithRowID(storyData.getKeyId());
            }
            catch (RemoteException e) {
              Log.e(LOG_TAG, "RemoteException Caught => " + e.getMessage());
              e.printStackTrace();
            }
            mOpener.openListStoryFragment();
            if (getResources().getBoolean(R.bool.isTablet) == true) {
              mOpener.openViewStoryFragment(-1);
            }
            else {
              getActivity().finish();
            }
          }

        }).setNegativeButton(R.string.story_view_deletion_dialog_no, null).show();
  }

  long getUniqueKey() {
    return getArguments().getLong(ROW_IDENTIFIER_TAG, 0);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mOpener = null;
    resolver = null;
  }

  @Override
  public void onResume() {
    super.onResume();
    try {
      setUiToStoryData(getUniqueKey());
    }
    catch (RemoteException e) {
      Toast.makeText(getActivity(), "Error retrieving information from local data store.", Toast.LENGTH_LONG).show();
      Log.e(LOG_TAG, "Error getting Story data from C.P.");
    }
  }
}