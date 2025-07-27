package com.app.selectifyai;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.selectifyai.Todo;

import java.util.List;
import java.util.ArrayList;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private Context context;
    private List<Todo> todoList;
    private List<Todo> filteredTodoList;
    private TodoListener listener;

    public interface TodoListener {
        void onCheckboxChanged(Todo todo, boolean checked);
        void onDeleteClicked(Todo todo);
    }

    public TodoAdapter(Context context, List<Todo> todoList, TodoListener listener) {
        this.context = context;
        this.todoList = todoList;
        this.filteredTodoList = new ArrayList<>(todoList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = filteredTodoList.get(position);
        holder.textTask.setText(todo.getBaslik());
        holder.checkDone.setChecked(todo.isTamamlandi());

        // Kategori göster
        if (todo.getKategori() != null && !todo.getKategori().isEmpty()) {
            // Türkçe kategoriyi çevrilmiş karşılığına çevir
            String displayCategory = mapTurkishCategoryToTranslated(todo.getKategori());
            holder.textCategory.setText(displayCategory);
            holder.textCategory.setVisibility(View.VISIBLE);
        } else {
            holder.textCategory.setVisibility(View.GONE);
        }

        // Öncelik göster
        if (todo.getOncelik() != null && !todo.getOncelik().isEmpty()) {
            // Türkçe önceliği çevrilmiş karşılığına çevir
            String displayPriority = mapTurkishPriorityToTranslated(todo.getOncelik());
            holder.textPriority.setText(displayPriority);
            holder.textPriority.setVisibility(View.VISIBLE);
            
            // Öncelik rengini ayarla
            int priorityColor;
            switch (todo.getOncelik().toLowerCase()) {
                case "yüksek":
                    priorityColor = context.getResources().getColor(R.color.priority_red);
                    break;
                case "orta":
                    priorityColor = context.getResources().getColor(R.color.priority_orange);
                    break;
                case "düşük":
                    priorityColor = context.getResources().getColor(R.color.priority_green);
                    break;
                default:
                    priorityColor = context.getResources().getColor(R.color.priority_default);
            }
            holder.textPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(priorityColor));
        } else {
            holder.textPriority.setVisibility(View.GONE);
        }

        // Açıklama göster
        if (todo.getAciklama() != null && !todo.getAciklama().isEmpty()) {
            holder.textDescription.setText(todo.getAciklama());
            holder.textDescription.setVisibility(View.VISIBLE);
        } else {
            holder.textDescription.setVisibility(View.GONE);
        }

        // Tamamlandıysa üstü çizili efekt
        if (todo.isTamamlandi()) {
            holder.textTask.setPaintFlags(holder.textTask.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.textTask.setPaintFlags(holder.textTask.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Checkbox değişince listener'a bildir
        holder.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onCheckboxChanged(todo, isChecked);
        });

        // Silme butonu listener'ı
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(todo));
        holder.btnDelete.setContentDescription(context.getString(R.string.todo_delete_content_desc));

        // Öncelik göstergesi rengi
        String oncelik = todo.getOncelik();
        if (oncelik != null) {
            int color;
            switch (oncelik.toLowerCase()) {
                case "yüksek":
                    color = context.getResources().getColor(R.color.priority_red);
                    break;
                case "orta":
                    color = context.getResources().getColor(R.color.priority_orange);
                    break;
                case "düşük":
                    color = context.getResources().getColor(R.color.priority_green);
                    break;
                default:
                    color = context.getResources().getColor(R.color.priority_default);
            }
            holder.iconPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        } else {
            holder.iconPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getResources().getColor(R.color.priority_default)));
        }
    }
    @Override
    public int getItemCount() {
        return filteredTodoList.size();
    }

    // Arama fonksiyonu
    public void filter(String query) {
        filteredTodoList.clear();
        if (query.isEmpty()) {
            filteredTodoList.addAll(todoList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Todo todo : todoList) {
                if (todo.getBaslik().toLowerCase().contains(lowerCaseQuery) ||
                    (todo.getKategori() != null && todo.getKategori().toLowerCase().contains(lowerCaseQuery)) ||
                    (todo.getAciklama() != null && todo.getAciklama().toLowerCase().contains(lowerCaseQuery))) {
                    filteredTodoList.add(todo);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Öncelik filtreleme
    public void filterByPriority(String priority) {
        filteredTodoList.clear();
        if (priority.equals(context.getString(R.string.filter_all))) {
            filteredTodoList.addAll(todoList);
        } else {
            // Çevrilmiş öncelik değerini Türkçe karşılığına çevir
            String turkishPriority = mapPriorityToTurkish(priority);
            for (Todo todo : todoList) {
                if (turkishPriority.equals(todo.getOncelik())) {
                    filteredTodoList.add(todo);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Kategori filtreleme
    public void filterByCategory(String category) {
        filteredTodoList.clear();
        if (category.equals(context.getString(R.string.filter_all))) {
            filteredTodoList.addAll(todoList);
        } else {
            // Çevrilmiş kategori değerini Türkçe karşılığına çevir
            String turkishCategory = mapCategoryToTurkish(category);
            for (Todo todo : todoList) {
                if (turkishCategory.equals(todo.getKategori())) {
                    filteredTodoList.add(todo);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Durum filtreleme
    public void filterByStatus(String status) {
        filteredTodoList.clear();
        if (status.equals(context.getString(R.string.filter_all))) {
            filteredTodoList.addAll(todoList);
        } else if (status.equals(context.getString(R.string.filter_completed))) {
            for (Todo todo : todoList) {
                if (todo.isTamamlandi()) {
                    filteredTodoList.add(todo);
                }
            }
        } else if (status.equals(context.getString(R.string.filter_pending))) {
            for (Todo todo : todoList) {
                if (!todo.isTamamlandi()) {
                    filteredTodoList.add(todo);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Veri güncellendiğinde filtrelenmiş listeyi de güncelle
    public void updateData() {
        filteredTodoList.clear();
        filteredTodoList.addAll(todoList);
        notifyDataSetChanged();
    }

    // Çevrilmiş öncelik değerini Türkçe karşılığına çevir
    private String mapPriorityToTurkish(String translatedPriority) {
        if (translatedPriority.equals(context.getString(R.string.high))) {
            return "Yüksek";
        } else if (translatedPriority.equals(context.getString(R.string.medium))) {
            return "Orta";
        } else if (translatedPriority.equals(context.getString(R.string.low))) {
            return "Düşük";
        }
        return translatedPriority;
    }

    // Çevrilmiş kategori değerini Türkçe karşılığına çevir
    private String mapCategoryToTurkish(String translatedCategory) {
        if (translatedCategory.equals(context.getString(R.string.category_general))) {
            return "Genel";
        } else if (translatedCategory.equals(context.getString(R.string.category_work))) {
            return "İş";
        } else if (translatedCategory.equals(context.getString(R.string.category_personal))) {
            return "Kişisel";
        } else if (translatedCategory.equals(context.getString(R.string.category_shopping))) {
            return "Alışveriş";
        } else if (translatedCategory.equals(context.getString(R.string.category_health))) {
            return "Sağlık";
        } else if (translatedCategory.equals(context.getString(R.string.category_education))) {
            return "Eğitim";
        }
        return translatedCategory;
    }

    // Türkçe önceliği çevrilmiş karşılığına çevir (gösterim için)
    private String mapTurkishPriorityToTranslated(String turkishPriority) {
        if ("Yüksek".equals(turkishPriority)) {
            return context.getString(R.string.high);
        } else if ("Orta".equals(turkishPriority)) {
            return context.getString(R.string.medium);
        } else if ("Düşük".equals(turkishPriority)) {
            return context.getString(R.string.low);
        }
        return turkishPriority;
    }

    // Türkçe kategoriyi çevrilmiş karşılığına çevir (gösterim için)
    private String mapTurkishCategoryToTranslated(String turkishCategory) {
        if ("Genel".equals(turkishCategory)) {
            return context.getString(R.string.category_general);
        } else if ("İş".equals(turkishCategory)) {
            return context.getString(R.string.category_work);
        } else if ("Kişisel".equals(turkishCategory)) {
            return context.getString(R.string.category_personal);
        } else if ("Alışveriş".equals(turkishCategory)) {
            return context.getString(R.string.category_shopping);
        } else if ("Sağlık".equals(turkishCategory)) {
            return context.getString(R.string.category_health);
        } else if ("Eğitim".equals(turkishCategory)) {
            return context.getString(R.string.category_education);
        }
        return turkishCategory;
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkDone;
        TextView textTask, textCategory, textPriority, textDescription;
        View iconPriority;
        ImageButton btnDelete;

        @SuppressLint("WrongViewCast")
        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            checkDone = itemView.findViewById(R.id.checkBoxTodo);
            textTask = itemView.findViewById(R.id.textViewTodo);
            textCategory = itemView.findViewById(R.id.textViewCategory);
            textPriority = itemView.findViewById(R.id.textViewPriority);
            textDescription = itemView.findViewById(R.id.textViewDescription);
            iconPriority = itemView.findViewById(R.id.priorityIndicator);
            btnDelete = itemView.findViewById(R.id.btnDeleteTodo);
        }
    }
}