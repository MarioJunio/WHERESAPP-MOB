package br.com.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.br.wheresapp.R;

import java.util.ArrayList;
import java.util.List;

import br.com.aplication.App;
import br.com.model.domain.Message;
import br.com.service.DateService;
import br.com.widgets.Emoji;

/**
 * Created by MarioJ on 21/03/15.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final String TAG = "ChatAdapter";

    // tipos de balaoes de mensagem
    public static final int BUBBLE_RIGHT = 0;
    public static final int BUBBLE_LEFT = 1;

    private Context context;
    private List<Message> messages;

    public ChatAdapter(Context context) {
        this.context = context;
        messages = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // declara a view referente ao layout que sera utilizado
        View view = null;

        // declara o viewholder que vai manipular os widgets do layout
        ViewHolder viewHolder = null;

        // checa se o balao da mensagem é o da direita ou esquerda, dependendo do remetente da mensagem
        if (viewType == BUBBLE_RIGHT)
            view = LayoutInflater.from(context).inflate(R.layout.chat_bubble_right, parent, false);
        else if (viewType == BUBBLE_LEFT)
            view = LayoutInflater.from(context).inflate(R.layout.chat_bubble_left, parent, false);

        // check se o layout foi instanciado de acordo com as regras de negocio
        if (view != null)
            viewHolder = new ViewHolder(view);

        // retorna o manipulador do layout
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        if (holder != null) {

            // obtem mensagem na posicao 'position'
            Message message = messages.get(position);

            // setup widgets do layout de acordo com a mensagem encontrada
            holder.messageTextView.setText(Emoji.replaceEmoji(message.getBody(), holder.messageTextView.getPaint().getFontMetricsInt(), App.Emoji_FONT_METRICS_SIZE));
            holder.timeTextView.setText(DateService.getTime(message.getDate()));

            // checa se foi o remetente que enviou a mensagem, e atualize o widget de imagem para notificar o usuario se a mensagem ainda foi 'submetida' ou esta 'aguardando' rede
            if (holder.messageStatus != null) {

                if (message.getDelivery() == Message.Delivery.SUBMITED)
                    holder.messageStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_single_tick));
                else
                    holder.messageStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_clock));
            }

        }

    }

    @Override
    public int getItemViewType(int position) {

        Message message = messages.get(position);

        if (message.getDelivery().equals(Message.Delivery.PENDING) || message.getDelivery().equals(Message.Delivery.SUBMITED))
            return BUBBLE_RIGHT;
        else if (message.getDelivery().equals(Message.Delivery.DELIVERED))
            return BUBBLE_LEFT;

        return -1;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getId();
    }

    /**
     * insere as mensagens na lista
     */
    public void setup(List<Message> messages) {

        for (Message m : messages)
            add(m);

    }

    /**
     * insere mensagem na lista
     */
    public void add(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * remove mensagem da lista
     */
    public void remove(int position) {
        messages.remove(position);
        notifyItemRemoved(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView messageStatus;
        public TextView messageTextView;
        public TextView timeTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            this.messageTextView = (TextView) itemView.findViewById(R.id.message_text);
            this.timeTextView = (TextView) itemView.findViewById(R.id.time_text);

            if (itemView.findViewById(R.id.user_reply_status) != null)
                this.messageStatus = (ImageView) itemView.findViewById(R.id.user_reply_status);
        }
    }
}
