package dal;

import org.springframework.stereotype.Repository; // מסמן את המחלקה כשכבת נתונים
import java.util.ArrayList;
import java.util.List;

@Repository
public class DataRepository {

    private List<String> messages = new ArrayList<>();

    public String getData() {
        return "Hello from DataRepository!";
    }

    public void saveData(String message) {
        messages.add(message);
    }
}