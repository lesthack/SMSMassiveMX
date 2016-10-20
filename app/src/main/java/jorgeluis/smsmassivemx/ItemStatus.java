package jorgeluis.smsmassivemx;

/**
 * Created by lesthack on 19/10/16.
 */

public class ItemStatus {
    private String name;
    private Boolean value;

    public ItemStatus(String name, Boolean value){
        this.name = name;
        this.value = value;
    }

    public String getItemName(){
        return this.name;
    }

    public Boolean getItemValue(){
        return this.value;
    }

    public String getItemValueStr(){
        if(this.value){
            return "ON";
        }
        return "OFF";
    }
}
