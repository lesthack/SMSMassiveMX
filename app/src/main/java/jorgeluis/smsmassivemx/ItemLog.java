package jorgeluis.smsmassivemx;

/**
 * Created by lesthack on 20/10/16.
 */

public class ItemLog {
    private int id;
    private String date;
    private String text;
    private int type;

    public ItemLog(int id, String date, String text, int type){
        this.id = id;
        this.date = date;
        this.text = text;
        this.type = type;
    }

    public String getItemDate(){
        return this.date;
    }

    public String getItemText(){
        return this.text;
    }

    public int getItemType(){
        return this.type;
    }
}
