package com.farizma.colorpalettefromimage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;

    private ClipboardManager clipboardManager;
    private ClipData clipData;

    private Bitmap bitmap = null;
    private Button button;
    private ImageView imageView, back, mode, show;
    private LinearLayout linearLayout;

    private boolean isHex = true;
    private String clipText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusBarConfig();

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        linearLayout = findViewById(R.id.linearLayout);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImage();
            }
        });

        back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mode = findViewById(R.id.mode);
        mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        show = findViewById(R.id.show);
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlert();
            }
        });
    }

    @Override
    public void onBackPressed() {
        recreate();
    }

    private void getImage()  {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_an_image)), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                //TODO: compress image if size is large
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                showToast("width: "+width+", height: "+height);
//                if(width > 1500 || height >1500)
//                    bitmap = Bitmap.createScaledBitmap(bitmap, width/5, height/5, true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(bitmap != null) {
                imageView.setImageBitmap(bitmap);
                button.setVisibility(View.INVISIBLE);
                createPaletteAsync(bitmap);
            }
        }
    }

    private void createPaletteAsync(Bitmap bitmap) {
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                setColor(palette.getVibrantSwatch());
                setColor(palette.getDarkVibrantSwatch());
                setColor(palette.getLightVibrantSwatch());
                setColor(palette.getMutedSwatch());
                setColor(palette.getDarkMutedSwatch());
                setColor(palette.getLightMutedSwatch());

                back.setVisibility(View.VISIBLE);
                mode.setVisibility(View.VISIBLE);
                show.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setColor(Palette.Swatch swatch) {

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        param.setMargins(10,0, 10, 0);

        if(swatch != null) {
            int color = swatch.getRgb();
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);

            final String textRGB = getTextRGB(red, green, blue);
            final String textHex = getTextHex(red, green, blue);

            TextView textView = new TextView(getApplicationContext());
            Drawable dr = getDrawable(R.drawable.box);
            dr.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            textView.setBackground(dr);

            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(isHex) clipText = textHex;
                    else clipText = textRGB;
                    clipData = ClipData.newPlainText("text", clipText);
                    clipboardManager.setPrimaryClip(clipData);
                    showToast(getString(R.string.copied_to_clipboard) + clipText);
                    return true;
                }
            });
            linearLayout.addView(textView, param);
        }
    }

    private String getTextRGB(int red, int green, int blue) {
        String textRGB = "(" + red +", " + green + ", " + blue + ")";
        return textRGB;
    }

    private String getTextHex(int red, int green, int blue) {
        String textHex = "#" +
                Integer.toString(red, 16) +
                Integer.toString(green, 16) +
                Integer.toString(blue, 16);
        return textHex;
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog);

        RadioGroup radioGroup = dialog.findViewById(R.id.radioGroup);
        RadioButton hex = dialog.findViewById(R.id.hex);
        RadioButton rgb = dialog.findViewById(R.id.rgb);

        if(isHex) hex.setChecked(true);
        else rgb.setChecked(true);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int itemChecked) {
                switch (itemChecked) {
                    case R.id.hex: isHex = true;
                        showToast(getString(R.string.mode_hex_message));
                        break;
                    case R.id.rgb: isHex = false;
                        showToast(getString(R.string.mode_rgb_message));
                        break;
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showAlert() {
        final Dialog dialog = new Dialog(this);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.alert);

        Button ok = dialog.findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void statusBarConfig() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = findViewById(R.id.rootView);
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            this.getWindow().setStatusBarColor(Color.WHITE);
        }
    }
}