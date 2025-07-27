package com.app.selectifyai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI   = 2;

    private final List<ChatMessage> messageList;
    private final SimpleDateFormat  timeFormat;
    private OnMessageLongClickListener longClickListener;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.timeFormat  = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override public int getItemViewType(int pos) {
        return messageList.get(pos).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p,int t) {
        LayoutInflater inf = LayoutInflater.from(p.getContext());
        if (t == VIEW_TYPE_USER)
            return new UserVH(inf.inflate(R.layout.item_message_user, p,false));
        return new AiVH(inf.inflate(R.layout.item_message_ai, p,false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h,int pos){
        ChatMessage m = messageList.get(pos);
        if (h instanceof UserVH) ((UserVH)h).bind(m);
        else ((AiVH)h).bind(m);
    }

    @Override public int getItemCount(){ return messageList.size(); }

    /* ---------- long-click listener ---------- */
    public interface OnMessageLongClickListener{
        void onMessageLongClick(int position, ChatMessage msg);
    }
    public void setOnMessageLongClickListener(OnMessageLongClickListener l){ this.longClickListener=l; }

    /* ---------- USER holder ---------- */
    class UserVH extends RecyclerView.ViewHolder{
        TextView txt, time;
        ImageView imageMessage;
        UserVH(@NonNull View v){
            super(v);
            txt  = v.findViewById(R.id.textMessage);
            time = v.findViewById(R.id.textTime);
            imageMessage = v.findViewById(R.id.imageMessage);
            v.setOnLongClickListener(view->{
                if(longClickListener!=null){
                    longClickListener.onMessageLongClick(getAdapterPosition(),
                            messageList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }
        void bind(ChatMessage m){
            txt.setText(m.getMessage());
            time.setText(timeFormat.format(new Date(m.getTimestamp())));
            
            // Görsel kontrolü
            if (m.hasImage() && m.getImageBase64() != null) {
                try {
                    byte[] decodedBytes = Base64.decode(m.getImageBase64(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    imageMessage.setImageBitmap(bitmap);
                    imageMessage.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    imageMessage.setVisibility(View.GONE);
                }
            } else {
                imageMessage.setVisibility(View.GONE);
            }
        }
    }

    /* ---------- AI holder ---------- */
    class AiVH extends RecyclerView.ViewHolder{
        TextView txt, time;
        ImageView imageMessage;
        AiVH(@NonNull View v){
            super(v);
            txt  = v.findViewById(R.id.textMessage);
            time = v.findViewById(R.id.textTime);
            imageMessage = v.findViewById(R.id.imageMessage);
            v.setOnLongClickListener(view->{
                if(longClickListener!=null){
                    longClickListener.onMessageLongClick(getAdapterPosition(),
                            messageList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }
        void bind(ChatMessage m){
            formatAndSetAIMessage(m.getMessage(), txt);
            time.setText(timeFormat.format(new Date(m.getTimestamp())));
            
            // AI mesajlarında görsel gösterilmez (sadece kullanıcı mesajlarında)
            imageMessage.setVisibility(View.GONE);
        }
    }

    /* ---------- AI mesaj biçimlendirme ---------- */
    private void formatAndSetAIMessage(String msg, TextView tv){
        String[] lines = msg.split("\\n");
        SpannableStringBuilder sb = new SpannableStringBuilder();

        boolean inCode=false; String lang=""; StringBuilder buf=new StringBuilder();

        for(String line:lines){
            if(line.trim().startsWith("**")){                      // başlık
                if(inCode){ addCode(sb,lang,buf.toString()); inCode=false; buf.setLength(0);}
                appendTitle(sb,line);
            }else if(line.trim().startsWith("```")){               // kod bloğu aç/kapat
                if(inCode){
                    addCode(sb,lang,buf.toString());
                    inCode=false; buf.setLength(0);
                }else{
                    inCode=true;
                    lang=line.trim().substring(3).trim();
                    if(lang.contains(":")) lang=lang.split(":")[0];
                }
            }else if(inCode){                                      // kod satırı
                buf.append(line).append("\n");
            }else{                                                 // normal metin
                sb.append(line).append("\n");
            }
        }
        if(inCode) addCode(sb,lang,buf.toString());

        tv.setText(sb);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void appendTitle(SpannableStringBuilder sb,String line){
        SpannableString s=new SpannableString(line+"\n");
        s.setSpan(new StyleSpan(Typeface.BOLD),0,line.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new RelativeSizeSpan(1.2f),0,line.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(s);
    }

    /* ---- sadece kod bloklarına stil verilir ---- */
    private void addCode(SpannableStringBuilder sb,String lang,String code){
        if(!lang.isEmpty()){
            SpannableString ls=new SpannableString(lang+"\n");
            ls.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                    0, lang.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ls.setSpan(new StyleSpan(Typeface.BOLD),
                    0, lang.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(ls);
        }

        SpannableString cs=new SpannableString(code);
        cs.setSpan(new BackgroundColorSpan(Color.parseColor("#23272F")),
                0, code.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        cs.setSpan(new ForegroundColorSpan(Color.parseColor("#F8F8F2")),
                0, code.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        cs.setSpan(new TypefaceSpan("monospace"),
                0, code.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        /* kopyala */
        cs.setSpan(new ClickableSpan(){
            @Override public void onClick(@NonNull View w){
                ClipboardManager cb=(ClipboardManager) w.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("code",code));
                Toast.makeText(w.getContext(),"Kod kopyalandı",Toast.LENGTH_SHORT).show();
            }
        },0,code.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sb.append(cs).append("\n");
    }
}