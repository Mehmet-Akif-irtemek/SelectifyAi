package com.app.selectifyai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<ChatSummary> chatList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerView = findViewById(R.id.recyclerViewChatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(chatList, chatId -> openChat(chatId));
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        loadChats();
    }

    private void loadChats() {
        if (uid == null) return;
        firestore.collection("kullanicilar").document(uid).collection("chats")
                .get()
                .addOnSuccessListener(this::onChatsLoaded)
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.chatler_yuklenemedi), Toast.LENGTH_SHORT).show());
    }

    private void onChatsLoaded(QuerySnapshot snapshots) {
        chatList.clear();
        for (DocumentSnapshot doc : snapshots) {
            String chatId = doc.getId();
            String firstMessage = doc.getString("firstMessage");
            if (firstMessage == null) firstMessage = "(Boş başlık)";
            chatList.add(new ChatSummary(chatId, firstMessage));
        }
        adapter.notifyDataSetChanged();
    }

    private void openChat(String chatId) {
        Intent intent = new Intent(this, AiChatActivity.class);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
    }

    // Chat özet modeli
    public static class ChatSummary {
        public String chatId;
        public String firstMessage;
        public ChatSummary(String chatId, String firstMessage) {
            this.chatId = chatId;
            this.firstMessage = firstMessage;
        }
    }

    // Adapter (iç sınıf)
    public static class ChatListAdapter extends RecyclerView.Adapter<ChatListViewHolder> {
        private final List<ChatSummary> items;
        private final OnChatClickListener listener;
        public interface OnChatClickListener { void onChatClick(String chatId); }
        public ChatListAdapter(List<ChatSummary> items, OnChatClickListener listener) {
            this.items = items;
            this.listener = listener;
        }
        @NonNull
        @Override
        public ChatListViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ChatListViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
            ChatSummary chat = items.get(position);
            ((android.widget.TextView) holder.itemView).setText(chat.firstMessage);
            holder.itemView.setOnClickListener(v -> listener.onChatClick(chat.chatId));
        }
        @Override
        public int getItemCount() { return items.size(); }
    }
    public static class ChatListViewHolder extends RecyclerView.ViewHolder {
        public ChatListViewHolder(@NonNull View itemView) { super(itemView); }
    }
} 