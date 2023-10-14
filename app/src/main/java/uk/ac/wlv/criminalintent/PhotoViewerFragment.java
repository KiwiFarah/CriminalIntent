package uk.ac.wlv.criminalintent;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class PhotoViewerFragment extends DialogFragment {
    private static final String ARG_PATH = "image_path";

    public static PhotoViewerFragment newInstance(String imagePath) {
        Bundle args = new Bundle();
        args.putString(ARG_PATH, imagePath);
        PhotoViewerFragment fragment = new PhotoViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String imagePath = getArguments().getString(ARG_PATH);
        Bitmap image = PictureUtils.getScaledBitmap(imagePath, getActivity());

        View v = new ImageView(getActivity());
        ((ImageView) v).setImageBitmap(image);

        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .create();
    }
}
