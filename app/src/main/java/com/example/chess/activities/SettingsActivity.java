package com.example.chess.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chess.R;

public class SettingsActivity extends AppCompatActivity {

    // Declare the UI components
    private Button backButton;
    private Button selectedPieceButton;
    private Button availableMovesButton;
    private ImageView board1ImageView;
    private ImageView board2ImageView;
    private ImageView board3ImageView;
    private ImageView board4ImageView;

    private int whiteSideSquareColor;
    private int blackSideSquareColor;

    private int availableMovesColor;
    private int selectedPieceColor;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        backButton = findViewById(R.id.backButton);
        selectedPieceButton = findViewById(R.id.selectedPieceButton);
        availableMovesButton = findViewById(R.id.availableMovesButton);
        board1ImageView = findViewById(R.id.imageViewBoard1);
        board2ImageView = findViewById(R.id.imageViewBoard2);
        board3ImageView = findViewById(R.id.imageViewBoard3);
        board4ImageView = findViewById(R.id.imageViewBoard4);


        sharedPreferences = getSharedPreferences("ChessSettings", MODE_PRIVATE);

        whiteSideSquareColor = sharedPreferences.getInt("whiteSideSquareColor", Color.parseColor("#FFFFFF"));
        blackSideSquareColor = sharedPreferences.getInt("blackSideSquareColor", Color.parseColor("#E0E0E0"));
        availableMovesColor = sharedPreferences.getInt("availableMovesColor", Color.parseColor("#ADD8E6"));
        selectedPieceColor = sharedPreferences.getInt("selectedPieceColor", Color.parseColor("#ADD8E6"));

        // Set up click listeners for the buttons
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        selectedPieceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog popupDialog = new Dialog(v.getContext());
                popupDialog.setContentView(R.layout.activity_colors);

                // Identify color views
                View color1View = popupDialog.findViewById(R.id.color1_view);
                View color2View = popupDialog.findViewById(R.id.color2_view);
                View color3View = popupDialog.findViewById(R.id.color3_view);
                View color4View = popupDialog.findViewById(R.id.color4_view);
                View color5View = popupDialog.findViewById(R.id.color5_view);
                View color6View = popupDialog.findViewById(R.id.color6_view);
                View color7View = popupDialog.findViewById(R.id.color7_view);
                View color8View = popupDialog.findViewById(R.id.color8_view);
                View color9View = popupDialog.findViewById(R.id.color9_view);
                View color10View = popupDialog.findViewById(R.id.color10_view);
                View color11View = popupDialog.findViewById(R.id.color11_view);
                View color12View = popupDialog.findViewById(R.id.color12_view);


                View.OnClickListener colorClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int viewId = v.getId();
                        if (viewId == R.id.color1_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color1);
                        } else if (viewId == R.id.color2_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color2);
                        } else if (viewId == R.id.color3_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color3);
                        } else if (viewId == R.id.color4_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color4);
                        } else if (viewId == R.id.color5_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color5);
                        } else if (viewId == R.id.color6_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color6);
                        } else if (viewId == R.id.color7_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color7);
                        } else if (viewId == R.id.color8_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color8);
                        } else if (viewId == R.id.color9_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color9);
                        } else if (viewId == R.id.color10_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color10);
                        } else if (viewId == R.id.color11_view) {
                            selectedPieceColor = getResources().getColor(R.color.faded_color11);
                        } else if (viewId == R.id.color12_view) {
                            selectedPieceColor = getResources().getColor(R.color.light_blue);
                        }

                        saveColorToSharedPreferences("selectedPieceColor", selectedPieceColor);
                        popupDialog.dismiss();
                    }
                };
                color1View.setOnClickListener(colorClickListener);
                color2View.setOnClickListener(colorClickListener);
                color3View.setOnClickListener(colorClickListener);
                color4View.setOnClickListener(colorClickListener);
                color5View.setOnClickListener(colorClickListener);
                color6View.setOnClickListener(colorClickListener);
                color7View.setOnClickListener(colorClickListener);
                color8View.setOnClickListener(colorClickListener);
                color9View.setOnClickListener(colorClickListener);
                color10View.setOnClickListener(colorClickListener);
                color11View.setOnClickListener(colorClickListener);
                color12View.setOnClickListener(colorClickListener);
                popupDialog.show();
            }
        });

        availableMovesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog popupDialog = new Dialog(v.getContext());
                popupDialog.setContentView(R.layout.activity_colors);

                // Identify color views
                View color1View = popupDialog.findViewById(R.id.color1_view);
                View color2View = popupDialog.findViewById(R.id.color2_view);
                View color3View = popupDialog.findViewById(R.id.color3_view);
                View color4View = popupDialog.findViewById(R.id.color4_view);
                View color5View = popupDialog.findViewById(R.id.color5_view);
                View color6View = popupDialog.findViewById(R.id.color6_view);
                View color7View = popupDialog.findViewById(R.id.color7_view);
                View color8View = popupDialog.findViewById(R.id.color8_view);
                View color9View = popupDialog.findViewById(R.id.color9_view);
                View color10View = popupDialog.findViewById(R.id.color10_view);
                View color11View = popupDialog.findViewById(R.id.color11_view);
                View color12View = popupDialog.findViewById(R.id.color12_view);


                View.OnClickListener colorClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int viewId = v.getId();
                        if (viewId == R.id.color1_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color1);
                        } else if (viewId == R.id.color2_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color2);
                        } else if (viewId == R.id.color3_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color3);
                        } else if (viewId == R.id.color4_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color4);
                        } else if (viewId == R.id.color5_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color5);
                        } else if (viewId == R.id.color6_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color6);
                        } else if (viewId == R.id.color7_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color7);
                        } else if (viewId == R.id.color8_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color8);
                        } else if (viewId == R.id.color9_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color9);
                        } else if (viewId == R.id.color10_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color10);
                        } else if (viewId == R.id.color11_view) {
                            availableMovesColor = getResources().getColor(R.color.faded_color11);
                        } else if (viewId == R.id.color12_view) {
                            availableMovesColor = getResources().getColor(R.color.light_blue);
                        }

                        saveColorToSharedPreferences("availableMovesColor", availableMovesColor);
                        popupDialog.dismiss();
                    }
                };
                color1View.setOnClickListener(colorClickListener);
                color2View.setOnClickListener(colorClickListener);
                color3View.setOnClickListener(colorClickListener);
                color4View.setOnClickListener(colorClickListener);
                color5View.setOnClickListener(colorClickListener);
                color6View.setOnClickListener(colorClickListener);
                color7View.setOnClickListener(colorClickListener);
                color8View.setOnClickListener(colorClickListener);
                color9View.setOnClickListener(colorClickListener);
                color10View.setOnClickListener(colorClickListener);
                color11View.setOnClickListener(colorClickListener);
                color12View.setOnClickListener(colorClickListener);
                popupDialog.show();
            }
        });

        board1ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whiteSideSquareColor = Color.parseColor("#F1DAB5");
                blackSideSquareColor = Color.parseColor("#B58863");
                saveColorToSharedPreferences("whiteSideSquareColor", whiteSideSquareColor);
                saveColorToSharedPreferences("blackSideSquareColor", blackSideSquareColor);
            }
        });

        board2ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whiteSideSquareColor = Color.parseColor("#E5DFC5");
                blackSideSquareColor = Color.parseColor("#49A248");
                saveColorToSharedPreferences("whiteSideSquareColor", whiteSideSquareColor);
                saveColorToSharedPreferences("blackSideSquareColor", blackSideSquareColor);
            }
        });

        board3ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whiteSideSquareColor = Color.parseColor("#9F9F9F");
                blackSideSquareColor = Color.parseColor("#666666");
                saveColorToSharedPreferences("whiteSideSquareColor", whiteSideSquareColor);
                saveColorToSharedPreferences("blackSideSquareColor", blackSideSquareColor);
            }
        });

        board4ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whiteSideSquareColor = Color.parseColor("#8EBADE");
                blackSideSquareColor = Color.parseColor("#3D518F");
                saveColorToSharedPreferences("whiteSideSquareColor", whiteSideSquareColor);
                saveColorToSharedPreferences("blackSideSquareColor", blackSideSquareColor);
            }
        });

    }
    private void saveColorToSharedPreferences(String key, int colorValue) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, colorValue);
        editor.apply();
    }
}
