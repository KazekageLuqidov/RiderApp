package ng.com.bitwebdev.rider.historyRecyclerView;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.util.List;

import ng.com.bitwebdev.rider.R;

/**
 * Created by Lucky on 01/31/2018.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
    private List<historyObject> itemList;
    private Context context;

    public HistoryAdapter(List<historyObject> itemList, Context context){
        this.itemList = itemList;
        this.context = context;
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_views, null, false);
        RecyclerView.LayoutParams Lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(Lp);
        HistoryViewHolder histV = new HistoryViewHolder(layoutView);
        return histV;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        holder.rideId.setText(itemList.get(position).getRideId());
        holder.time.setText(itemList.get(position).getTime());

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
