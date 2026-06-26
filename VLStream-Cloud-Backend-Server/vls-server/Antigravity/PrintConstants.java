import org.springblade.core.launch.constant.TokenConstant;
import java.lang.reflect.Field;

public class PrintConstants {
    public static void main(String[] args) {
        try {
            for (Field field : TokenConstant.class.getDeclaredFields()) {
                System.out.println(field.getName() + " = " + field.get(null));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
