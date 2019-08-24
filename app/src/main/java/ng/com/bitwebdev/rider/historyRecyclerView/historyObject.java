package ng.com.bitwebdev.rider.historyRecyclerView;

/**
 * Created by Lucky on 01/31/2018.
 */

public class historyObject {

    private String rideId;
    private String time;


    public historyObject(String rideId, String time){
        this.rideId = rideId;
        this.rideId = time;
    }

    public String getRideId (){
        return rideId;
    }
    public String getTime (){
        return time;
    }



}
