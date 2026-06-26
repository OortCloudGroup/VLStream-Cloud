import org.springblade.core.secure.BladeUser;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TypeHelper {
    public static void main(String[] args) {
        try {
            Class<?> clazz = DeviceInfo.class;
            System.out.println("Analyzing DeviceInfo and its superclasses:");
            while (clazz != null && clazz != Object.class) {
                System.out.println("Class: " + clazz.getName());
                for (Field field : clazz.getDeclaredFields()) {
                    System.out.println(String.format("  Field: %s | Type: %s", field.getName(), field.getType().getName()));
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
