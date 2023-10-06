package uk.ac.wlv.criminalintent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHONE_CONTACT = 2;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private Crime mCrime;
    private EditText mTitleField;
    Button mDateButton;
    CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mCallButton;
    private String mChosenPhoneNumber;

    public static CrimeFragment newInstance(UUID crimeID) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeID);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);
        mTitleField = v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mCrime.setTitle(s.toString());
            }
        });

        mDateButton = v.findViewById(R.id.crime_date);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        String formattedDate = sdf.format(mCrime.getDate());
        mDateButton.setText(formattedDate);
        mDateButton.setOnClickListener(v1 -> {
            FragmentManager manager = getFragmentManager();
            DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
            dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
            dialog.show(manager, DIALOG_DATE);
        });

        mSolvedCheckBox = v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> mCrime.setSolved(isChecked));

        mReportButton = v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(v12 -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
            i = Intent.createChooser(i, getString(R.string.send_report));
            startActivity(i);
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(v13 -> startActivityForResult(pickContact, REQUEST_CONTACT));
        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        mCallButton = v.findViewById(R.id.crime_call);
        mCallButton.setOnClickListener(v14 -> {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            } else {
                pickContactForPhone();
            }
        });

        Button mDeleteButton = v.findViewById(R.id.crime_delete);
        mDeleteButton.setBackgroundColor(Color.RED);
        mDeleteButton.setOnClickListener(v15 -> {
            CrimeLab.get(getActivity()).deleteCrime(mCrime);
            getActivity().finish();
        });

        return v;
    }

    private void pickContactForPhone() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(pickContactIntent, REQUEST_PHONE_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_PHONE_CONTACT) {
            handleContactPhoneResult(data);
        } else if (requestCode == REQUEST_CONTACT) {
            handleContactNameResult(data);
        }
    }

    private void handleContactNameResult(Intent data) {
        Uri contactUri = data.getData();
        String[] queryFields = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
        Cursor c = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);
        if (c != null && c.moveToFirst()) {
            int nameColumnIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            if (nameColumnIndex != -1) {
                String suspect = c.getString(nameColumnIndex);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            }
            c.close();
        }
    }


    private void handleContactPhoneResult(Intent data) {
        Uri contactUri = data.getData();
        String[] queryFields = new String[]{ContactsContract.Contacts._ID};
        Cursor c = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);
        if (c != null && c.moveToFirst()) {
            int idColumnIndex = c.getColumnIndex(ContactsContract.Contacts._ID);
            if (idColumnIndex != -1) {
                String id = c.getString(idColumnIndex);
                Cursor phoneCursor = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    int numberColumnIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (numberColumnIndex != -1) {
                        String phoneNumber = phoneCursor.getString(numberColumnIndex);
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                        startActivity(intent);
                    }
                    phoneCursor.close();
                }
            }
            c.close();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickContactForPhone();
        }
    }

    private String getCrimeReport() {
        String solvedString = mCrime.isSolved() ? getString(R.string.crime_report_solved) : getString(R.string.crime_report_unsolved);
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect() == null ? getString(R.string.crime_report_no_suspect) : getString(R.string.crime_report_suspect, mCrime.getSuspect());
        return getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);
    }
}
