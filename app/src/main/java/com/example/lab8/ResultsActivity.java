package com.example.lab8;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsActivity extends AppCompatActivity {

    public static final String EXTRA_MATH = "extra_math";
    public static final String EXTRA_RUS = "extra_rus";
    public static final String EXTRA_INFORM = "extra_inform";
    public static final String EXTRA_SOCIAL = "extra_social";
    public static final String EXTRA_CHEMISTRY = "extra_chemistry";
    public static final String EXTRA_PHYSICS = "extra_physics";
    public static final String EXTRA_ENG = "extra_eng";
    public static final String EXTRA_GEO = "extra_geo";

    private MyAdapter adapter;
    private ProgressBar progress;

    private Map<String, Integer> entered;
    private boolean showAll = false;

    private final List<Api.SpecialtyDto> listResult = new ArrayList<>();
    private final List<Api.SpecialtyDto> listAll = new ArrayList<>();

    private MaterialButton buttonResult;
    private MaterialButton buttonAll;
    private MaterialButton buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        progress = findViewById(R.id.progress);
        buttonResult = findViewById(R.id.button_result);
        buttonAll = findViewById(R.id.button_all);
        buttonBack = findViewById(R.id.button_back);

        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter();
        rv.setAdapter(adapter);

        entered = readEnteredFromIntent();
        setupModeButtons();
        setupBackButton();
        loadSpecialties(false);
    }

    private void setupBackButton() {
        if (buttonBack == null) return;
        buttonBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupModeButtons() {
        buttonResult.setOnClickListener(v -> {
            showAll = false;
            if (listResult.isEmpty()) loadSpecialties(false);
            else render();
        });
        buttonAll.setOnClickListener(v -> {
            showAll = true;
            if (listAll.isEmpty()) loadSpecialties(true);
            else render();
        });
        updateModeButtons();
    }

    private Map<String, Integer> readEnteredFromIntent() {
        Map<String, Integer> m = new HashMap<>();
        m.put("math", getIntent().getIntExtra(EXTRA_MATH, 0));
        m.put("rus", getIntent().getIntExtra(EXTRA_RUS, 0));
        m.put("inform", getIntent().getIntExtra(EXTRA_INFORM, 0));
        m.put("social", getIntent().getIntExtra(EXTRA_SOCIAL, 0));
        m.put("chemistry", getIntent().getIntExtra(EXTRA_CHEMISTRY, 0));
        m.put("physics", getIntent().getIntExtra(EXTRA_PHYSICS, 0));
        m.put("eng", getIntent().getIntExtra(EXTRA_ENG, 0));
        m.put("geo", getIntent().getIntExtra(EXTRA_GEO, 0));
        return m;
    }

    private void loadSpecialties(boolean loadAll) {
        showLoading(true);

        Map<String, Integer> body = new HashMap<>();
        if (loadAll) {
            // "Все дисциплины" — просим у API полный список (100 везде)
            body.put("math", 100);
            body.put("inform", 100);
            body.put("social", 100);
            body.put("rus", 100);
            body.put("chemistry", 100);
            body.put("physics", 100);
            body.put("eng", 100);
            body.put("geo", 100);
        } else {
            // "Результат" — реальные введённые баллы
            body.put("math", entered.get("math"));
            body.put("inform", entered.get("inform"));
            body.put("social", entered.get("social"));
            body.put("rus", entered.get("rus"));
            body.put("chemistry", entered.get("chemistry"));
            body.put("physics", entered.get("physics"));
            body.put("eng", entered.get("eng"));
            body.put("geo", entered.get("geo"));
        }

        Api.getApi()
                .postScores(body)
                .enqueue(new Callback<List<Api.SpecialtyDto>>() {
                    @Override
                    public void onResponse(Call<List<Api.SpecialtyDto>> call, Response<List<Api.SpecialtyDto>> response) {
                        showLoading(false);
                        if (!response.isSuccessful()) {
                            Toast.makeText(ResultsActivity.this, "Ошибка API: " + response.code(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        List<Api.SpecialtyDto> target = loadAll ? listAll : listResult;
                        target.clear();
                        if (response.body() != null) target.addAll(response.body());
                        render();
                    }

                    @Override
                    public void onFailure(Call<List<Api.SpecialtyDto>> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(ResultsActivity.this, "Сеть недоступна: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void render() {
        updateModeButtons();
        List<Api.SpecialtyDto> src = showAll ? listAll : listResult;
        List<Item> list = buildUi(src, showAll);
        adapter.submit(list);
        if (!showAll && src.size() > 0 && list.isEmpty()) {
            Toast.makeText(this, "Нет дисциплин, проходящих по введённым баллам", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateModeButtons() {
        if (buttonResult == null || buttonAll == null) return;

        int primary = ContextCompat.getColor(this, R.color.color_primary);
        int secondary = ContextCompat.getColor(this, R.color.color_secondary);

        ColorStateList primaryTint = ColorStateList.valueOf(primary);
        ColorStateList secondaryTint = ColorStateList.valueOf(secondary);

        if (showAll) {
            buttonAll.setBackgroundTintList(secondaryTint);
            buttonResult.setBackgroundTintList(primaryTint);
        } else {
            buttonResult.setBackgroundTintList(secondaryTint);
            buttonAll.setBackgroundTintList(primaryTint);
        }
    }

    private List<Item> buildUi(List<Api.SpecialtyDto> specialties, boolean showAll) {
        List<Item> out = new ArrayList<>();
        if (specialties == null) return out;

        for (Api.SpecialtyDto s : specialties) {
            if (!showAll && !passesThresholds(s)) {
                continue;
            }

            String title = s.specialtyName != null ? s.specialtyName : "(без названия)";
            out.add(new HeaderItem(title));

            addIfRequired(out, "math", "Математика", s.mathPoint);
            addIfRequired(out, "rus", "Русский язык", s.rusPoint);
            addIfRequired(out, "inform", "Информатика", s.itkPoint);
            addIfRequired(out, "physics", "Физика", s.physPoint);
            addIfRequired(out, "chemistry", "Химия", s.chemPoint);
            addIfRequired(out, "social", "Обществознание", s.socPoint);
        }
        return out;
    }

    private boolean passesThresholds(Api.SpecialtyDto s) {
        return check("math", s.mathPoint)
                && check("rus", s.rusPoint)
                && check("inform", s.itkPoint)
                && check("physics", s.physPoint)
                && check("chemistry", s.chemPoint)
                && check("social", s.socPoint);
    }

    private boolean check(String key, int minPoint) {
        if (minPoint <= 0) return true;
        int enteredValue = entered != null && entered.get(key) != null ? entered.get(key) : 0;
        return enteredValue >= minPoint;
    }

    private void addIfRequired(List<Item> out, String key, String name, int minPoint) {
        if (minPoint <= 0) return;
        Integer enteredValue = entered.get(key);
        out.add(new RowItem(key, name, minPoint, enteredValue));
    }

    // ---------------- Adapter ----------------

    private interface Item {
        int TYPE_HEADER = 0;
        int TYPE_ROW = 1;
        int type();
    }

    private static class HeaderItem implements Item {
        final String title;
        HeaderItem(String title) { this.title = title; }
        @Override public int type() { return TYPE_HEADER; }
    }

    private static class RowItem implements Item {
        final String id;
        final String name;
        final int min;
        final Integer entered;

        RowItem(String id, String name, int min, Integer entered) {
            this.id = id;
            this.name = name;
            this.min = min;
            this.entered = entered;
        }

        @Override public int type() { return TYPE_ROW; }
    }

    private static class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Item> items = new ArrayList<>();

        void submit(List<Item> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == Item.TYPE_HEADER) {
                View v = inf.inflate(R.layout.speciality_header_item, parent, false);
                return new HeaderVH(v);
            } else {
                View v = inf.inflate(R.layout.speciality_row_item, parent, false);
                return new RowVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Item item = items.get(position);
            if (holder instanceof HeaderVH) {
                HeaderItem h = (HeaderItem) item;
                ((HeaderVH) holder).title.setText(h.title);
            } else {
                RowItem r = (RowItem) item;
                RowVH vh = (RowVH) holder;
                vh.name.setText(r.name);
                vh.min.setText(String.valueOf(r.min));
                vh.entered.setText(r.entered == null ? "-" : String.valueOf(r.entered));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            final TextView title;
            HeaderVH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvDisciplineTitle);
            }
        }

        static class RowVH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView min;
            final TextView entered;

            RowVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvName);
                min = itemView.findViewById(R.id.tvMin);
                entered = itemView.findViewById(R.id.tvEntered);
            }
        }
    }
}


