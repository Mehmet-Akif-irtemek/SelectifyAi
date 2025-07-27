
package com.app.selectifyai;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TodoActivity extends AnaAktivite {

    private ProgressBar loadingBar;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private List<Todo> todoList = new ArrayList<>();
    private TodoManager todoManager = new TodoManager();

    Button btnPriorityReminder;
    String selectedPriority;
    Calendar selectedTime = null;
    
    private EditText searchEditText;
    private Spinner prioritySpinner, categorySpinner, statusSpinner;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todoview);

        createNotificationChannel();
        scheduleEndOfDayCheck();
        hideSystemUI();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Varsayılan öncelik değerini ayarla
        selectedPriority = getString(R.string.medium);
        
        // Arama ve filtreleme bileşenlerini başlat
        initSearchAndFilters();

        EditText editTextTodo = findViewById(R.id.editTextTodo);
        editTextTodo.setHint(getString(R.string.add_task));
        ImageView buttonAdd = findViewById(R.id.imageButton);
        buttonAdd.setContentDescription(getString(R.string.todo_add_button));
        recyclerView = findViewById(R.id.recyclerViewTodo);
        com.airbnb.lottie.LottieAnimationView lottieLoading = findViewById(R.id.lottieLoading);
        //TextView priorityLabel = findViewById(R.id.textView20); // kaldırıldı
        //priorityLabel.setText(getString(R.string.todo_priority_label));
        //TextView gorevSayaci = findViewById(R.id.gorevSayaci); // kaldırıldı
        //gorevSayaci.setText(getString(R.string.todo_total_count, todoList.size()));
        // Lottie için boş mesajı (gerekirse erişim)
        lottieLoading.setContentDescription(getString(R.string.todo_no_tasks));

        recyclerView.setVisibility(View.GONE);
        lottieLoading.setVisibility(View.VISIBLE);

        adapter = new TodoAdapter(this, todoList, new TodoAdapter.TodoListener() {
            @Override
            public void onCheckboxChanged(Todo todo, boolean checked) {
                todoManager.tamamlandiGuncelle(todo.getId(), checked);
            }

            @Override
            public void onDeleteClicked(Todo todo) {
                todoManager.sil(todo.getId());
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView r, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Todo deleted = todoList.get(pos);
                todoManager.sil(deleted.getId());
                Snackbar.make(recyclerView, getString(R.string.task_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.undo), v -> todoManager.yeniTodoEkle(deleted))
                        .show();
            }
        }).attachToRecyclerView(recyclerView);

        todoManager.getTodoRef().addSnapshotListener((value, error) -> {
            if (value != null) {
                todoList.clear();
                for (var doc : value.getDocuments()) {
                    Todo t = doc.toObject(Todo.class);
                    if (t != null) {
                        t.setId(doc.getId());
                        todoList.add(t);
                    }
                }
                adapter.updateData();
                if (todoList.isEmpty()) {
                    lottieLoading.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    lottieLoading.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        buttonAdd.setOnClickListener(v -> {
            // Ekleme butonuna tıklanınca önce öncelik ve zaman dialogunu aç
            showPriorityReminderDialog();
        });

        btnPriorityReminder = findViewById(R.id.btnPriorityReminder);
        btnPriorityReminder.setOnClickListener(v -> showPriorityReminderDialog());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, AnaEkran.class));
                return true;
            } else if (item.getItemId() == R.id.nav_tasks) {
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                startActivity(new Intent(this, Profil.class));
                return true;
            }
            return false;
        });

        // Bildirim izni kontrolü ve isteği (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 2001);
            }
        }
    }

    private void createNotificationChannel() {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder("todo_channel", NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(getString(R.string.notification_channel_name))
                .setDescription(getString(R.string.notification_channel_desc))
                .build();
        NotificationManagerCompat.from(this).createNotificationChannel(channel);
    }

    private void scheduleEndOfDayCheck() {
        Intent intent = new Intent(this, TodoReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void showPriorityReminderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_priority_reminder, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupPriority);
        Spinner categorySpinnerDialog = dialogView.findViewById(R.id.categorySpinnerDialog);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        Button btnPickTime = dialogView.findViewById(R.id.btnPickTime);
        TextView textPickedTime = dialogView.findViewById(R.id.textPickedTime);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        EditText editTextTodo = findViewById(R.id.editTextTodo);

        // Kategori spinner'ını ayarla
        String[] categories = {getString(R.string.category_general), getString(R.string.category_work), 
                              getString(R.string.category_personal), getString(R.string.category_shopping), 
                              getString(R.string.category_health), getString(R.string.category_education)};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinnerDialog.setAdapter(categoryAdapter);

        radioGroup.check(R.id.radioMedium);
        btnPickTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                textPickedTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
            timePicker.show();
        });
        btnSave.setOnClickListener(v -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.radioHigh) selectedPriority = getString(R.string.high);
            else if (checkedId == R.id.radioMedium) selectedPriority = getString(R.string.medium);
            else if (checkedId == R.id.radioLow) selectedPriority = getString(R.string.low);
            
            String todoTitle = editTextTodo.getText().toString().trim();
            String selectedCategory = categorySpinnerDialog.getSelectedItem().toString();
            String description = editTextDescription.getText().toString().trim();
            
            // Çevrilmiş kategoriyi Türkçe karşılığına çevir
            String turkishCategory = mapCategoryToTurkish(selectedCategory);
            // Çevrilmiş önceliği Türkçe karşılığına çevir
            String turkishPriority = mapPriorityToTurkish(selectedPriority);
            
            if (!todoTitle.isEmpty()) {
                todoManager.yeniTodoEkle(todoTitle, turkishPriority, turkishCategory, description);
                editTextTodo.setText("");
                if (selectedTime != null) {
                    setTodoAlarm(selectedTime, todoTitle);
                }
            }
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setTodoAlarm(Calendar calendar, String todoTitle) {
        Intent intent = new Intent(this, TodoSingleReminderReceiver.class);
        intent.putExtra("todo_title", todoTitle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.notification_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.notification_permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initSearchAndFilters() {
        searchEditText = findViewById(R.id.searchEditText);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        statusSpinner = findViewById(R.id.statusSpinner);

        // Arama işlevselliği
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Öncelik spinner'ı
        String[] priorities = {getString(R.string.filter_all), getString(R.string.high), 
                              getString(R.string.medium), getString(R.string.low)};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.filterByPriority(priorities[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Kategori spinner'ı
        String[] categories = {getString(R.string.filter_all), getString(R.string.category_general), 
                              getString(R.string.category_work), getString(R.string.category_personal), 
                              getString(R.string.category_shopping), getString(R.string.category_health), 
                              getString(R.string.category_education)};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.filterByCategory(categories[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Durum spinner'ı
        String[] statuses = {getString(R.string.filter_all), getString(R.string.filter_pending), 
                            getString(R.string.filter_completed)};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.filterByStatus(statuses[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Çevrilmiş öncelik değerini Türkçe karşılığına çevir
    private String mapPriorityToTurkish(String translatedPriority) {
        if (translatedPriority.equals(getString(R.string.high))) {
            return "Yüksek";
        } else if (translatedPriority.equals(getString(R.string.medium))) {
            return "Orta";
        } else if (translatedPriority.equals(getString(R.string.low))) {
            return "Düşük";
        }
        return translatedPriority;
    }

    // Çevrilmiş kategori değerini Türkçe karşılığına çevir
    private String mapCategoryToTurkish(String translatedCategory) {
        if (translatedCategory.equals(getString(R.string.category_general))) {
            return "Genel";
        } else if (translatedCategory.equals(getString(R.string.category_work))) {
            return "İş";
        } else if (translatedCategory.equals(getString(R.string.category_personal))) {
            return "Kişisel";
        } else if (translatedCategory.equals(getString(R.string.category_shopping))) {
            return "Alışveriş";
        } else if (translatedCategory.equals(getString(R.string.category_health))) {
            return "Sağlık";
        } else if (translatedCategory.equals(getString(R.string.category_education))) {
            return "Eğitim";
        }
        return translatedCategory;
    }
}
