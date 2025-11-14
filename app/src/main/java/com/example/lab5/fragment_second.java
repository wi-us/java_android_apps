package com.example.lab5;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class fragment_second extends Fragment {

    private TextView questionText;
    private TextView resultsText;
    private Button startButton;
    private Button answerButton;

    private int currentQuestion = 0;
    private int correctAnswers = 0;
    private int totalQuestions = 0;
    private boolean isTestStarted = false;
    private boolean[] resultsArray = new boolean[3]; // Массив для хранения результатов

    public class Quiz
    {
        public Map<String,QuestionData> questions;
        public class QuestionData {
            public String question;
            public String[] answers;
            public int correctIndex;

            public QuestionData(String question, String[] answers, int correctIndex) {
                this.question = question;
                this.answers = answers;
                this.correctIndex = correctIndex;
            }
        }
        public QuestionData getQuestion(String questionId) {
            return questions.get(questionId);
        }
        public Map<String, QuestionData> getAllQuestions() {
            return questions;
        }
        public void addQuestion(String questionId, String question, String[] answers, int correctIndex) {
            questions.put(questionId, new QuestionData(question, answers, correctIndex));
        }
    }

    // Массив вопросов и правильных ответов
    private String[] questions = {
        "Какой язык программирования используется для разработки Android приложений?",
        "Что такое переменная в программировании?",
        "Какое животное является травоядным?"
    };
    
    private String[][] options = {
        {
            "Java",
                "Java Script",
                "Python",
                "C++"
        },
        {
            "Именованная область памяти для хранения данных, значение которой может изменяться в процессе выполнения программы",
                "Постоянное значение, которое нельзя изменить после объявления.",
                "Специальный тип функции",
                "Название программы"
        },
        {
            "Крокодил",
                "Волк",
                "Корова",
                "Лев"
        }
    };
    
    private int[] correctAnswersArray = {0, 0, 2}; // Индексы правильных ответов (0-based)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_second, container, false);
        
        // Инициализация элементов интерфейса
        questionText = view.findViewById(R.id.questionText);
        resultsText = view.findViewById(R.id.resultsText);
        startButton = view.findViewById(R.id.startButton);
        answerButton = view.findViewById(R.id.answerButton);
        
        // Обработчик нажатия на кнопку "Начать тестирование"
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });
        
        // Обработчик нажатия на кнопку "Ответить"
        answerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuizDialog();
            }
        });
        
        return view;
    }
    
    private void startTest() {
        // Скрываем кнопку "Начать тестирование"
        startButton.setVisibility(View.GONE);
        
        // Показываем элементы теста
        questionText.setVisibility(View.VISIBLE);
        answerButton.setVisibility(View.VISIBLE);
        resultsText.setVisibility(View.GONE); // Скрываем результаты до окончания теста
        
        // Сбрасываем счет и результаты
        currentQuestion = 0;
        correctAnswers = 0;
        isTestStarted = true;
        resultsArray = new boolean[3];
        
        // Показываем первый вопрос
        showCurrentQuestion();
    }
    
    private void showCurrentQuestion() {
        if (currentQuestion < questions.length) {
            questionText.setText(questions[currentQuestion]);
            totalQuestions = questions.length;
        } else {
            // Все вопросы пройдены
            questionText.setText("Тестирование завершено!");
            answerButton.setEnabled(false);
            answerButton.setText("Завершено");
            displayResults(); // Показываем результаты
        }
    }
    
    private void showQuizDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_quiz, null);
        
        // Настройка элементов диалога
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button submitButton = dialogView.findViewById(R.id.submitButton);
        
        dialogTitle.setText("Выберите правильный ответ:");
        
        // Очистка предыдущих вариантов
        radioGroup.clearCheck();
        
        // Установка вариантов ответов
        RadioButton option1 = dialogView.findViewById(R.id.option1);
        RadioButton option2 = dialogView.findViewById(R.id.option2);
        RadioButton option3 = dialogView.findViewById(R.id.option3);
        RadioButton option4 = dialogView.findViewById(R.id.option4);
        
        String[] currentOptions = options[currentQuestion];
        option1.setText(currentOptions[0]);
        option2.setText(currentOptions[1]);
        option3.setText(currentOptions[2]);
        option4.setText(currentOptions[3]);
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Обработчики кнопок
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                    int selectedIndex = getSelectedIndex(selectedId);
                    checkAnswer(selectedIndex);
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Пожалуйста, выберите ответ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
    }
    
    private int getSelectedIndex(int selectedId) {
        if (selectedId == R.id.option1) {
            return 0;
        } else if (selectedId == R.id.option2) {
            return 1;
        } else if (selectedId == R.id.option3) {
            return 2;
        } else if (selectedId == R.id.option4) {
            return 3;
        } else {
            return -1;
        }
    }
    
    private void checkAnswer(int selectedIndex) {
        int correctIndex = correctAnswersArray[currentQuestion];
        boolean isCorrect = selectedIndex == correctIndex;
        
        if (isCorrect) {
            correctAnswers++;
        } else {
            String correctAnswer = options[currentQuestion][correctIndex];
        }
        
        // Сохраняем результат
        resultsArray[currentQuestion] = isCorrect;
        
        // Переход к следующему вопросу
        currentQuestion++;
        showCurrentQuestion();
    }
    
    private void displayResults() {
        resultsText.setVisibility(View.VISIBLE);
        
        // Формируем полный текст
        StringBuilder fullText = new StringBuilder();
        for (int i = 0; i < resultsArray.length; i++) {
            String symbol = resultsArray[i] ? "✓" : "✗";
            fullText.append((i + 1)).append(". ").append(symbol).append("\n");
        }
        
        // Создаем SpannableString
        SpannableString spannable = new SpannableString(fullText.toString());
        String text = fullText.toString();
        
        // Применяем цвета к каждому символу
        int currentIndex = 0;
        for (int i = 0; i < resultsArray.length; i++) {
            // Находим позицию символа в строке
            int symbolIndex = text.indexOf((resultsArray[i] ? "✓" : "✗"), currentIndex);
            if (symbolIndex != -1) {
                int color = resultsArray[i] ? Color.rgb(0, 154, 99) : Color.RED;
                spannable.setSpan(new ForegroundColorSpan(color), symbolIndex, symbolIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                currentIndex = symbolIndex + 1;
            }
        }
        
        resultsText.setText(spannable);
    }
}