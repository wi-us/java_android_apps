package com.example.lab2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {


    public static class NumbersRow
    {
        public enum operations
        {
            PLUS,
            MINUS,
            DIV,
            UMN,
            NONE,
            EQUAL
        };

        public NumbersRow() {
            this.number1 = 0;
            this.number2 = 0;
            this.result = 0;
        }
        public double number1;
        public double number2;
        public operations method = operations.NONE;
        public double result;
        public void setNumber1(double num)
        {
            this.number1 = num;
        }
        public void setNumber2(double num)
        {
            this.number2 = num;
        }
        public void calculate()
        {
            switch (this.method) {
                case PLUS:
                    this.result = this.number1 + this.number2;
                    break;
                case MINUS:
                    this.result = this.number1 - this.number2;
                    break;
                case DIV:
                    if (this.number2 != 0) {
                        this.result = this.number1 / this.number2;
                    } else {
                        this.result = 0;
                    }
                    break;
                case UMN:
                    this.result = this.number1 * this.number2;
                    break;
                case NONE:
                case EQUAL:
                    this.result = 0;
                    break;
            }
        }
    }

    NumbersRow numbersRow = new NumbersRow();
    Button buttonPlus;
    Button buttonMinus;
    Button buttonDiv;
    Button buttonUmn;
    Button buttonClear;
    EditText num1;
    EditText num2;
    TextView num3;
    Button[] buttons;
    TextView operatorView;
    private void updateResultTextView() {
        num3.setText("=" + String.valueOf(numbersRow.result));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        buttonPlus = findViewById(R.id.button_plus);
        buttonMinus = findViewById(R.id.button_minus);
        buttonDiv = findViewById(R.id.button_div);
        buttonUmn = findViewById(R.id.button_umn);
        buttonClear = findViewById(R.id.button_clear);

        buttons = new Button[]{buttonPlus, buttonMinus, buttonDiv, buttonUmn};
        num1 = findViewById(R.id.text_calculatorText1);
        num2 = findViewById(R.id.text_calculatorText2);
        num3 = findViewById(R.id.text_calculatorText3);
        operatorView = findViewById(R.id.text_operator);

        num1.setText("0");
        num2.setText("0");
        num3.setText("0");
        updateResultTextView();
        updateButtonColors(null);

        num1.setOnClickListener(v -> {
            if (num1.getText().toString().equals("0")) {
                num1.setText("");
            }
        });

        num2.setOnClickListener(v -> {
            if (num2.getText().toString().equals("0")) {
                num2.setText("");
            }
        });

        num1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentText = s.toString();
                if (currentText.isEmpty()) return;
                try {
                    double number = Double.parseDouble(currentText);
                    numbersRow.setNumber1(number);
                    numbersRow.calculate();
                    updateResultTextView();
                } catch (NumberFormatException e) {
                    Log.e("EditText", "Не удалось преобразовать в число: " + currentText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String currentText = s.toString();
                if (currentText.isEmpty()) return;
                try {
                    double number = Double.parseDouble(currentText);
                    numbersRow.setNumber1(number);
                    numbersRow.calculate();
                    updateResultTextView();
                } catch (NumberFormatException e) {
                    Log.e("EditText", "Не удалось преобразовать в число: " + currentText);
                }
            }
        });
        num2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentText = s.toString();
                if (currentText.isEmpty()) return;
                try {
                    double number = Double.parseDouble(currentText);
                    numbersRow.setNumber2(number);
                    performCalculation();
                } catch (NumberFormatException e) {
                    Log.e("EditText", "Не удалось преобразовать в число: " + currentText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String currentText = s.toString();
                if (currentText.isEmpty()) return;
                try {
                    double number = Double.parseDouble(currentText);
                    numbersRow.setNumber2(number);
                    performCalculation();
                } catch (NumberFormatException e) {
                    Log.e("EditText", "Не удалось преобразовать в число: " + currentText);
                }
            }
        });


        buttonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numbersRow.method = NumbersRow.operations.PLUS;
                performCalculation();
                numbersRow.calculate();
                updateResultTextView();
                updateButtonColors(buttonPlus);
                updateOperatorView();
            }
        });

        buttonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numbersRow.method = NumbersRow.operations.MINUS;
                performCalculation();
                numbersRow.calculate();
                updateResultTextView();
                updateButtonColors(buttonMinus);
                updateOperatorView();
            }
        });

        buttonDiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numbersRow.method = NumbersRow.operations.DIV;
                performCalculation();
                numbersRow.calculate();
                updateResultTextView();
                updateButtonColors(buttonDiv);
                updateOperatorView();
            }
        });

        buttonUmn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numbersRow.method = NumbersRow.operations.UMN;
                performCalculation();
                numbersRow.calculate();
                updateResultTextView();
                updateButtonColors(buttonUmn);
                updateOperatorView();
            }
        });

        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numbersRow.method = NumbersRow.operations.NONE;
                num1.setText("0");
                num2.setText("0");
                num3.setText("0");
                numbersRow.setNumber1(0);
                numbersRow.setNumber2(0);
                numbersRow.result = 0;
                updateButtonColors(null);
                updateOperatorView();
            }
        });
    }
    private void updateButtonColors(Button selectedButton) {
        for (Button button : buttons) {
            if (button == selectedButton) {
                button.setBackgroundColor(getColor(R.color.pressed));
            } else {
                button.setBackgroundColor(getColor(R.color.def));
            }
        }
    }
    private void updateOperatorView() {
        switch (numbersRow.method) {
            case PLUS:
                operatorView.setText("+");
                operatorView.setVisibility(View.VISIBLE);
                break;
            case MINUS:
                operatorView.setText("-");
                operatorView.setVisibility(View.VISIBLE);
                break;
            case DIV:
                operatorView.setText("/");
                operatorView.setVisibility(View.VISIBLE);
                break;
            case UMN:
                operatorView.setText("*");
                operatorView.setVisibility(View.VISIBLE);
                break;
            case NONE:
            case EQUAL:
                operatorView.setVisibility(View.INVISIBLE);
                break;
        }
    }
    private void performCalculation() {
        if (numbersRow.method == NumbersRow.operations.DIV && numbersRow.number2 == 0) {
            showDivisionByZeroDialog();
            num3.setText("NaN");
            numbersRow.result = 0;
        } else {
            numbersRow.calculate();
            updateResultTextView();
        }
    }
    private void showDivisionByZeroDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage("Деление на ноль невозможно.")
                .setPositiveButton("OK", null)
                .show();
    }
}