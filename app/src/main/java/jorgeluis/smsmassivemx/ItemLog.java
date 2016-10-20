package jorgeluis.smsmassivemx;

/**
 * Created by lesthack on 20/10/16.
 */

public class ItemLog {
    private int id;
    private String date;
    private String text;

    public ItemLog(int id, String date, String text){
        this.id = id;
        this.date = date;
        this.text = text;
    }

    public String getItemDate(){
        return this.date;
    }

    public String getItemText(){
        return this.text;
    }

}
